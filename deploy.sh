#!/usr/bin/env bash
set -euo pipefail
cd ~/app
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

