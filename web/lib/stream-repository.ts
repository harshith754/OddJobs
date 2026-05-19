import { getServerPersistenceConfig, hasSupabasePersistence } from "./server-config";
import {
  CreateStreamSessionRequest,
  CreateStreamSessionResponse,
  PublicHistoryResponse,
  PublicLatestResponse,
  SessionMutationResponse,
  StreamImage,
  StreamRecord,
  StreamSession,
  StreamStatus
} from "./types";

type UploadFrameInput = {
  sessionId: string;
  sequenceNumber?: number;
  width?: number;
  height?: number;
  fileSizeBytes?: number;
  imageUrl?: string;
  file?: File;
};

type StreamRepository = {
  createSession(payload?: CreateStreamSessionRequest): Promise<CreateStreamSessionResponse>;
  uploadFrame(input: UploadFrameInput): Promise<StreamImage | null>;
  mutateSessionStatus(
    sessionId: string,
    status: StreamStatus
  ): Promise<SessionMutationResponse | null>;
  getLatestFrame(token: string): Promise<PublicLatestResponse | null>;
  getHistory(token: string): Promise<PublicHistoryResponse | null>;
};

type DbStreamRow = {
  id: string;
  name: string;
  public_token: string;
  status: StreamStatus;
  default_interval_seconds: number;
  default_quality: "balanced" | "high" | "max";
};

type DbSessionRow = {
  id: string;
  stream_id: string;
  status: StreamStatus;
  started_at: string;
  ended_at: string | null;
  last_image_at: string | null;
};

type DbImageRow = {
  id: string;
  session_id: string;
  storage_path: string;
  sequence_number: number;
  width: number;
  height: number;
  file_size_bytes: number;
  created_at: string;
};

const config = getServerPersistenceConfig();

function nowIso() {
  return new Date().toISOString();
}

function randomId(prefix: string) {
  return `${prefix}-${Math.random().toString(36).slice(2, 10)}`;
}

function buildViewerUrl(token: string) {
  return `${config.appUrl.replace(/\/$/, "")}/s/${token}`;
}

function storageObjectUrl(storagePath: string) {
  if (!config.supabaseUrl) {
    return storagePath;
  }

  return `${config.supabaseUrl}/storage/v1/object/public/${config.storageBucket}/${storagePath}`;
}

function mapDbImageToPublicImage(row: DbImageRow): StreamImage {
  return {
    id: row.id,
    sessionId: row.session_id,
    imageUrl: storageObjectUrl(row.storage_path),
    sequenceNumber: row.sequence_number,
    createdAt: row.created_at,
    width: row.width,
    height: row.height,
    fileSizeBytes: row.file_size_bytes
  };
}

async function postgrest<T>(
  path: string,
  init: RequestInit = {},
  preferSingle = false
): Promise<T> {
  if (!config.supabaseUrl || !config.supabaseServiceRoleKey) {
    throw new Error("Supabase persistence is not configured");
  }

  const response = await fetch(`${config.supabaseUrl}/rest/v1${path}`, {
    ...init,
    headers: {
      apikey: config.supabaseServiceRoleKey,
      Authorization: `Bearer ${config.supabaseServiceRoleKey}`,
      "Content-Type": "application/json",
      Prefer: preferSingle ? "return=representation" : "return=representation",
      ...(init.headers ?? {})
    },
    cache: "no-store"
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`PostgREST request failed (${response.status}): ${body}`);
  }

  return (await response.json()) as T;
}

async function uploadToStorage(storagePath: string, file: File): Promise<void> {
  if (!config.supabaseUrl || !config.supabaseServiceRoleKey) {
    throw new Error("Supabase persistence is not configured");
  }

  const response = await fetch(
    `${config.supabaseUrl}/storage/v1/object/${config.storageBucket}/${storagePath}`,
    {
      method: "POST",
      headers: {
        apikey: config.supabaseServiceRoleKey,
        Authorization: `Bearer ${config.supabaseServiceRoleKey}`,
        "Content-Type": file.type || "image/jpeg",
        "x-upsert": "true"
      },
      body: Buffer.from(await file.arrayBuffer()),
      cache: "no-store"
    }
  );

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`Storage upload failed (${response.status}): ${body}`);
  }
}

