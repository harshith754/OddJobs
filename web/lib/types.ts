export type StreamStatus = "active" | "paused" | "ended" | "error";

export type StreamRecord = {
  id: string;
  name: string;
  publicToken: string;
  status: StreamStatus;
  defaultIntervalSeconds: number;
  defaultQuality: "balanced" | "high" | "max";
};

export type StreamSession = {
  id: string;
  streamId: string;
  status: StreamStatus;
  startedAt: string;
  endedAt: string | null;
  lastImageAt: string | null;
};

export type SessionSummary = {
  id: string;
  streamId: string;
  status: StreamStatus;
  startedAt: string;
  endedAt: string | null;
  lastImageAt: string | null;
  imageCount: number;
};

export type StreamImage = {
  id: string;
  sessionId: string;
  imageUrl: string;
  sequenceNumber: number;
  createdAt: string;
  width: number;
  height: number;
  fileSizeBytes: number;
};

export type CreateStreamSessionRequest = {
  name?: string;
  settings?: {
    intervalSeconds?: number;
    quality?: "balanced" | "high" | "max";
  };
};

export type CreateStreamSessionResponse = {
  streamId: string;
  sessionId: string;
  streamToken: string;
  viewerUrl: string;
  status: StreamStatus;
};

export type UploadFrameRequest = {
  imageUrl?: string;
  sequenceNumber?: number;
  width?: number;
  height?: number;
  fileSizeBytes?: number;
};

export type PublicLatestResponse = {
  status: StreamStatus;
  latestImage: StreamImage | null;
  streamName: string;
  sessionId: string | null;
};

export type PublicHistoryResponse = {
  status: StreamStatus;
  streamName: string;
  sessionId: string | null;
  images: StreamImage[];
};

export type SessionMutationResponse = {
  sessionId: string;
  status: StreamStatus;
};

export type SessionListResponse = {
  streamId: string;
  streamName: string;
  streamToken: string;
  sessions: SessionSummary[];
};

export type SessionDeleteResponse = {
  sessionId: string;
  deletedImages: number;
};
