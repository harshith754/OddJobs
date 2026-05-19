# Spec 001: Frame Stream MVP

## Status

Implemented and usable as a personal end-to-end build.

## Current Implementation Status

- Android app shell is implemented and testable on device.
- `Frame Stream` can start a foreground service and capture local camera frames at interval.
- The app currently keeps only the latest local frame file for on-device verification.
- Android uploads captured frames to the Next.js API and Supabase-backed storage/metadata flow.
- Android can list prior sessions and delete a session with its associated images.
- Android requests camera and notification permission before starting capture on supported Android versions.
- Web/API session and image persistence are implemented behind a repository layer with Supabase-backed mode and in-memory fallback.
- Database and storage bucket setup are migration-driven in `db/migrations/`.
- Web viewer is deployed and usable against the public backend.

## Product Summary

`OddJobs` is a personal Android utility app. The first odd job is `Frame Stream`, which captures high-quality still images from the Android back camera at a configured interval, uploads them to a backend, and exposes a private viewer link for other people to inspect the latest frame and recent history.

## MVP Decisions

- Android-only
- No login flow in the app
- One permanent personal `Frame Stream` in V1
- One stable public viewer token
- Every start/stop creates a new session
- History is included in MVP
- Background use means working while the user opens other non-camera apps
- Phone may remain plugged in

## Acceptance Criteria

### Android

- App opens to the `OddJobs` home screen
- `Frame Stream` appears as an available odd job
- User can configure interval and quality
- User can start, pause, resume, and stop a stream
- App shows a stable viewer URL
- Capture continues while user leaves the app for normal non-camera usage
- A foreground service notification remains visible while active
- App can list prior sessions and delete a session with its stored images

### Web

- Viewer route exists at `/s/[token]`
- Viewer shows current stream status
- Viewer shows latest uploaded image
- Viewer shows recent history
- Viewer supports polling-based refresh in MVP
- Viewer presents recent frames as a live-updating carousel with selected-frame detail

### Backend

- Stream identity is long-lived
- Sessions are created per start/stop cycle
- Uploaded frames are stored as files with metadata rows
- Public APIs expose latest image and history by token
- Session management APIs expose session listing and deletion

## Data Model

- `streams`
- `stream_sessions`
- `stream_images`

See `/db/migrations/0001_initial_schema.sql` for the initial schema.

## Implementation Notes

- Prefer clarity over aggressive optimization in V1
- Default interval should be `2s`
- Default quality should be `High`
- Real device target begins with Samsung S23
- Frame retention can be cleaned up after usage, rather than aggressively limited during MVP
- Current quality implementation is compression-based (`Balanced`/`High`/`Max` map to JPEG quality presets), not distinct camera resolution profiles yet.
- Capture cadence is currently cycle-based (`capture + upload + sleep`), so a configured `1s` interval is not strict fixed-rate timing.
