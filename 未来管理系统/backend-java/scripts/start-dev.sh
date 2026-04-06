#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "启动开发环境（macOS）…"

if [ -f .env.dev ]; then
  set -a
  # shellcheck disable=SC1091
  source .env.dev
  set +a
fi

mkdir -p logs uploads

if [ -f scripts/use-java17.sh ]; then
  # shellcheck disable=SC1091
  source scripts/use-java17.sh
fi

echo "启动 Spring Boot（profile=dev）…"
exec mvn spring-boot:run -Dspring-boot.run.profiles=dev
