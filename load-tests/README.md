# Pruebas de carga (k6)

Requiere el stack levantado (`docker compose -f ../docker-compose.seed.yml up -d --build`)
y la imagen `grafana/k6`.

## Carga sostenida

0 -> 150 VUs en 3 min, meseta de 150 VUs por 7 min, bajada a 0 en 2 min.

```bash
docker run --rm -i --network host -v "$PWD":/scripts grafana/k6 run \
  -e BASE_URL=http://localhost:18091 --summary-export=/scripts/results-sustained.json \
  /scripts/sustained.js
```

## Spike test

0 -> 750 VUs en 30s, meseta de 1 min, bajada a 0 en 30s (~10x la carga sostenida).

```bash
docker run --rm -i --network host -v "$PWD":/scripts grafana/k6 run \
  -e BASE_URL=http://localhost:18091 --summary-export=/scripts/results-spike.json \
  /scripts/spike.js
```

`--network host` solo funciona en Linux; en Mac/Windows con Docker Desktop usa
`-e BASE_URL=http://host.docker.internal:18091` en vez de `--network host`.

## Breakpoint (Fase8)

A diferencia de `sustained.js`/`spike.js` (que no llegan a degradarse ni con 750 VUs),
`breakpoint.js` sube la tasa de llegada de peticiones sin techo (`ramping-arrival-rate`,
500 -> 10 000 req/s) sobre la misma mezcla de endpoints, con `abortOnFail` en los
thresholds: en cuanto `p(95)` supera 500ms o el error rate supera 1%, k6 corta la prueba
ahi mismo. Ese punto es el breakpoint real, no un numero elegido a mano.

```bash
docker run --rm -i --network host -v "$PWD":/scripts grafana/k6 run \
  -e BASE_URL=http://localhost:18091 --summary-export=/scripts/results-breakpoint.json \
  /scripts/breakpoint.js
```

**Resultados reales (2026-09-26, `docker-compose.demo.yml`, misma maquina, backend
precalentado con ~200 peticiones antes de cada corrida para evitar medir el arranque
en frio de la JVM):**

| Indicador | Antes (`DB_POOL_MAX_SIZE=10`, valor por defecto de Hikari) | Despues (`DB_POOL_MAX_SIZE=30`) |
|---|---:|---:|
| Throughput al momento del corte | 5 658 req/s | 6 301 req/s (+11%) |
| Tasa de llegada (iteraciones/s) al corte | 2 549 iters/s | 2 948 iters/s (+16%) |
| VUs activas al momento del corte | 2 105 | 2 754 |
| p95 en el momento del corte | 503ms (cruzo el umbral de 500ms) | 509ms (cruzo el umbral de 500ms) |
| Error rate | 0.00% (el sistema nunca devolvio errores; el corte es solo por latencia) | 0.00% |

`results-breakpoint-before.json`/`results-breakpoint-after.json` en este directorio son
la salida cruda de esas dos corridas (`--summary-export`). El pool de HikariCP en 10
(valor por defecto senalado como pendiente en revisiones anteriores) explicaba una
fraccion real, aunque no toda, del techo: subirlo a 30 movio el breakpoint un ~11-16%
mas alla antes de que la latencia se degrade, con 0% de errores en ambos casos (el
sistema nunca cae, solo se vuelve mas lento). El resto del techo en esta maquina de
desarrollo es CPU del propio contenedor del backend, no la base de datos.
`DB_POOL_MAX_SIZE` es una variable de entorno real de `docker-compose.seed.yml`
(`spring.datasource.hikari.maximum-pool-size` en `application.yml`).

## Qué no se prueba y por qué

`POST /api/v1/driver/deliveries/confirm` no esta incluido: cada factura solo se puede
confirmar una vez (regla real del dominio, no una limitacion de la prueba), asi que no
hay forma de repetirlo miles de veces sin generar una factura nueva por cada llamada.
Se prueban en cambio los endpoints de lectura, que son los que reciben la mayor parte
del trafico real de la aplicacion.
