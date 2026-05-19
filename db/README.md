# Database

This folder is provider-neutral on purpose.

## Structure

- `migrations/`: ordered schema changes

## Workflow

1. Add schema changes as new files in `migrations/`
2. Apply them in order to the active database provider
3. Do not maintain a duplicate schema snapshot file

## Code-Driven Migration Workflow

Use the scripts in `db/scripts/`:

- `db/scripts/push.sh`
- `db/scripts/list.sh`

These scripts:

- keep `db/` provider-neutral
- use plain `psql`
- apply migrations using `SUPABASE_DB_URL`
- track applied files in `oddjobs_schema_migrations`

This avoids baking provider-specific CLI conventions into the repo.

## Current Provider Target

The current target provider is Supabase because it gives:

- Postgres database
- object storage
- a free tier to start

But the folder layout and migration history are intentionally provider-neutral so the project can move later without renaming core repository structure.

## Initial Setup

1. Create the Supabase project
2. Copy `db/.env.example` to `db/.env`
3. Fill in `SUPABASE_DB_URL`
4. Run `db/scripts/push.sh`
5. Copy `web/.env.example` to `web/.env.local`
6. Fill in:
   - `SUPABASE_URL`
   - `SUPABASE_SECRET_KEY`
   - `SUPABASE_STORAGE_BUCKET`

The migrations include creation of the public `stream-images` bucket, so object storage setup is also driven from code.

## Notes

- Store image metadata in SQL tables
- Store actual uploaded image files in object storage
- Do not store full image blobs directly in the database
- Do not commit `db/.env` or raw database credentials

## Important

The password you shared should not be committed into the repo or hardcoded into scripts. Keep it only in `db/.env` or your shell environment.
