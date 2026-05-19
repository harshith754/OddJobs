import {
  CreateStreamSessionRequest,
  CreateStreamSessionResponse,
  PublicHistoryResponse,
  PublicLatestResponse,
  SessionMutationResponse,
  StreamImage,
  StreamRecord,
  StreamSession,
  StreamStatus,
  UploadFrameRequest
} from "./types";

const stream: StreamRecord = {
  id: "stream-main-frame-stream",
  name: "Main Frame Stream",
  publicToken: "main-frame-stream",
  status: "paused",
  defaultIntervalSeconds: 2,
  defaultQuality: "high"
};

let sessions: StreamSession[] = [];
let images: StreamImage[] = [];
let seeded = false;

function nowIso() {
  return new Date().toISOString();
}

function randomId(prefix: string) {
  return `${prefix}-${Math.random().toString(36).slice(2, 10)}`;
}

function getCurrentSession() {
  return sessions.find((session) => session.status === "active" || session.status === "paused") ?? null;
}

function setStreamStatus(status: StreamStatus) {
  stream.status = status;
}

export function ensureSeedData() {
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
  setStreamStatus("active");
}

export function createSession(
  payload: CreateStreamSessionRequest = {}
): CreateStreamSessionResponse {
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

  setStreamStatus("active");

  return {
    streamId: stream.id,
    sessionId,
    streamToken: stream.publicToken,
    viewerUrl: `https://oddjobs.app/s/${stream.publicToken}`,
    status: "active"
  };
}

export function uploadFrame(
  sessionId: string,
  payload: UploadFrameRequest = {}
) {
  const session = sessions.find((item) => item.id == sessionId);
  if (!session) {
    return null;
  }

  const sequenceNumber =
    payload.sequenceNumber ??
    images.filter((image) => image.sessionId === sessionId).length + 1;
  const createdAt = nowIso();
  const image = {
    id: randomId("image"),
    sessionId,
    imageUrl:
      payload.imageUrl ??
      `https://picsum.photos/seed/${sessionId}-${sequenceNumber}/1600/900`,
    sequenceNumber,
    createdAt,
    width: payload.width ?? 1600,
    height: payload.height ?? 900,
    fileSizeBytes: payload.fileSizeBytes ?? 420_000
  };

  images.unshift(image);
  session.lastImageAt = createdAt;
  session.status = "active";
  setStreamStatus("active");

  return image;
}

export function mutateSessionStatus(
  sessionId: string,
  status: StreamStatus
): SessionMutationResponse | null {
  const session = sessions.find((item) => item.id === sessionId);
  if (!session) {
    return null;
  }

  session.status = status;

  if (status === "ended") {
    session.endedAt = nowIso();
  }

  setStreamStatus(status);

  return {
    sessionId,
    status
  };
}

export function getLatestFrame(token: string): PublicLatestResponse | null {
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
}

export function getHistory(token: string): PublicHistoryResponse | null {
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

