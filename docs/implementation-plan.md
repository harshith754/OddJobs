# OddJobs V1 Implementation Plan

## Scope

- One Android app
- One working odd job: `Frame Stream`
- One stable personal stream link
- Many sessions and frame history
- Background capture target while using non-camera apps

## Android Work

1. Replace placeholder `FrameStreamViewModel` logic with:
   - permission handling
   - CameraX integration
   - foreground service orchestration
   - upload repository
2. Add DataStore for defaults:
   - backend URL
   - interval
   - quality
3. Add active status updates from service to UI.
4. Add share/copy/open viewer actions.

## Web/Backend Work

1. Replace mock data with Supabase queries.
2. Implement:
   - `POST /api/streams`
   - `POST /api/streams/{sessionId}/images`
   - `POST /api/streams/{sessionId}/pause`
   - `POST /api/streams/{sessionId}/resume`
   - `POST /api/streams/{sessionId}/end`
   - `GET /api/public/streams/{token}/latest`
   - `GET /api/public/streams/{token}/images`
3. Add client polling on `/s/[token]`.

## Data Model

- `streams`: long-lived stream identity and public token
- `stream_sessions`: each start/stop run
- `stream_images`: uploaded frame metadata

## Retention

- Keep all frames during active usage.
- Add cleanup path for images older than 24 hours once the upload path is stable.
