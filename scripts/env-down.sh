#!/usr/bin/env bash
set -euo pipefail

# 停止基础依赖服务（环境）
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${ROOT_DIR}/infra/docker-compose.yml"

if command -v docker >/dev/null 2>&1; then
  docker compose -f "${COMPOSE_FILE}" down
else
  echo "未检测到 docker，请先安装 Docker Desktop" >&2
  exit 1
fi
