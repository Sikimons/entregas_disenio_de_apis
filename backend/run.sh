#!/usr/bin/env bash
# Carga backend/.env al entorno y arranca el backend con Maven.
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "No existe backend/.env. Copia los valores de ejemplo y completa tu configuracion de base de datos." >&2
  exit 1
fi

set -a
source .env
set +a

mvn spring-boot:run