async function getMainStream(): Promise<DbStreamRow> {
  const rows = await postgrest<DbStreamRow[]>(
    `/streams?public_token=eq.${encodeURIComponent(config.streamToken)}&select=*`
  );
  const stream = rows[0];
  if (!stream) {
    throw new Error(`No stream found for token ${config.streamToken}`);
  }
  return stream;
}

const inMemoryRepository = (() => {
  const stream: StreamRecord = {
    id: "stream-main-frame-stream",
    name: "Main Frame Stream",
    publicToken: config.streamToken,
    status: "paused",
    defaultIntervalSeconds: 2,
    defaultQuality: "high"
  };

  let sessions: StreamSession[] = [];
  let images: StreamImage[] = [];
  let seeded = false;

  function ensureSeedData() {
    if (seeded) {
      return;
    }

    const sessionId = randomId("session");
    const startedAt = nowIso();

    sessions = [
      {
        id: sessionId,
        streamId: stream.id,
        status: "active",
        startedAt,
        endedAt: null,
        lastImageAt: startedAt
      }
    ];

    images = Array.from({ length: 12 }, (_, index) => ({
      id: randomId("image"),
      sessionId,
      imageUrl: `https://picsum.photos/seed/oddjobs-${index + 1}/1600/900`,
      sequenceNumber: index + 1,
      createdAt: new Date(Date.now() - (11 - index) * 2_000).toISOString(),
      width: 1600,
      height: 900,
      fileSizeBytes: 420_000
    }));

    seeded = true;
    stream.status = "active";
  }

  function getCurrentSession() {
    return (
      sessions.find(
        (session) => session.status === "active" || session.status === "paused"
      ) ?? null
    );
  }

  return {
    async createSession(
      payload: CreateStreamSessionRequest = {}
    ): Promise<CreateStreamSessionResponse> {
      ensureSeedData();
      const activeSession = getCurrentSession();
      if (activeSession) {
        activeSession.status = "ended";
        activeSession.endedAt = nowIso();
      }

      const sessionId = randomId("session");
      const startedAt = nowIso();
      const name = payload.name?.trim();

      if (name) {
        stream.name = name;
      }

      if (payload.settings?.intervalSeconds) {
        stream.defaultIntervalSeconds = payload.settings.intervalSeconds;
      }

      if (payload.settings?.quality) {
        stream.defaultQuality = payload.settings.quality;
      }

      sessions.unshift({
        id: sessionId,
        streamId: stream.id,
        status: "active",
        startedAt,
        endedAt: null,
        lastImageAt: null
      });

      stream.status = "active";

      return {
        streamId: stream.id,
        sessionId,
        streamToken: stream.publicToken,
        viewerUrl: buildViewerUrl(stream.publicToken),
        status: "active"
      };
    },

    async uploadFrame(input: UploadFrameInput): Promise<StreamImage | null> {
      ensureSeedData();
      const session = sessions.find((item) => item.id === input.sessionId);
      if (!session) {
        return null;
      }

      const sequenceNumber =
        input.sequenceNumber ??
        images.filter((image) => image.sessionId === input.sessionId).length + 1;
      const createdAt = nowIso();
      const image = {
        id: randomId("image"),
        sessionId: input.sessionId,
        imageUrl:
          input.imageUrl ??
          `https://picsum.photos/seed/${input.sessionId}-${sequenceNumber}/1600/900`,
        sequenceNumber,
        createdAt,
        width: input.width ?? 1600,
        height: input.height ?? 900,
        fileSizeBytes: input.fileSizeBytes ?? 420_000
      };

      images.unshift(image);
      session.lastImageAt = createdAt;
      session.status = "active";
      stream.status = "active";

      return image;
    },

    async mutateSessionStatus(
      sessionId: string,
      status: StreamStatus
    ): Promise<SessionMutationResponse | null> {
      ensureSeedData();
      const session = sessions.find((item) => item.id === sessionId);
      if (!session) {
        return null;
      }

      session.status = status;
      if (status === "ended") {
        session.endedAt = nowIso();
      }
      stream.status = status;

      return { sessionId, status };
    },

    async getLatestFrame(token: string): Promise<PublicLatestResponse | null> {
      ensureSeedData();
      if (token !== stream.publicToken) {
        return null;
      }

      const currentSession = getCurrentSession();
      const latestImage =
        currentSession == null
          ? null
          : images.find((image) => image.sessionId === currentSession.id) ?? null;

      return {
        status: stream.status,
        latestImage,
        streamName: stream.name,
        sessionId: currentSession?.id ?? null
      };
    },

    async getHistory(token: string): Promise<PublicHistoryResponse | null> {
      ensureSeedData();
      if (token !== stream.publicToken) {
        return null;
      }

      const currentSession = getCurrentSession();
      const sessionImages =
        currentSession == null
          ? []
          : images.filter((image) => image.sessionId === currentSession.id);

      return {
        status: stream.status,
        streamName: stream.name,
        sessionId: currentSession?.id ?? null,
        images: sessionImages
      };
    }
  } satisfies StreamRepository;
})();

