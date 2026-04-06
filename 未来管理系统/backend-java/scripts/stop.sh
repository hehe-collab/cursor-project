#!/usr/bin/env bash
PORT="${SERVER_PORT:-3001}"
echo "停止占用端口 ${PORT} 的进程…"
PIDS=$(lsof -ti ":${PORT}" 2>/dev/null || true)
if [ -z "${PIDS}" ]; then
  echo "端口 ${PORT} 无监听进程"
  exit 0
fi
echo "$PIDS" | xargs kill -15 2>/dev/null || true
sleep 2
PIDS2=$(lsof -ti ":${PORT}" 2>/dev/null || true)
if [ -n "${PIDS2}" ]; then
  echo "强制结束…"
  echo "$PIDS2" | xargs kill -9 2>/dev/null || true
fi
echo "完成"
