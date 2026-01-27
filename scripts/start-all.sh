#!/usr/bin/env bash
set -euo pipefail

# 启动全部服务（骨架）
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${ROOT_DIR}/logs"

# 服务列表按启动顺序排列（网关先起便于观察）
MODULES=(
  exchange-gateway
  exchange-user
  exchange-wallet
  exchange-spot
  exchange-contract
  exchange-market
)

mkdir -p "${LOG_DIR}"

for module in "${MODULES[@]}"; do
  echo "正在启动 ${module} ..."
  nohup mvn -f "${ROOT_DIR}/pom.xml" -pl "${module}" -am spring-boot:run -DskipTests \
    > "${LOG_DIR}/${module}.log" 2>&1 &
  echo $! > "${LOG_DIR}/${module}.pid"
  sleep 1
  echo "${module} 启动命令已提交，日志：${LOG_DIR}/${module}.log"
done

echo "全部服务已提交启动（骨架）。"
