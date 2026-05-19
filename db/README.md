# Database

This folder is provider-neutral on purpose.

## Structure

- `migrations/`: ordered schema changes

## Workflow

1. Add schema changes as new files in `migrations/`
2. Apply them in order to the active database provider
3. Do not maintain a duplicate schema snapshot file

## Current Provider Target

The current target provider is Supabase because it gives:

- Postgres database
- object storage
- a free tier to start

But the folder layout and migration history are intentionally provider-neutral so the project can move later without renaming core repository structure.

## Initial Setup

1. Create a project in the chosen provider
2. Create an object storage bucket named `stream-images`
3. Apply `migrations/0001_initial_schema.sql`
4. Copy `web/.env.example` to `web/.env.local`
5. Fill in:
   - `SUPABASE_URL`
   - `SUPABASE_SERVICE_ROLE_KEY`
   - `SUPABASE_STORAGE_BUCKET`

## Notes

- Store image metadata in SQL tables
- Store actual uploaded image files in object storage
- Do not store full image blobs directly in the database

