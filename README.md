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
  supabase/    SQL schema and storage notes
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
- `android/`, `web/`, and `supabase/` are the implementation workspaces

## Status

This repo is scaffolded for implementation. It is not yet dependency-installed or fully runnable in this environment.

## Next Build Steps

1. Open `android/` in Android Studio.
2. Let Android Studio create/update the Gradle wrapper if needed.
3. Create a Next.js app environment for `web/` with `npm install`.
4. Provision Supabase and apply `supabase/schema.sql`.
5. Replace mock API logic with real Supabase storage/database integration.
