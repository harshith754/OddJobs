# Spec 001: Frame Stream MVP

## Status

Drafted and approved for initial implementation.

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

### Web

- Viewer route exists at `/s/[token]`
- Viewer shows current stream status
- Viewer shows latest uploaded image
- Viewer shows recent history
- Viewer supports polling-based refresh in MVP

### Backend

- Stream identity is long-lived
- Sessions are created per start/stop cycle
- Uploaded frames are stored as files with metadata rows
- Public APIs expose latest image and history by token

## Data Model

- `streams`
- `stream_sessions`
- `stream_images`

See `/supabase/schema.sql` for the initial schema.

## Implementation Notes

- Prefer clarity over aggressive optimization in V1
- Default interval should be `2s`
- Default quality should be `High`
- Real device target begins with Samsung S23
- Frame retention can be cleaned up after usage, rather than aggressively limited during MVP
