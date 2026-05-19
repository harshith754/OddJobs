#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DB_DIR="$ROOT_DIR/db"

if ! command -v psql >/dev/null 2>&1; then
  echo "psql is not installed"
  echo "Install PostgreSQL client tools first, then rerun this script."
  exit 1
fi

if [[ -f "$DB_DIR/.env" ]]; then
  # shellcheck disable=SC1091
  source "$DB_DIR/.env"
fi

if [[ -z "${SUPABASE_DB_URL:-}" ]]; then
  echo "SUPABASE_DB_URL is not set."
  echo "Copy db/.env.example to db/.env and fill it in."
  exit 1
fi

psql "$SUPABASE_DB_URL" <<'SQL'
create table if not exists oddjobs_schema_migrations (
    filename text primary key,
    applied_at timestamptz not null default now()
);
SQL

shopt -s nullglob
for file in "$DB_DIR"/migrations/*.sql; do
  filename="$(basename "$file")"
  already_applied="$(
    psql "$SUPABASE_DB_URL" -tAc \
      "select 1 from oddjobs_schema_migrations where filename = '$filename' limit 1"
  )"

  if [[ "$already_applied" == "1" ]]; then
    echo "Skipping already applied migration: $filename"
    continue
  fi

  echo "Applying migration: $filename"
  psql "$SUPABASE_DB_URL" -v ON_ERROR_STOP=1 -f "$file"
  psql "$SUPABASE_DB_URL" -v ON_ERROR_STOP=1 -c \
    "insert into oddjobs_schema_migrations (filename) values ('$filename')"
done
