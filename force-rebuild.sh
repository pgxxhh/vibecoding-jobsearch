#!/usr/bin/env bash
set -euo pipefail

# Force rebuild to include new migration files

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo ">> Force rebuilding backend with new migrations..."
docker compose build --no-cache --pull backend

echo ">> Force rebuild complete. Now you can run normal deploy."
echo ">> Run: sh deploy.sh"