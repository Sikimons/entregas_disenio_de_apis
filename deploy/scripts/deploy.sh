#!/usr/bin/env bash
# Despliega (o hace rollback de) el backend de un entorno en esta VM. Lo invoca CD
# (.github/workflows/cd.yml, via AWS SSM) y tambien se puede correr a mano.
#
# Uso: deploy/scripts/deploy.sh <staging|production> [tag]
#   tag: tag inmutable de la imagen (vX.Y.Z o sha-xxxxxxx). Sin tag, redespliega el
#        ultimo que funciono (deploy/state/<entorno>.last_good) -> rollback manual.
#
# Pasos: pull -> backup de la BD -> up --wait (espera healthchecks) -> verificacion
# por nginx -> guarda el tag como "ultimo bueno". Si algo falla despues del pull,
# vuelve automaticamente al ultimo tag bueno y termina con error.
#
# Rollback = imagen anterior, NO esquema anterior: Flyway es forward-only, asi que las
# migraciones deben ser compatibles con la version previa (expand/contract). Si hace
# falta volver el esquema, restaurar el dump que este script deja antes de cada
# despliegue (ver README -> "Rollback").
set -euo pipefail

ENV_NAME="${1:?uso: deploy.sh <staging|production> [tag]}"
DEPLOY_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$DEPLOY_DIR/env/$ENV_NAME.env"
STATE_DIR="$DEPLOY_DIR/state"
LAST_GOOD_FILE="$STATE_DIR/$ENV_NAME.last_good"
WAIT_TIMEOUT="${DEPLOY_WAIT_TIMEOUT:-240}"

[ -f "$ENV_FILE" ] || { echo "No existe $ENV_FILE (copia $ENV_FILE.example y completalo)" >&2; exit 1; }
mkdir -p "$STATE_DIR"

PREVIOUS_TAG="$(cat "$LAST_GOOD_FILE" 2>/dev/null || true)"
TAG="${2:-$PREVIOUS_TAG}"
[ -n "$TAG" ] || { echo "Sin tag y sin despliegue previo registrado en $LAST_GOOD_FILE" >&2; exit 1; }

log() { echo "[deploy $ENV_NAME] $*"; }

# Las variables del shell tienen prioridad sobre --env-file al interpolar el compose,
# asi que BACKEND_TAG/FRONTEND_TAG exportados aqui pisan los del .env del entorno.
compose() {
  BACKEND_TAG="$1" FRONTEND_TAG="$1" \
    docker compose -f "$DEPLOY_DIR/docker-compose.yml" --env-file "$ENV_FILE" "${@:2}"
}

# verify/up se llaman dentro de "if", donde set -e no aplica: cada paso corta con
# "|| return 1" explicito para que un fallo intermedio no quede oculto.
verify() {
  local tag="$1"
  # 1) nginx arriba y sirviendo (mismo endpoint que su healthcheck).
  compose "$tag" exec -T nginx wget -qO- http://127.0.0.1/healthz >/dev/null || return 1
  # 2) backend listo para trafico, a traves de la red interna del compose.
  compose "$tag" exec -T backend curl -sf http://localhost:8080/actuator/health/readiness \
    | grep -q '"status":"UP"'
}

up() {
  local tag="$1"
  # --wait: no retorna hasta que todos los servicios con healthcheck esten "healthy"
  # (falla si alguno queda unhealthy o se agota el tiempo). --remove-orphans limpia
  # servicios que ya no existan en el compose de este commit.
  compose "$tag" up -d --wait --wait-timeout "$WAIT_TIMEOUT" --remove-orphans
}

rollback() {
  if [ -z "$PREVIOUS_TAG" ] || [ "$PREVIOUS_TAG" = "$TAG" ]; then
    log "FALLO con $TAG y no hay un tag anterior distinto al que volver."
    compose "$TAG" ps || true
    compose "$TAG" logs --tail 100 backend || true
    exit 1
  fi
  log "FALLO con $TAG; logs del backend:"
  compose "$TAG" logs --tail 100 backend || true
  log "rollback a $PREVIOUS_TAG"
  if up "$PREVIOUS_TAG" && verify "$PREVIOUS_TAG"; then
    log "rollback OK: sigue corriendo $PREVIOUS_TAG"
  else
    log "el rollback a $PREVIOUS_TAG TAMBIEN fallo; revisar a mano"
  fi
  exit 1
}

log "desplegando $TAG (anterior: ${PREVIOUS_TAG:-ninguno})"

# Si la imagen no existe o no hay acceso a GHCR, se falla aqui sin tocar lo que corre.
# DEPLOY_SKIP_PULL=1 solo para probar el script con imagenes construidas localmente.
if [ "${DEPLOY_SKIP_PULL:-0}" != "1" ]; then
  compose "$TAG" pull --quiet
fi

"$DEPLOY_DIR/scripts/backup.sh" "$ENV_NAME" "pre-$TAG"

if ! up "$TAG" || ! verify "$TAG"; then
  rollback
fi

echo "$TAG" > "$LAST_GOOD_FILE"
# Libera disco de la VM: imagenes que ya no usa ningun contenedor y de mas de 7 dias
# (conserva las recientes para que un rollback no dependa de volver a bajarlas).
docker image prune -af --filter "until=168h" >/dev/null || true
log "OK: $TAG desplegado"
