#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DB_DIR="$ROOT_DIR/db"

if ! command -v supabase >/dev/null 2>&1; then
  echo "supabase CLI is not installed"
  echo "Install it first, then rerun this script."
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

TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT

mkdir -p "$TEMP_DIR/supabase"
cp -R "$DB_DIR/migrations" "$TEMP_DIR/supabase/migrations"

cat > "$TEMP_DIR/supabase/config.toml" <<'EOF'
project_id = "oddjobs"

[db]
major_version = 15
EOF

cd "$TEMP_DIR"
supabase db push --db-url "$SUPABASE_DB_URL"

