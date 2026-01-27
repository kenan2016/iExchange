#!/usr/bin/env bash
set -euo pipefail

# 停止全部服务（骨架）
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${ROOT_DIR}/logs"

if [ ! -d "${LOG_DIR}" ]; then
  echo "未找到日志目录，默认无需停止。"
  exit 0
fi

for pid_file in "${LOG_DIR}"/*.pid; do
  if [ -f "${pid_file}" ]; then
    pid=$(cat "${pid_file}")
    if kill -0 "${pid}" >/dev/null 2>&1; then
      echo "停止进程 ${pid}（${pid_file}）..."
      kill "${pid}"
    else
      echo "进程 ${pid} 不存在，跳过。"
    fi
    rm -f "${pid_file}"
  fi
done

echo "停止命令已执行（骨架）。"
