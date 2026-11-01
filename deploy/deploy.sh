#!/usr/bin/env bash
# Deploys one image version on the server (called by GitHub Actions over SSH): /opt/nikahgator/deploy.sh <tag>
# check settings -> back up DB -> pull images -> restart -> health check; roll back if unhealthy.
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/nikahgator}"
TAG="${1:?usage: deploy.sh <image-tag>}"
COMPOSE=(docker compose -f "$APP_DIR/docker-compose.prod.yml" --env-file "$APP_DIR/.env")
STATE_FILE="$APP_DIR/.deployed-tag"
BACKUP_DIR="$APP_DIR/backups"

cd "$APP_DIR"
log() { echo "[deploy $(date '+%F %T')] $*"; }
trap 'rc=$?; log "ERROR: step failed (exit $rc) at line $LINENO: $BASH_COMMAND"' ERR

envval() { { grep -E "^$1=" .env 2>/dev/null || true; } | tail -n1 | cut -d= -f2- | sed -e 's/^["'"'"']//' -e 's/["'"'"']$//'; }
setval() {
  local key="$1" value="$2" tmp; tmp="$(mktemp)"
  grep -vE "^#?\s*$key=" .env > "$tmp" || true
  printf '%s=%s\n' "$key" "$value" >> "$tmp"
  cat "$tmp" > .env && rm -f "$tmp"
}
rand() { local s; s="$(openssl rand -base64 120 | tr -dc 'A-Za-z0-9')"; printf '%s' "${s:0:$1}"; }

# ---- 1. Settings ----
if [ ! -f .env ]; then
  log "No .env yet: creating one with random passwords and JWT secret."
  umask 077
  cat > .env <<ENV
DB_NAME=nikahgator
DB_USER=nikahgator
DB_PASSWORD=$(rand 32)
DB_ROOT_PASSWORD=$(rand 32)
JWT_SECRET=$(rand 64)
JWT_EXPIRATION_MS=28800000
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=
PUBLIC_URL=http://localhost
HTTP_BIND=127.0.0.1:8082
CADDY_NETWORK=rahbar_default
JAVA_OPTS=-Xms256m -Xmx768m
ENV
  chmod 600 .env
fi
if [ -f .incoming-secrets ]; then
  while IFS='=' read -r key value; do
    case "$key" in MAIL_USERNAME|MAIL_PASSWORD|MAIL_FROM|MAIL_HOST|MAIL_PORT) [ -n "$value" ] && setval "$key" "$value" ;; esac
  done < .incoming-secrets
  rm -f .incoming-secrets; chmod 600 .env
fi
JWT="$(envval JWT_SECRET)"
if [ "${#JWT}" -lt 32 ]; then log "JWT_SECRET must be a long random value."; exit 1; fi
HEALTH_URL="http://${HEALTH_ADDR:-$(envval HTTP_BIND)}/api/public/health"

PREVIOUS_TAG="$(cat "$STATE_FILE" 2>/dev/null || true)"
log "Deploying $TAG (currently running: ${PREVIOUS_TAG:-nothing})"

# ---- 2. Backup ----
mkdir -p "$BACKUP_DIR"
if docker ps --format '{{.Names}}' | grep -qx nikahgator-mysql; then
  FILE="$BACKUP_DIR/nikahgator-$(date +%Y%m%d-%H%M%S)-before-${TAG:0:12}.sql.gz"
  log "Backing up the database to $FILE"
  docker exec nikahgator-mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers "$MYSQL_DATABASE"' | gzip > "$FILE"
  ls -1t "$BACKUP_DIR"/nikahgator-*-before-*.sql.gz 2>/dev/null | tail -n +21 | xargs -r rm -f || true
fi

# ---- 3. Pull and start ----
export TAG
for svc in backend frontend; do
  for attempt in 1 2 3 4 5 6; do
    if "${COMPOSE[@]}" pull --quiet "$svc"; then break; fi
    [ "$attempt" = 6 ] && { log "ERROR: could not download the $svc image."; exit 1; }
    log "Download of $svc failed (try $attempt of 6), retrying"; sleep $((attempt * 10))
  done
done
"${COMPOSE[@]}" up -d --remove-orphans

# ---- 4. Health check ----
for i in $(seq 1 36); do
  if curl -fsS -o /dev/null --max-time 5 "$HEALTH_URL"; then
    echo "$TAG" > "$STATE_FILE"; log "Version $TAG is up and healthy."
    docker image prune -af --filter "until=168h" >/dev/null 2>&1 || true
    exit 0
  fi
  sleep 5
done

# ---- 5. Roll back ----
log "Health check failed. Recent backend log:"
docker logs --tail 80 nikahgator-backend 2>&1 || true
if [ -n "$PREVIOUS_TAG" ] && [ "$PREVIOUS_TAG" != "$TAG" ]; then
  log "Rolling back to $PREVIOUS_TAG"; export TAG="$PREVIOUS_TAG"; "${COMPOSE[@]}" up -d --remove-orphans
fi
exit 1
