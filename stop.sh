#!/usr/bin/env bash
# Stop everything started by ./run.sh
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOGS="$ROOT/.run"
MYSQL_NAME="employee-mysql"

log() { printf '\033[1;34m[stop]\033[0m %s\n' "$*"; }

# frontend + backend by recorded PID (fall back to port/pattern)
for svc in frontend backend; do
  if [ -f "$LOGS/$svc.pid" ]; then
    kill "$(cat "$LOGS/$svc.pid")" 2>/dev/null || true
    rm -f "$LOGS/$svc.pid"
  fi
done
pkill -f "spring-boot:run" 2>/dev/null || true
pkill -f "vite" 2>/dev/null || true
fuser -k 8082/tcp 5173/tcp 2>/dev/null || true
log "Stopped backend and frontend."

# database
docker rm -f "$MYSQL_NAME" >/dev/null 2>&1 && log "Removed MySQL container." || log "No MySQL container to remove."

log "Done."
