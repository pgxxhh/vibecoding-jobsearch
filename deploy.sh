#!/usr/bin/env bash
set -euo pipefail

# Resolve the repo directory so sourcing works even if we invoke the script
# from another working directory (e.g. via cron or a CI pipeline).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="${PROJECT_DIR:-$SCRIPT_DIR}"

TMP_ENV_FILE=""

cleanup() {
  if [[ -n "${TMP_ENV_FILE:-}" && -f "${TMP_ENV_FILE}" ]]; then
    rm -f "${TMP_ENV_FILE}"
  fi
}

trap cleanup EXIT

# Compose reads variables from a single env file. We merge `.env` (base defaults)
# with `.env.production` (overrides for managed services) so production deploys
# don't accidentally fall back to the local dockerised MySQL host.
ENV_FILES=()
if [[ -f "$PROJECT_DIR/.env" ]]; then
  ENV_FILES+=("$PROJECT_DIR/.env")
fi

if [[ -f "$PROJECT_DIR/.env.production" ]]; then
  echo ">> loading environment overrides from $PROJECT_DIR/.env.production"
  ENV_FILES+=("$PROJECT_DIR/.env.production")
fi

COMPOSE_ENV_FILE=""
if (( ${#ENV_FILES[@]} > 0 )); then
  if (( ${#ENV_FILES[@]} > 1 )); then
    TMP_ENV_FILE="$(mktemp)"

    declare -A override_keys=()
    while IFS= read -r key; do
      [[ -z "$key" ]] && continue
      override_keys["$key"]=1
    done < <(grep -E '^[A-Za-z_][A-Za-z0-9_]*=' "$PROJECT_DIR/.env.production" | cut -d= -f1)

    while IFS= read -r line || [[ -n "$line" ]]; do
      if [[ "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)= ]]; then
        key="${BASH_REMATCH[1]}"
        if [[ -n "${override_keys[$key]:-}" ]]; then
          continue
        fi
      fi
      printf '%s\n' "$line" >> "$TMP_ENV_FILE"
    done < "$PROJECT_DIR/.env"

    printf '\n' >> "$TMP_ENV_FILE"
    cat "$PROJECT_DIR/.env.production" >> "$TMP_ENV_FILE"
    printf '\n' >> "$TMP_ENV_FILE"

    COMPOSE_ENV_FILE="$TMP_ENV_FILE"
  else
    COMPOSE_ENV_FILE="${ENV_FILES[0]}"
  fi

  set -a
  for file in "${ENV_FILES[@]}"; do
    # shellcheck disable=SC1090
    source "$file"
  done
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

COMPOSE_CMD=(docker compose)
if [[ -n "$COMPOSE_ENV_FILE" ]]; then
  COMPOSE_CMD+=(--env-file "$COMPOSE_ENV_FILE")
  echo ">> using docker compose env file $COMPOSE_ENV_FILE"
fi

echo ">> build images"
"${COMPOSE_CMD[@]}" build --pull backend frontend

echo ">> deploy"
"${COMPOSE_CMD[@]}" up -d --remove-orphans

echo ">> status"
"${COMPOSE_CMD[@]}" ps

