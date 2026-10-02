#!/usr/bin/env bash
# Corre una prueba contra el entorno que diga el archivo .env, desde cualquier maquina con
# Docker (k6 corre en el contenedor oficial grafana/k6, sin instalar nada mas). Sirve igual
# para el stack local, staging o produccion (dominio via Cloudflare o directo a la EC2).
#
# Uso: load-tests/run.sh <smoke|sustained|spike|breakpoint|dashboard-heavy|verify> <archivo.env>
#   smoke            2 VUs, 30 s: valida conectividad, TLS y credenciales (correlo primero)
#   sustained|spike|breakpoint|dashboard-heavy   escenarios k6 (ver README)
#   verify           e2e de scripts/verify_delivery.py (requiere python3); ESCRIBE facturas
#                    VERIFY/... y confirma entregas en el entorno destino
#
# El .env sale de load-tests/env/<entorno>.env.example (copialo sin ".example"). Variables:
# BASE_URL, RUTA_ADMIN, RUTA_PASSWORD, INSECURE_TLS, LOAD_SCALE, P95_MS, USER_AGENT,
# K6_DOCKER_NETWORK, RUTA_DRIVER, RUTA_DRIVER_PASSWORD, RUTA_CHECK_FIXTURES.
# ASSUME_YES=1 omite la confirmacion al apuntar a un destino que no es local.
set -euo pipefail

SCENARIO="${1:?uso: run.sh <smoke|sustained|spike|breakpoint|dashboard-heavy|verify> <archivo.env>}"
ENV_FILE="${2:?uso: run.sh <escenario> <archivo.env>}"

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$HERE")"
[ -f "$ENV_FILE" ] || { echo "No existe $ENV_FILE (copia load-tests/env/<entorno>.env.example)" >&2; exit 1; }

case "$SCENARIO" in
  smoke|sustained|spike|breakpoint|dashboard-heavy|verify) ;;
  *) echo "Escenario desconocido: $SCENARIO" >&2; exit 1 ;;
esac

# Lee VAR=valor del .env sin ejecutarlo como shell (mismo formato que docker --env-file:
# sin comillas, sin expansion), para que un "$" en una contrasena no se interprete.
env_value() { grep -E "^$1=" "$ENV_FILE" | tail -1 | cut -d= -f2- || true; }

BASE_URL="$(env_value BASE_URL)"
[ -n "$BASE_URL" ] || { echo "Falta BASE_URL en $ENV_FILE" >&2; exit 1; }
LOAD_SCALE="$(env_value LOAD_SCALE)"

echo "[run] escenario=$SCENARIO destino=$BASE_URL load_scale=${LOAD_SCALE:-1}"

# Confirmacion antes de apuntar carga (o escrituras, en verify) a algo que no es local.
if [[ ! "$BASE_URL" =~ ^https?://(localhost|127\.0\.0\.1|backend|host\.docker\.internal)([:/]|$) ]]; then
  case "$SCENARIO" in
    sustained|spike|breakpoint|dashboard-heavy|verify)
      if [ "${ASSUME_YES:-0}" != "1" ]; then
        [ -t 0 ] || { echo "Destino remoto: confirma con ASSUME_YES=1 (no hay terminal interactiva)." >&2; exit 1; }
        read -r -p "[run] '$SCENARIO' contra $BASE_URL (destino remoto). ¿Continuar? [y/N] " answer
        [[ "$answer" =~ ^[yY]$ ]] || { echo "Cancelado."; exit 1; }
      fi ;;
  esac
fi

if [ "$SCENARIO" = "verify" ]; then
  export RUTA_API="$BASE_URL"
  # Solo se exportan las que tienen valor: una variable vacia pisaria el default del script.
  for var in RUTA_ADMIN RUTA_PASSWORD RUTA_DRIVER RUTA_DRIVER_PASSWORD RUTA_CHECK_FIXTURES; do
    value="$(env_value "$var")"
    [ -z "$value" ] || export "$var=$value"
  done
  if [ "$(env_value INSECURE_TLS)" = "true" ]; then export RUTA_INSECURE_TLS=1; fi
  exec python3 "$ROOT/scripts/verify_delivery.py"
fi

# Resultados fuera del directorio de scripts (que se monta de solo lectura). k6 corre con
# el uid del usuario actual para poder escribir en ./results sin root ni chmod.
RESULTS_DIR="$HERE/results"
mkdir -p "$RESULTS_DIR"
NAME="$(basename "$ENV_FILE" .env)"
OUT="$NAME-$SCENARIO-${LOAD_SCALE:-1}-$(date -u +%Y%m%dT%H%M%SZ).json"

ARGS=(--rm -i --user "$(id -u):$(id -g)" --env-file "$ENV_FILE"
      -v "$HERE":/scripts:ro -v "$RESULTS_DIR":/results)
NETWORK="$(env_value K6_DOCKER_NETWORK)"
[ -z "$NETWORK" ] || ARGS+=(--network "$NETWORK")

docker run "${ARGS[@]}" grafana/k6 run --summary-export="/results/$OUT" "/scripts/$SCENARIO.js"
echo "[run] resumen: $RESULTS_DIR/$OUT"
