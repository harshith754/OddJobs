# OddJobs

Spec-driven monorepo for a personal Android utility app and its web/backend companion.

Current MVP target:

- Android app shell
- `Frame Stream` odd job
- Reusable private viewer link
- Backend APIs for stream/session/image metadata
- Web viewer for latest image and history

## Repo Layout

```txt
OddJobs/
  specs/       Product and implementation specs
  android/     Android app (Kotlin + Compose)
  web/         Next.js viewer + API routes
  db/          Database migrations and storage notes
  docs/        Supporting implementation notes
```

## Product Shape

- `OddJobs` is the container app.
- `Frame Stream` is the first working odd job.
- V1 assumes one long-lived personal stream with a stable public token.
- Each start/stop creates a session under that stream.
- Uploaded images belong to sessions and remain queryable as history.

## Development Approach

- Specs live first under `specs/`
- Code is shaped to match approved specs
- `android/`, `web/`, and `db/` are the implementation workspaces

## Status

This repo is scaffolded for implementation. It is not yet dependency-installed or fully runnable in this environment.

## Next Build Steps

1. Open `android/` in Android Studio.
2. Let Android Studio create/update the Gradle wrapper if needed.
3. Create a Next.js app environment for `web/` with `npm install`.
4. Copy `web/.env.example` to `web/.env.local` and fill in provider values.
5. Provision the database/storage project and apply `db/migrations/0001_initial_schema.sql`.
6. The `stream-images` bucket is created by migration `0002_create_stream_images_bucket.sql`.

## Web Persistence

The web/API layer now supports two persistence modes:

- **Supabase-backed mode** when `SUPABASE_URL` and `SUPABASE_SECRET_KEY` are set
- **in-memory fallback** when those variables are missing

In Supabase-backed mode:

- `stream_sessions` rows are created per start/stop cycle
- `stream_images` rows are stored per session
- uploaded image files are written to Supabase Storage
- the viewer page reads latest image and history from database-backed session data

The Android app does not yet upload to this backend. The route contract is ready for that integration.
