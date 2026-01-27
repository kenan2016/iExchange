#!/usr/bin/env bash
set -euo pipefail

# 启动基础依赖服务（环境）
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${ROOT_DIR}/infra/docker-compose.yml"
DATA_DIR="${ROOT_DIR}/infra/data"

# 创建本地数据目录，便于持久化演示
mkdir -p "${DATA_DIR}/mysql" "${DATA_DIR}/mongo" "${DATA_DIR}/redis" "${DATA_DIR}/nacos" "${DATA_DIR}/seata"

# 启动基础组件：MySQL、MongoDB、Redis、Kafka
if command -v docker >/dev/null 2>&1; then
  docker compose -f "${COMPOSE_FILE}" up -d
else
  echo "未检测到 docker，请先安装 Docker Desktop" >&2
  exit 1
fi
