#!/usr/bin/env bash
set -euo pipefail

# Resolve the repo directory so sourcing works even if we invoke the script
# from another working directory (e.g. via cron or a CI pipeline).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="${PROJECT_DIR:-$SCRIPT_DIR}"

# Load production overrides (database credentials, etc.) if present. This lets us
# keep secrets outside of version control while still exporting them for the
# compose deployment below.
if [[ -f "$PROJECT_DIR/.env.production" ]]; then
  echo ">> loading environment overrides from $PROJECT_DIR/.env.production"
  # shellcheck disable=SC1091
  set -a
  source "$PROJECT_DIR/.env.production"
  set +a
fi

cd "$PROJECT_DIR"
BRANCH="${1:-master}"

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-mysql,prod}"
export SPRING_JPA_HIBERNATE_DDL_AUTO="${SPRING_JPA_HIBERNATE_DDL_AUTO:-validate}"

echo ">> using SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE (Flyway migrations will run on backend startup)"
echo ">> using SPRING_JPA_HIBERNATE_DDL_AUTO=$SPRING_JPA_HIBERNATE_DDL_AUTO"

echo ">> fetch latest on $BRANCH"
git fetch --all --prune
git checkout "$BRANCH"
git pull

echo ">> build images"
docker compose build --pull backend frontend

echo ">> deploy"
docker compose up -d --remove-orphans

echo ">> status"
docker compose ps

