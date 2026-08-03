#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Run the whole app locally with one command:  ./run.sh
#
#   MySQL (Docker)  ->  Spring Boot backend (local profile)  ->  React frontend
#
# Stop it all with:  ./stop.sh
# ---------------------------------------------------------------------------
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOGS="$ROOT/.run"
mkdir -p "$LOGS"

MYSQL_NAME="employee-mysql"
BACKEND_URL="http://localhost:8082"
FRONTEND_URL="http://localhost:5173"

log()  { printf '\033[1;34m[run]\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31m[run] ERROR:\033[0m %s\n' "$*" >&2; exit 1; }

# --- prerequisites --------------------------------------------------------
for tool in docker mvn npm curl; do
  command -v "$tool" >/dev/null 2>&1 || die "'$tool' is not installed / not on PATH"
done

# --- 1. database ----------------------------------------------------------
log "Starting MySQL ($MYSQL_NAME)..."
docker rm -f "$MYSQL_NAME" >/dev/null 2>&1 || true
docker run -d --name "$MYSQL_NAME" \
  -p 3306:3306 \
  -e MYSQL_DATABASE=employeedb \
  -e MYSQL_USER=employee \
  -e MYSQL_PASSWORD=employeepass \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -v "$ROOT/db/init.sql:/docker-entrypoint-initdb.d/init.sql" \
  mysql:8.0 >/dev/null

log "Waiting for MySQL to accept connections..."
for i in $(seq 1 60); do
  if docker exec "$MYSQL_NAME" mysqladmin ping -h localhost -prootpass >/dev/null 2>&1; then
    log "MySQL is ready."; break
  fi
  [ "$i" = 60 ] && die "MySQL did not become ready in time (see: docker logs $MYSQL_NAME)"
  sleep 2
done

# --- 2. backend -----------------------------------------------------------
log "Starting backend (Spring Boot, 'local' profile) -> $BACKEND_URL"
( cd "$ROOT/app/backend" && mvn -q spring-boot:run -Dspring-boot.run.profiles=local ) \
  > "$LOGS/backend.log" 2>&1 &
echo $! > "$LOGS/backend.pid"

log "Waiting for backend health (first run downloads Maven deps, be patient)..."
for i in $(seq 1 120); do
  if curl -fs "$BACKEND_URL/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
    log "Backend is UP."; break
  fi
  [ "$i" = 120 ] && die "Backend did not become healthy (see: $LOGS/backend.log)"
  sleep 3
done

# --- 3. frontend ----------------------------------------------------------
log "Starting frontend (Vite) -> $FRONTEND_URL"
if [ ! -d "$ROOT/app/frontend/node_modules" ]; then
  log "Installing frontend dependencies (first run only)..."
  ( cd "$ROOT/app/frontend" && npm install ) > "$LOGS/npm-install.log" 2>&1
fi
( cd "$ROOT/app/frontend" && npm run dev -- --host ) > "$LOGS/frontend.log" 2>&1 &
echo $! > "$LOGS/frontend.pid"

for i in $(seq 1 30); do
  if curl -fs -o /dev/null "$FRONTEND_URL" 2>/dev/null; then break; fi
  sleep 2
done

# --- done -----------------------------------------------------------------
cat <<EOF

$(printf '\033[1;32m✔ All tiers are running.\033[0m')

  Frontend (UI)     : $FRONTEND_URL
  Backend API       : $BACKEND_URL/api/employees
  Backend health    : $BACKEND_URL/actuator/health
  Database (MySQL)  : localhost:3306  (db: employeedb, user: employee)

  Logs              : $LOGS/backend.log , $LOGS/frontend.log
  Stop everything   : ./stop.sh

EOF