const supabaseRepository: StreamRepository = {
  async createSession(
    payload: CreateStreamSessionRequest = {}
  ): Promise<CreateStreamSessionResponse> {
    const stream = await getMainStream();
    const activeSessions = await postgrest<DbSessionRow[]>(
      `/stream_sessions?stream_id=eq.${stream.id}&status=in.(active,paused)&select=*`
    );

    await Promise.all(
      activeSessions.map((session) =>
        postgrest<DbSessionRow[]>(
          `/stream_sessions?id=eq.${session.id}`,
          {
            method: "PATCH",
            body: JSON.stringify({
              status: "ended",
              ended_at: nowIso()
            })
          }
        )
      )
    );

    if (payload.name?.trim() || payload.settings?.intervalSeconds || payload.settings?.quality) {
      await postgrest<DbStreamRow[]>(
        `/streams?id=eq.${stream.id}`,
        {
          method: "PATCH",
          body: JSON.stringify({
            name: payload.name?.trim() || stream.name,
            default_interval_seconds:
              payload.settings?.intervalSeconds ?? stream.default_interval_seconds,
            default_quality: payload.settings?.quality ?? stream.default_quality,
            status: "active",
            updated_at: nowIso()
          })
        }
      );
    } else {
      await postgrest<DbStreamRow[]>(
        `/streams?id=eq.${stream.id}`,
        {
          method: "PATCH",
          body: JSON.stringify({
            status: "active",
            updated_at: nowIso()
          })
        }
      );
    }

    const inserted = await postgrest<DbSessionRow[]>(
      `/stream_sessions`,
      {
        method: "POST",
        body: JSON.stringify({
          stream_id: stream.id,
          status: "active"
        })
      }
    );
    const session = inserted[0];

    return {
      streamId: stream.id,
      sessionId: session.id,
      streamToken: stream.public_token,
      viewerUrl: buildViewerUrl(stream.public_token),
      status: "active"
    };
  },

  async uploadFrame(input: UploadFrameInput): Promise<StreamImage | null> {
    const sessionRows = await postgrest<DbSessionRow[]>(
      `/stream_sessions?id=eq.${input.sessionId}&select=*`
    );
    const session = sessionRows[0];
    if (!session) {
      return null;
    }

    let storagePath = input.imageUrl ?? "";
    if (input.file) {
      const sequenceNumber =
        input.sequenceNumber ??
        ((await postgrest<DbImageRow[]>(
          `/stream_images?session_id=eq.${input.sessionId}&select=id`
        )).length + 1);
      storagePath = `streams/${session.stream_id}/sessions/${input.sessionId}/frames/${String(
        sequenceNumber
      ).padStart(6, "0")}-${Date.now()}.jpg`;
      await uploadToStorage(storagePath, input.file);
      input.sequenceNumber = sequenceNumber;
      input.fileSizeBytes = input.fileSizeBytes ?? input.file.size;
    }

    const inserted = await postgrest<DbImageRow[]>(
      `/stream_images`,
      {
        method: "POST",
        body: JSON.stringify({
          session_id: input.sessionId,
          storage_path: storagePath,
          sequence_number: input.sequenceNumber ?? 1,
          width: input.width ?? 0,
          height: input.height ?? 0,
          file_size_bytes: input.fileSizeBytes ?? 0
        })
      }
    );
    const image = inserted[0];

    await postgrest<DbSessionRow[]>(
      `/stream_sessions?id=eq.${input.sessionId}`,
      {
        method: "PATCH",
        body: JSON.stringify({
          status: "active",
          last_image_at: image.created_at
        })
      }
    );

    return mapDbImageToPublicImage(image);
  },

  async mutateSessionStatus(
    sessionId: string,
    status: StreamStatus
  ): Promise<SessionMutationResponse | null> {
    const sessionRows = await postgrest<DbSessionRow[]>(
      `/stream_sessions?id=eq.${sessionId}&select=*`
    );
    const session = sessionRows[0];
    if (!session) {
      return null;
    }

    await postgrest<DbSessionRow[]>(
      `/stream_sessions?id=eq.${sessionId}`,
      {
        method: "PATCH",
        body: JSON.stringify({
          status,
          ended_at: status === "ended" ? nowIso() : null
        })
      }
    );

    await postgrest<DbStreamRow[]>(
      `/streams?id=eq.${session.stream_id}`,
      {
        method: "PATCH",
        body: JSON.stringify({
          status,
          updated_at: nowIso()
        })
      }
    );

    return { sessionId, status };
  },

  async getLatestFrame(token: string): Promise<PublicLatestResponse | null> {
    const streamRows = await postgrest<DbStreamRow[]>(
      `/streams?public_token=eq.${encodeURIComponent(token)}&select=*`
    );
    const stream = streamRows[0];
    if (!stream) {
      return null;
    }

    const sessionRows = await postgrest<DbSessionRow[]>(
      `/stream_sessions?stream_id=eq.${stream.id}&status=in.(active,paused)&order=started_at.desc&limit=1&select=*`
    );
    const session = sessionRows[0] ?? null;
    let latestImage: StreamImage | null = null;

    if (session) {
      const imageRows = await postgrest<DbImageRow[]>(
        `/stream_images?session_id=eq.${session.id}&order=sequence_number.desc&limit=1&select=*`
      );
      latestImage = imageRows[0] ? mapDbImageToPublicImage(imageRows[0]) : null;
    }

    return {
      status: stream.status,
      latestImage,
      streamName: stream.name,
      sessionId: session?.id ?? null
    };
  },

  async getHistory(token: string): Promise<PublicHistoryResponse | null> {
    const streamRows = await postgrest<DbStreamRow[]>(
      `/streams?public_token=eq.${encodeURIComponent(token)}&select=*`
    );
    const stream = streamRows[0];
    if (!stream) {
      return null;
    }

    const sessionRows = await postgrest<DbSessionRow[]>(
      `/stream_sessions?stream_id=eq.${stream.id}&status=in.(active,paused)&order=started_at.desc&limit=1&select=*`
    );
    const session = sessionRows[0] ?? null;
    let images: StreamImage[] = [];

    if (session) {
      const imageRows = await postgrest<DbImageRow[]>(
        `/stream_images?session_id=eq.${session.id}&order=sequence_number.desc&select=*`
      );
      images = imageRows.map(mapDbImageToPublicImage);
    }

    return {
      status: stream.status,
      streamName: stream.name,
      sessionId: session?.id ?? null,
      images
    };
  }
};

export function getStreamRepository(): StreamRepository {
  return hasSupabasePersistence(config) ? supabaseRepository : inMemoryRepository;
}

