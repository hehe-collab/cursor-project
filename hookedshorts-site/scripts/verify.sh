#!/usr/bin/env bash
# Smoke test: build + static preview HTTP 200 (macOS / Linux, bash)
set -euo pipefail
cd "$(dirname "$0")/.."
echo "== 1) npm run build =="
npm run build
echo "== 2) static serve out/ and curl =="
# Python http.server from `out/` (no extra npm deps). Next export uses privacy.html etc.
PORT="${VERIFY_PORT:-4099}"
cd out
python3 -m http.server "$PORT" > /tmp/hookedshorts-serve.log 2>&1 &
PID=$!
cd ..
sleep 1
code=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:${PORT}/" || true)
code_priv=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:${PORT}/privacy.html" || true)
kill "$PID" 2>/dev/null || true
wait "$PID" 2>/dev/null || true
if [ "$code" != "200" ] || [ "$code_priv" != "200" ]; then
  echo "FAIL: GET / = $code GET /privacy.html = $code_priv (expected 200). Log: /tmp/hookedshorts-serve.log"
  exit 1
fi
echo "OK: GET / = $code, GET /privacy.html = $code_priv"
echo "== verify.sh passed =="
