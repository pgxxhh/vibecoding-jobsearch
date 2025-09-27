#!/usr/bin/env bash
set -euo pipefail
cd ~/app
BRANCH="${1:-master}"

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

