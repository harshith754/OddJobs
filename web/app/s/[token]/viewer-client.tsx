"use client";

import { useEffect, useMemo, useState } from "react";
import { PublicHistoryResponse, PublicLatestResponse } from "../../../lib/types";

type ViewerClientProps = {
  token: string;
  initialLatest: PublicLatestResponse;
  initialHistory: PublicHistoryResponse;
};

async function fetchJson<T>(url: string): Promise<T> {
  const response = await fetch(url, { cache: "no-store" });
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }

  return (await response.json()) as T;
}

export function ViewerClient({
  token,
  initialLatest,
  initialHistory
}: ViewerClientProps) {
  const [latest, setLatest] = useState(initialLatest);
  const [history, setHistory] = useState(initialHistory);
  const [isAutoRefreshEnabled, setAutoRefreshEnabled] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isAutoRefreshEnabled) {
      return;
    }

    const interval = window.setInterval(async () => {
      try {
        const [latestResponse, historyResponse] = await Promise.all([
          fetchJson<PublicLatestResponse>(`/api/public/streams/${token}/latest`),
          fetchJson<PublicHistoryResponse>(`/api/public/streams/${token}/images`)
        ]);
        setLatest(latestResponse);
        setHistory(historyResponse);
        setError(null);
      } catch (requestError) {
        setError(
          requestError instanceof Error
            ? requestError.message
            : "Failed to refresh viewer"
        );
      }
    }, 1_000);

    return () => window.clearInterval(interval);
  }, [isAutoRefreshEnabled, token]);

  const lastUpdatedLabel = useMemo(() => {
    if (latest.latestImage == null) {
      return "No image uploaded yet";
    }

    return new Date(latest.latestImage.createdAt).toLocaleString();
  }, [latest.latestImage]);

  return (
    <>
      <section className="hero">
        <div className="badge">Private viewer link</div>
        <h1>{latest.streamName}</h1>
        <p className="muted">
          Status: {latest.status}. Session: {latest.sessionId ?? "none"}.
        </p>
      </section>

      <section className="panel stack">
        <div className="toolbar">
          <div className="stack">
            <h2>Latest Frame</h2>
            <p className="muted">Polling every second for MVP.</p>
          </div>
          <button
            className="button"
            onClick={() => setAutoRefreshEnabled((current) => !current)}
            type="button"
          >
            {isAutoRefreshEnabled ? "Pause refresh" : "Resume refresh"}
          </button>
        </div>
        {error ? <p className="errorText">{error}</p> : null}
        {latest.latestImage ? (
          <>
            <img
              className="frame"
              src={latest.latestImage.imageUrl}
              alt="Latest uploaded frame"
            />
            <p className="muted">
              Sequence {latest.latestImage.sequenceNumber} | Updated{" "}
              {lastUpdatedLabel}
            </p>
          </>
        ) : (
          <p className="muted">No image uploaded yet.</p>
        )}
      </section>

      <section className="panel stack">
        <div>
          <h2>Recent History</h2>
          <p className="muted">
            Latest-first session history for the current permanent stream link.
          </p>
        </div>
        <div className="historyList">
          {history.images.map((image) => (
            <div key={image.id} className="historyItem">
              <strong>Frame {image.sequenceNumber}</strong>
              <span className="muted">
                {new Date(image.createdAt).toLocaleString()}
              </span>
              <span className="muted">Session {image.sessionId}</span>
            </div>
          ))}
        </div>
      </section>
    </>
  );
}

