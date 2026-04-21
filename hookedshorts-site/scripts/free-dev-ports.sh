#!/usr/bin/env bash
# Frees the *listener* on each port (only LISTEN, not random clients connecting to 3100).
set -euo pipefail
for p in 3100 3005 3210; do
  pids=$(lsof -n -P -iTCP:"$p" -sTCP:LISTEN -t 2>/dev/null || true)
  if [ -n "${pids:-}" ]; then
    echo "Listener on port $p -> PID $pids — stopping"
    kill $pids 2>/dev/null || true
    sleep 0.2
    pids2=$(lsof -n -P -iTCP:"$p" -sTCP:LISTEN -t 2>/dev/null || true)
    if [ -n "${pids2:-}" ]; then
      kill -9 $pids2 2>/dev/null || true
    fi
  fi
done
echo "Done. You can run: npm run dev"
