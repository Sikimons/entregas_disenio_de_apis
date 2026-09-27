#!/usr/bin/env bash
# Respaldo de la base de datos de un entorno con pg_dump (formato custom, -Fc: comprimido
# y restaurable con pg_restore). Lo llama deploy.sh antes de cada despliegue (red de
# seguridad ante una migracion Flyway que salga mal: Flyway es forward-only y el
# rollback de deploy.sh solo vuelve a la imagen anterior, no al esquema anterior) y
# tambien sirve para el respaldo diario por cron (ver README).
#
# Uso: deploy/scripts/backup.sh <staging|production> [etiqueta]
#
# Variables opcionales (en el entorno o en deploy/env/<entorno>.env):
#   BACKUP_DIR             destino local (por defecto <repo>/backups/<entorno>)
#   BACKUP_RETENTION_DAYS  dias que se conservan los dumps locales (por defecto 7)
#   BACKUP_S3_URI          si se define (s3://bucket/prefijo), sube cada dump con el
#                          rol IAM de la instancia (requiere aws cli)
#
# Solo aplica con el profile "db" (Postgres dentro del stack). Con una BD externa
# (p. ej. RDS) este script no hace nada: usa los snapshots automaticos del proveedor.
set -euo pipefail

ENV_NAME="${1:?uso: backup.sh <staging|production> [etiqueta]}"
LABEL="${2:-manual}"

DEPLOY_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$DEPLOY_DIR/env/$ENV_NAME.env"
[ -f "$ENV_FILE" ] || { echo "No existe $ENV_FILE" >&2; exit 1; }

# Toma del .env del entorno solo las variables que usa este script (no se hace
# "source" del archivo completo: trae secretos que no hacen falta en este shell).
env_value() { grep -E "^$1=" "$ENV_FILE" | tail -1 | cut -d= -f2- || true; }
BACKUP_DIR="${BACKUP_DIR:-$(env_value BACKUP_DIR)}"
BACKUP_DIR="${BACKUP_DIR:-$(dirname "$DEPLOY_DIR")/backups/$ENV_NAME}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-$(env_value BACKUP_RETENTION_DAYS)}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"
BACKUP_S3_URI="${BACKUP_S3_URI:-$(env_value BACKUP_S3_URI)}"

compose() { docker compose -f "$DEPLOY_DIR/docker-compose.yml" --env-file "$ENV_FILE" "$@"; }

if [ -z "$(compose ps --status running -q postgres 2>/dev/null)" ]; then
  echo "[backup] postgres no esta corriendo en '$ENV_NAME' (primer arranque o BD externa); se omite el respaldo."
  exit 0
fi

mkdir -p "$BACKUP_DIR"
FILE="$BACKUP_DIR/$(date -u +%Y%m%dT%H%M%SZ)-$LABEL.dump"

# Usuario y base se leen dentro del contenedor (POSTGRES_USER/POSTGRES_DB) para no
# depender de que el .env use los mismos nombres que los valores por defecto.
# shellcheck disable=SC2016 # se expanden dentro del contenedor, no en este shell
compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$FILE.partial"
mv "$FILE.partial" "$FILE"
echo "[backup] $FILE ($(du -h "$FILE" | cut -f1))"

if [ -n "$BACKUP_S3_URI" ]; then
  aws s3 cp --only-show-errors "$FILE" "${BACKUP_S3_URI%/}/$ENV_NAME/$(basename "$FILE")"
  echo "[backup] subido a ${BACKUP_S3_URI%/}/$ENV_NAME/"
fi

find "$BACKUP_DIR" -name '*.dump' -type f -mtime +"$BACKUP_RETENTION_DAYS" -print -delete \
  | sed 's/^/[backup] eliminado por retencion: /'
