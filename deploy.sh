#!/usr/bin/env bash
set -euo pipefail

# Resolve the repo directory so sourcing works even if we invoke the script
# from another working directory (e.g. via cron or a CI pipeline).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="${PROJECT_DIR:-$SCRIPT_DIR}"

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
    TMP_ENV_FILE="$(mktemp "$PROJECT_DIR/.env.tmp.XXXXXX")"
    cleanup_tmp() {
      rm -f "$TMP_ENV_FILE"
    }
    trap cleanup_tmp EXIT

    : > "$TMP_ENV_FILE"

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

    mv "$TMP_ENV_FILE" "$PROJECT_DIR/.env"
    trap - EXIT
    unset -f cleanup_tmp

    COMPOSE_ENV_FILE="$PROJECT_DIR/.env"
    echo ">> wrote merged environment to $COMPOSE_ENV_FILE"
  else
    COMPOSE_ENV_FILE="${ENV_FILES[0]}"
  fi

fi

read_env_var() {
  local key="$1"
  if (( ${#ENV_FILES[@]} == 0 )); then
    return 0
  fi

  python3 - "$key" "${ENV_FILES[@]}" <<'PY'
import codecs
import sys

key = sys.argv[1]
paths = sys.argv[2:]
value = ""

for path in paths:
    try:
        with open(path, encoding="utf-8") as fh:
            for raw in fh:
                line = raw.rstrip("\n\r")
                stripped = line.lstrip()
                if not stripped or stripped.startswith("#"):
                    continue
                if "=" not in line:
                    continue

                name, val = line.split("=", 1)
                name = name.strip()
                if name.startswith("export "):
                    name = name[len("export "):].strip()

                if name != key:
                    continue

                val = val.strip()
                if val and val[0] in ("'", '"') and val[-1] == val[0]:
                    quote = val[0]
                    val = val[1:-1]
                    if quote == '"':
                        try:
                            val = codecs.decode(val, "unicode_escape")
                        except Exception:
                            pass
                else:
                    hash_pos = val.find("#")
                    if hash_pos != -1:
                        val = val[:hash_pos].rstrip()

                value = val
    except FileNotFoundError:
        continue

print(value, end="")
PY
}

if [[ -z "${SPRING_DATASOURCE_URL:-}" ]]; then
  SPRING_DATASOURCE_URL="$(read_env_var SPRING_DATASOURCE_URL)"
fi

if [[ -z "${SPRING_PROFILES_ACTIVE:-}" ]]; then
  SPRING_PROFILES_ACTIVE="$(read_env_var SPRING_PROFILES_ACTIVE)"
fi

if [[ -z "${SPRING_JPA_HIBERNATE_DDL_AUTO:-}" ]]; then
  SPRING_JPA_HIBERNATE_DDL_AUTO="$(read_env_var SPRING_JPA_HIBERNATE_DDL_AUTO)"
fi

if [[ -n "${SPRING_DATASOURCE_URL:-}" && "$SPRING_DATASOURCE_URL" =~ jdbc:mysql://mysql(:|/|$) ]]; then
  echo "error: SPRING_DATASOURCE_URL still points at the docker mysql service; aborting deploy" >&2
  exit 1
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

