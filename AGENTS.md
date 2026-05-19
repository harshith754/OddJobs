# AGENTS.md

This is the canonical agent instruction file for this repository.

## Scope

- Applies to the whole monorepo unless a deeper `AGENTS.md` overrides it.
- Keep this file concise. Add only project-specific rules agents cannot reliably infer from code or standard tooling.

## Working Model

- This is a spec-driven monorepo.
- Start from `specs/` before changing implementation.
- Current canonical product spec: [specs/001-frame-stream/spec.md](specs/001-frame-stream/spec.md)
- If code and spec disagree, update the spec first or explicitly record the deviation in the same task.

## Monorepo Layout

- `android/`: Android app (`OddJobs`)
- `web/`: Next.js viewer and API routes
- `supabase/`: SQL schema and storage model
- `docs/`: supporting implementation notes
- `specs/`: product and delivery specs

## Tech Stack

### Android

- Kotlin `1.9.24`
- Android Gradle Plugin `8.5.2`
- Compile SDK `35`
- Min SDK `29`
- Jetpack Compose with compiler extension `1.5.14`
- Navigation Compose `2.7.7`
- Material 3 via Compose BOM `2024.06.00`

### Web

- Next.js `15.0.3`
- React `18.3.1`
- TypeScript `5.6.3`

### Backend/Data

- Supabase Postgres
- Supabase Storage

## Commands

Run commands from the repo root unless a command explicitly changes directory.

### Android

- First-time bootstrap in Android Studio: open `android/` and let Android Studio create/update the Gradle wrapper if missing.
- Build debug APK after wrapper exists: `cd android && ./gradlew assembleDebug`
- Build release APK after signing is configured: `cd android && ./gradlew assembleRelease`
- Run unit tests after wrapper exists: `cd android && ./gradlew testDebugUnitTest`

### Web

- Install dependencies: `cd web && npm install`
- Run dev server: `cd web && npm run dev`
- Production build: `cd web && npm run build`

### Supabase

- Apply schema manually from `supabase/schema.sql`
- Treat storage bucket creation as a manual infrastructure step unless explicit automation is added

## Delivery Order

When implementing a spec, prefer this sequence:

1. update or confirm the relevant spec
2. shape schema/contracts
3. implement backend/API surfaces
4. implement Android or web consumers
5. run the smallest relevant verification commands

## Project Conventions

- `OddJobs` is the container app. `Frame Stream` is the first real odd job.
- V1 assumes one long-lived personal `Frame Stream` with a stable public token.
- Each start/stop creates a session.
- History is part of the MVP, not a later add-on.
- Background behavior target is: capture continues while the user switches to normal non-camera apps.
- Do not add login/account flows unless the spec changes.

## Testing Rules

- Prefer narrow validation tied to the files changed.
- For Android UI/state changes, add unit or instrumentation coverage only where logic becomes non-trivial.
- For web API changes, verify route input/output shapes and failure states.
- Before finishing a task, run the smallest relevant build/test commands that are actually available in the repo state.
- If a command cannot run because the wrapper, dependencies, SDK, or network setup is missing, say that explicitly.

## Ask First

- Adding new runtime dependencies in `android/` or `web/`
- Changing the database schema in a way that breaks existing API contracts
- Introducing auth, accounts, or public sharing semantics
- Deleting stored images automatically with a new retention policy
- Changing the stable stream-link model from reusable to per-session

## Never

- Never commit secrets, `.env` files, signing keys, or Supabase service-role credentials
- Never replace the spec-driven workflow with code-first drift
- Never treat `Frame Stream` as live video streaming for MVP
- Never assume screen-off capture is a guaranteed product promise unless the spec is updated and validated on target hardware

## File Placement

- Keep root-wide rules here.
- Add deeper `AGENTS.md` files only when subproject-specific rules become substantial enough to justify local overrides.
