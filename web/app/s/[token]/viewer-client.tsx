"use client";

import { useEffect, useMemo, useState } from "react";
import {
  PublicHistoryResponse,
  PublicLatestResponse,
  StreamImage
} from "../../../lib/types";

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
  const [selectedImageId, setSelectedImageId] = useState<string | null>(
    initialLatest.latestImage?.id ?? initialHistory.images[0]?.id ?? null
  );
  const [error, setError] = useState<string | null>(null);
  const [copyState, setCopyState] = useState<string | null>(null);

  useEffect(() => {
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
  }, [token]);

  useEffect(() => {
    const availableIds = new Set(history.images.map((image) => image.id));
    if (selectedImageId && availableIds.has(selectedImageId)) {
      return;
    }

    setSelectedImageId(latest.latestImage?.id ?? history.images[0]?.id ?? null);
  }, [history.images, latest.latestImage, selectedImageId]);

  const lastUpdatedLabel = useMemo(() => {
    if (latest.latestImage == null) {
      return "No image uploaded yet";
    }

    return new Date(latest.latestImage.createdAt).toLocaleString();
  }, [latest.latestImage]);

  const selectedImage: StreamImage | null = useMemo(() => {
    if (selectedImageId == null) {
      return latest.latestImage;
    }

    return (
      history.images.find((image) => image.id === selectedImageId) ??
      latest.latestImage
    );
  }, [history.images, latest.latestImage, selectedImageId]);

  async function handleCopySelectedImage() {
    if (selectedImage == null) {
      return;
    }

    try {
      const response = await fetch(selectedImage.imageUrl, { cache: "no-store" });
      if (!response.ok) {
        throw new Error(`Image fetch failed: ${response.status}`);
      }

      const blob = await response.blob();
      if (
        typeof ClipboardItem !== "undefined" &&
        navigator.clipboard &&
        "write" in navigator.clipboard
      ) {
        await navigator.clipboard.write([
          new ClipboardItem({
            [blob.type || "image/jpeg"]: blob
          })
        ]);
        setCopyState("Image copied");
      } else if (navigator.clipboard && "writeText" in navigator.clipboard) {
        await navigator.clipboard.writeText(selectedImage.imageUrl);
        setCopyState("Image URL copied");
      } else {
        throw new Error("Clipboard is not available in this browser");
      }
    } catch (copyError) {
      setCopyState(
        copyError instanceof Error ? copyError.message : "Failed to copy image"
      );
    }
  }

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
            <h2>Live Frames</h2>
            <p className="muted">Auto-refreshing every second.</p>
          </div>
          <p className="muted">
            Latest: {latest.latestImage?.sequenceNumber ?? "none"} | Updated{" "}
            {lastUpdatedLabel}
          </p>
        </div>
        {error ? <p className="errorText">{error}</p> : null}
        {history.images.length > 0 ? (
          <div className="carousel" role="list" aria-label="Frame history carousel">
            {history.images.map((image) => (
              <button
                key={image.id}
                className={`carouselItem${
                  image.id === selectedImageId ? " carouselItemSelected" : ""
                }`}
                onClick={() => setSelectedImageId(image.id)}
                type="button"
              >
                <img
                  className="carouselThumb"
                  src={image.imageUrl}
                  alt={`Frame ${image.sequenceNumber}`}
                />
                <span className="carouselMeta">#{image.sequenceNumber}</span>
              </button>
            ))}
          </div>
        ) : (
          <p className="muted">No image uploaded yet.</p>
        )}
      </section>

      <section className="panel stack">
        <div className="toolbar">
          <div>
            <h2>Selected Frame</h2>
            <p className="muted">
              Click any frame in the carousel to inspect it here.
            </p>
          </div>
          {selectedImage ? (
            <button
              className="button"
              onClick={handleCopySelectedImage}
              type="button"
            >
              Copy image
            </button>
          ) : null}
        </div>
        {selectedImage ? (
          <>
            <img
              className="frame"
              src={selectedImage.imageUrl}
              alt={`Selected frame ${selectedImage.sequenceNumber}`}
            />
            {copyState ? <p className="muted">{copyState}</p> : null}
            <div className="historyList">
              <div className="historyItem">
                <strong>Frame {selectedImage.sequenceNumber}</strong>
                <span className="muted">
                  {new Date(selectedImage.createdAt).toLocaleString()}
                </span>
                <span className="muted">Session {selectedImage.sessionId}</span>
                <span className="muted">
                  {selectedImage.width}x{selectedImage.height} |{" "}
                  {selectedImage.fileSizeBytes} bytes
                </span>
              </div>
            </div>
          </>
        ) : (
          <p className="muted">No frame selected.</p>
        )}
      </section>
    </>
  );
}
