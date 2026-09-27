# Ruta · Gestion de entregas

Aplicacion independiente con frontend React, backend Java 17 / Spring Boot y
PostgreSQL. Gestiona sus propias facturas, productos, PIN, fotos, coordenadas,
conductores e historial. El paquete Java es `com.ruta.deliverypin`.

## Desplegar con datos de muestra

Todo el tema de `docker-compose` vive en un solo lugar, [`deploy/`](deploy/) — el mismo
archivo (`deploy/docker-compose.yml`) sirve para desarrollo local, staging y produccion,
solo cambia el `--env-file` y los `profiles` activos. Necesitas Docker Desktop abierto.
Para generar lotes adicionales tambien necesitas Python 3; la carga inicial no requiere
Python ni ejecutar un script manualmente.

### 1. Configurar los secretos

Copia el archivo de ejemplo y define tus propios valores (contrasena de la base de
datos, `JWT_SECRET` y la contrasena del administrador):

```bash
cp deploy/env/local.env.example deploy/env/local.env
```

`deploy/env/local.env` no se comitea (ver `.gitignore`).

### 2. Levantar los servicios

Desde la raiz del proyecto (PowerShell, bash o cualquier shell con Docker):

```bash
docker compose -f deploy/docker-compose.yml --env-file deploy/env/local.env up -d --build --wait
```

Este comando levanta PostgreSQL, un job `seed` que carga `demo/invoices.json` (facturas,
PIN y un conductor de muestra) y termina, y despues el backend, el frontend y un nginx
local (sin TLS, ver `deploy/nginx/templates/local.conf.template`) que sirve todo por un
solo puerto. El backend real siempre arranca con `APP_SEED_ENABLED=false` -el guard de
secretos (`SecretsGuardRunner`) sigue activo incluso en este stack de muestra-; solo el
job `seed` usa `APP_SEED_ENABLED=true` momentaneamente. `--build` construye las imagenes
desde `./backend`/`./frontend` en vez de bajarlas de GHCR. `--wait` no retorna hasta que
todos los servicios con healthcheck esten `healthy`. Puedes consultar el estado y los
logs:

```powershell
docker compose -f deploy/docker-compose.yml --env-file deploy/env/local.env ps
docker compose -f deploy/docker-compose.yml --env-file deploy/env/local.env logs --tail 50 backend
```

### 3. Consultar las facturas y los PIN

- Aplicacion y API: http://localhost:8080 (la app en `/`, la API en `/api/`, detras del
  mismo nginx)
- Administrador: `admin` / la contrasena que definiste en `ADMIN_PASSWORD` (tu
  `deploy/env/local.env`)
- Conductor existente: `conductor` / `conductor123`

Entra como administrador y abre **Facturas** para consultar los datos y los PIN.
En una instalacion nueva, crea el conductor desde **Equipo**. El administrador
se crea automaticamente. `demo/invoices.json` carga las 59 facturas originales
solo si no hay facturas en la base. El conjunto contiene 314 lineas de productos
e incluye las 50 facturas aleatorias de supermercado. Sus numeros y PIN estan en
[`demo/facturas_y_pines.csv`](demo/facturas_y_pines.csv).
El volumen de PostgreSQL conserva usuarios, entregas, fotos y facturas.
Si ya tienes facturas cargadas, volver a levantar los servicios no las reemplaza
ni vuelve a importar el JSON.

### 4. Generar otras 50 facturas (opcional)

Con los servicios encendidos, ejecuta desde la misma carpeta:

```powershell
python scripts/generate_invoices.py --count 50
```

El script crea y publica 50 facturas adicionales con productos de supermercado,
numeros aleatorios como `001-104-0000001234` y PIN de seis digitos. Se agregan a las
facturas existentes. Actualiza la pantalla **Facturas** para verlas.

El CSV del nuevo lote se guarda en `demo/nuevo_lote_facturas_y_pines.csv`.
Cada ejecucion reemplaza ese CSV, pero conserva las facturas anteriores en la base.
Puedes cambiar `--count 50` por la cantidad deseada, entre 1 y 1000.
El script usa por defecto la API `http://localhost:8080` y `admin` / la contrasena de
tu `deploy/env/local.env` (`ADMIN_PASSWORD`); configura `RUTA_API`, `RUTA_ADMIN` y
`RUTA_PASSWORD` si cambias esos valores.

### 5. Apagar el entorno conservando los datos

```powershell
docker compose -f deploy/docker-compose.yml --env-file deploy/env/local.env down
```

Para volver a iniciarlo, quita `seed` de `COMPOSE_PROFILES` (ya no hace falta resembrar)
y repite el comando del paso 2 sin `--build` si no cambiaste codigo. Las facturas, los
PIN y las entregas permanecen guardados en el volumen de PostgreSQL.

## Uso

- **Panorama:** mapa, resultados y actividad del equipo.
- **Facturas:** buscar, crear un borrador, publicar/generar PIN y exportar CSV.
- **Equipo:** crear cuentas, activar/desactivar y eliminar usuarios.
- **Conductor:** buscar factura, verificar productos, capturar foto y confirmar
  con PIN y GPS; tambien permite reportar incidencias y consultar historial.

Solo los administradores pueden consultar los PIN. La busqueda del conductor
muestra hasta 20 resultados; utiliza el numero completo para encontrar una factura.
En el navegador permite el acceso a ubicacion/camara desde `localhost`.

## API de facturas (administrador)

- `GET /api/v1/admin/invoices?q=`: consultar facturas, estados y PIN.
- `POST /api/v1/admin/invoices`: crear borrador.
- `POST /api/v1/admin/invoices/{id}/publish`: publicar; genera un PIN de seis digitos
  si se requiere y no existe, y conserva un PIN generado anteriormente.

Ejemplo de creacion:

```json
{
  "number": "001-104-0000001234",
  "partnerName": "Cliente de prueba",
  "deliveryAddress": "Calle de prueba, Guayaquil",
  "latitude": -2.170998,
  "longitude": -79.922359,
  "requiresPin": true,
  "products": [{"description": "Arroz blanco - funda 1 kg", "quantity": 2}]
}
```

La confirmacion y la foto/historial se guardan en una unica transaccion. Se rechazan
facturas inexistentes, borradores, facturas sin PIN, PIN incorrectos y entregas
repetidas. Las confirmaciones simultaneas se serializan mediante bloqueo de fila.

## Costo por entrega verificada (showback, administrador)

Reporte informativo (Fase1 §4.5): no genera cargos contables ni cobra a nadie.

- `GET /api/v1/admin/cost?month=yyyy-MM`: entregas confirmadas y GB de evidencia del mes
  (calculados por el sistema), mas el costo de infraestructura, almacenamiento y soporte
  segun las tarifas configuradas, y el costo por entrega resultante.
- `PUT /api/v1/admin/cost/support-hours`: registra las horas de soporte del mes
  (`{"month": "2026-09", "hours": 10}`); varian cada mes, por eso no son variable de entorno.

Las tarifas de infraestructura, almacenamiento y soporte se configuran con
`APP_COST_INFRA_MONTHLY_USD`, `APP_COST_STORAGE_PER_GB_USD` y `APP_COST_SUPPORT_HOUR_USD`
(0 por defecto). Con 0, el panel sigue mostrando entregas confirmadas y GB de evidencia,
que son datos reales del sistema.

## Circuit Breaker (administrador)

Protege `LocalInvoiceAdapter` (la simulacion del ERP de Fase1 §3), en linea con el riesgo
de integracion que el propio documento anticipa (Fase1 §5.1). Activado por defecto.

- `GET /api/v1/admin/resilience/status`: estado actual (`CLOSED`/`OPEN`/`HALF_OPEN`), tasa de
  fallo y fallos simulados pendientes.
- `POST /api/v1/admin/resilience/simulate-failures` con `{"count": 6}`: activa N fallos
  simulados en las proximas llamadas a facturas, para probar el patron sin depender de que
  algo externo falle de verdad. Tras eso, busca una factura desde la pantalla del conductor
  varias veces: el breaker deberia abrirse (`OPEN`, respuestas 503 sin tocar la base de
  datos), y tras `APP_CIRCUIT_BREAKER_WAIT_SECONDS` pasar a `HALF_OPEN` y luego `CLOSED` si
  las siguientes llamadas tienen exito.

Se configura con `APP_CIRCUIT_BREAKER_ENABLED` (`true` por defecto; en `false` las llamadas
van directas, sin el breaker), `APP_CIRCUIT_BREAKER_FAILURE_RATE` (umbral de fallo, 50% por
defecto) y `APP_CIRCUIT_BREAKER_WAIT_SECONDS` (tiempo en estado abierto, 15s por defecto).

## Desarrollo y despliegue

Para verificar el flujo completo sin reiniciar las facturas existentes:

```powershell
python scripts/verify_delivery.py
```

Las pruebas crean sus propios pedidos con prefijo `VERIFY/`. Comprueban permisos,
preservacion de datos, publicacion, PIN, concurrencia, foto, GPS e incidencias.

### Pruebas automatizadas

Backend (incluye pruebas de integracion con Testcontainers contra un Postgres real;
requiere Docker y montar su socket dentro del contenedor que corre Maven):

```bash
docker run --rm -v "$PWD/backend":/app -v maven-repo-cache:/root/.m2 \
  -v /var/run/docker.sock:/var/run/docker.sock -w /app \
  maven:3.9-eclipse-temurin-17 mvn test
```

El reporte de cobertura (JaCoCo) queda en `backend/target/site/jacoco/index.html`.
Sin Docker disponible para Testcontainers, `mvn test -Dtest='!*IntegrationTest'` corre
el resto de la suite (excluye solo la prueba que necesita un Postgres real).

Frontend (TypeScript + pnpm; requiere Node 22+ — `jsdom` 30/`undici` 8 usan una API del
navegador que Node 20 todavia no implementa y el arranque de vitest falla con
`webidl.util.markAsUncloneable is not a function`, por eso `frontend/Dockerfile` y
`.github/workflows/ci.yml` ya usan Node 24; con Corepack habilitado, `pnpm install`
activa sola la version de pnpm fijada en `frontend/package.json` -> `packageManager`):

```bash
cd frontend && corepack enable && pnpm install --frozen-lockfile && pnpm exec vitest run
```

`pnpm run build` corre `tsc -b` (chequeo de tipos) antes de `vite build`; el build
falla si hay errores de tipos.

El backend usa `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_POOL_MAX_SIZE`,
`JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`,
`APP_COST_INFRA_MONTHLY_USD`, `APP_COST_STORAGE_PER_GB_USD`, `APP_COST_SUPPORT_HOUR_USD`,
`APP_CIRCUIT_BREAKER_ENABLED`, `APP_CIRCUIT_BREAKER_FAILURE_RATE`, `APP_CIRCUIT_BREAKER_WAIT_SECONDS`,
`APP_RETRY_ENABLED`, `APP_RETRY_MAX_ATTEMPTS`, `APP_RETRY_WAIT_MILLIS`,
`APP_CACHE_INVOICE_LINES_TTL_SECONDS`, `APP_CACHE_EXPECTED_LOCATION_TTL_SECONDS` y `APP_CACHE_MAX_ENTRIES`.
`APP_SEED_ENABLED` es `false` por defecto. Para cargar datos de muestra,
habilitalo y configura `APP_SEED_FILE` con la ruta del JSON.

Todo el `docker-compose` del proyecto vive en un solo archivo, [`deploy/docker-compose.yml`](deploy/docker-compose.yml)
(antes habia un `docker-compose.seed.yml` aparte en la raiz solo para el stack local de
arriba, que duplicaba el servicio `seed` y buena parte del backend; se unifico todo aqui).
El mismo archivo sirve para los tres entornos:

- **Local (desarrollo):** todo en un host, sin TLS, construyendo las imagenes en vez de
  bajarlas de GHCR (ver arriba, `deploy/env/local.env.example`).
- **Escenario A (staging/produccion actual):** backend + Postgres + nginx en una VM
  (EC2); el frontend se publica aparte en **Cloudflare Pages**.
- **Escenario B:** backend, Postgres, nginx y frontend en la misma VM, detras del mismo
  nginx.

Los tres ejes de variacion se resuelven cada uno con la herramienta que le corresponde:

| Eje | Mecanismo |
|---|---|
| Entorno (local/staging/produccion): URLs, secretos, tags de imagen | `--env-file deploy/env/<entorno>.env` |
| Topologia (¿Postgres local? ¿frontend en esta VM?) | `profiles` de Compose (`db`, `frontend`, `seed`) |
| Config de nginx segun topologia | `NGINX_SITE=api` (solo backend), `full` (backend + frontend, con TLS) o `local` (backend + frontend, sin TLS) |

Uso (el mismo script que ejecuta CD en la VM, ver abajo):

```bash
cp deploy/env/production.env.example deploy/env/production.env
# completar deploy/env/production.env: SERVER_NAME, DB_PASSWORD, JWT_SECRET,
# ADMIN_PASSWORD, CORS_ALLOWED_ORIGINS, BACKEND_IMAGE/FRONTEND_IMAGE, etc.
deploy/scripts/deploy.sh production v1.4.0
```

Notas:

- Las imagenes se publican en GHCR por el workflow `cd.yml`
  (`ghcr.io/<owner>/ruta-backend` / `ruta-frontend`). Se despliega siempre un tag
  **inmutable** (`vX.Y.Z` en produccion, `sha-xxxxxxx` en staging), nunca `latest`.
- **TLS con Cloudflare:** genera un *Origin Certificate* (dashboard de Cloudflare ->
  SSL/TLS -> Origin Server) y guardalo como `deploy/certs/origin.pem` /
  `deploy/certs/origin-key.pem` (ignorados por git). Pon el dominio en modo **Full
  (strict)**. `deploy/nginx/snippets/cloudflare-realip.conf` restaura la IP real del
  cliente (necesaria para el rate-limit de login en `AuthController`).
- **Primer arranque con datos iniciales:** corre una vez
  `COMPOSE_PROFILES=db,seed deploy/scripts/deploy.sh staging <tag>`. Un servicio `seed`
  de una sola pasada carga `demo/invoices.json` (o el archivo que apunte `APP_SEED_FILE`,
  si en produccion real se reemplaza por un dataset inicial propio en vez del de muestra)
  y termina; el backend real siempre corre con `APP_SEED_ENABLED=false` (el guard de
  secretos sigue exigiendo `JWT_SECRET` y `ADMIN_PASSWORD` propios). Solo en el primer
  arranque, nunca de forma continua: sin ERP externo que alimente facturas nuevas, esas se
  crean desde el panel (`Admin -> Facturas`) o con `scripts/generate_invoices.py`, no
  volviendo a activar el profile `seed`.
- **Robustez en la VM** (`deploy/docker-compose.yml`): rotacion de logs (`LOG_MAX_SIZE`/
  `LOG_MAX_FILE`), limites de memoria (`BACKEND_MEM_LIMIT`, `POSTGRES_MEM_LIMIT`; la JVM
  dimensiona el heap con `-XX:MaxRAMPercentage=75`), apagado ordenado del backend
  (`server.shutdown: graceful` + `stop_grace_period: 30s`) y healthcheck sobre
  `/actuator/health/readiness`.
- **CORS** acepta patrones (`https://*.proyecto.pages.dev`), necesarios en staging para
  las previews por PR de Cloudflare Pages.
- Si prefieres correr el backend sin Docker, `backend/.env.example` sigue sirviendo de
  referencia para las mismas variables (`./backend/run.sh` las carga desde `.env`).

### `deploy/scripts/deploy.sh` y respaldos

`deploy/scripts/deploy.sh <staging|production> [tag]`:

1. `docker compose pull` del tag (si la imagen no existe, falla sin tocar lo que corre).
2. `deploy/scripts/backup.sh` -> `pg_dump -Fc` en `backups/<entorno>/` (red de seguridad
   ante migraciones Flyway).
3. `docker compose up -d --wait` (espera los healthchecks de Postgres, backend y nginx).
4. Verifica `nginx /healthz` y `backend /actuator/health/readiness`.
5. OK -> guarda el tag en `deploy/state/<entorno>.last_good`. Fallo -> **rollback
   automatico** al ultimo tag bueno y termina con error (CD queda en rojo).

Sin `tag`, redespliega el ultimo bueno. El rollback es de **imagen, no de esquema**:
Flyway es forward-only, asi que las migraciones deben ser compatibles con la version
anterior (patron expand/contract: primero agregar columnas/tablas, eliminar lo viejo en
una release posterior). Si hace falta volver tambien el esquema:

```bash
deploy/scripts/deploy.sh production v1.3.2     # imagen anterior
C="docker compose -f deploy/docker-compose.yml --env-file deploy/env/production.env"
$C stop backend                                # nadie escribe mientras se restaura
$C exec -T postgres sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists' \
  < backups/production/<fecha>-pre-v1.4.0.dump
$C start backend
```

Respaldo diario (cron en la VM; `BACKUP_RETENTION_DAYS` y, opcional, `BACKUP_S3_URI` en
el `.env` del entorno):

```bash
echo '15 3 * * * root /opt/ruta/deploy/scripts/backup.sh production cron >> /var/log/ruta-backup.log 2>&1' \
  | sudo tee /etc/cron.d/ruta-backup
```

## CI/CD y versionado

Modelo de ramas: `develop` es la rama de trabajo y se despliega a **staging**; `main`
solo recibe merges al momento de liberar y se despliega a **produccion**. CI y CD son
dos workflows separados, encadenados con `workflow_run`:

```
PR ──────────► CI (tests + build de imagenes) ──────────► Pages: preview automatico (nativo)
develop push ► CI ► CD: publish :staging/:sha-xxx ► deploy EC2 staging
                        Pages: build/deploy automatico de "develop" (nativo, en paralelo)
main push ───► CI ► CD: semantic-release vX.Y.Z ► publish :vX.Y.Z/:latest
                        ► [aprobacion "production"] ► deploy EC2 prod
                        └► back-merge main -> develop (CHANGELOG.md)
                        Pages: build/deploy automatico de "main" (nativo, en paralelo)
```

El frontend **no se despliega desde estos workflows**: Cloudflare Pages tiene su propia
integracion de Git (build y deploy nativos por rama, con previews automaticos por PR)
-- ver "Cloudflare Pages" en el setup de abajo. Reimplementar eso con `wrangler` en
Actions solo duplicaria lo que Cloudflare ya hace, y peor (sin sus previews automaticas
ni su CDN gestionando el propio despliegue).

- **`.github/workflows/ci.yml`** ("CI"): corre en cada push a `develop`/`main` y en
  cada PR. Prueba el backend (`mvn test`, incluida Testcontainers) y el frontend
  (`vitest` + `tsc` dentro de `pnpm run build`) y valida que ambos `Dockerfile`
  construyan (`push: false`, cache `type=gha`). Nunca publica imagenes ni despliega.
- **`.github/workflows/cd.yml`** ("CD"): se dispara cuando "CI" termina. El job `gate`
  solo deja continuar si CI termino en verde, fue un **push** (no un PR) del **propio
  repositorio** a `develop` o `main` -- un PR desde un fork con una rama llamada `main`
  no puede disparar un release ni un despliegue. Hace checkout del commit exacto que CI
  valido (`workflow_run.head_sha`). Jobs:
  - **`release`** (solo `main`): [`semantic-release`](https://semantic-release.gitbook.io/)
    (`.releaserc.json`, dependencias en el `package.json` de la raiz) calcula la version
    con [Conventional Commits](https://www.conventionalcommits.org/) (`feat:` -> minor,
    otro -> patch, `!:`/`BREAKING CHANGE:` -> major), actualiza `CHANGELOG.md`, crea el
    tag `vX.Y.Z` y la release de GitHub.
  - **`back-merge`** (solo si hubo release): mezcla `main` en `develop` para que el
    commit del changelog no haga divergir las ramas.
  - **`publish-images`**: publica `ruta-backend` y `ruta-frontend` en GHCR. En `main`
    (solo si hubo release): `vX.Y.Z`, `X.Y.Z`, `latest`, `sha-xxxxxxx`. En `develop`:
    `staging`, `sha-xxxxxxx`. La imagen del frontend solo se usa en el escenario B (todo
    en la VM); en el escenario A (frontend en Cloudflare Pages) no se descarga de GHCR.
  - **`deploy-backend`**: environment `staging` o `production`. Se autentica en AWS por
    **OIDC** (sin llaves de larga vida) y ejecuta en la EC2, via **SSM Run Command**
    (sin SSH ni puerto 22 abierto), `git checkout <sha>` + `deploy/scripts/deploy.sh`.
    El job falla si el despliegue falla (y en ese caso la VM ya hizo rollback).

### Setup de CD (una sola vez)

**AWS**

1. IAM -> Identity providers -> OpenID Connect: `https://token.actions.githubusercontent.com`,
   audience `sts.amazonaws.com`.
2. Rol `gha-ruta-deploy` con trust policy limitada a los environments del repo:
   ```json
   "Condition": {
     "StringEquals": { "token.actions.githubusercontent.com:aud": "sts.amazonaws.com" },
     "StringLike": { "token.actions.githubusercontent.com:sub": [
       "repo:ups-master/ruta-delivery:environment:staging",
       "repo:ups-master/ruta-delivery:environment:production" ] }
   }
   ```
   y permisos `ssm:SendCommand` (sobre el documento `AWS-RunShellScript` y las
   instancias del proyecto) y `ssm:GetCommandInvocation`.
3. La(s) EC2 con un instance profile que incluya `AmazonSSMManagedInstanceCore` (y
   `s3:PutObject` sobre el bucket si se usa `BACKUP_S3_URI`). Security group: solo
   443 (idealmente solo desde los rangos de Cloudflare); el 22 puede quedar cerrado.

**VM (por entorno)**

```bash
sudo git clone https://github.com/ups-master/ruta-delivery.git /opt/ruta  # repo privado: deploy key de solo lectura
cp /opt/ruta/deploy/env/production.env.example /opt/ruta/deploy/env/production.env  # completar
# certificados de origen de Cloudflare en /opt/ruta/deploy/certs/
echo "$GHCR_PAT" | sudo docker login ghcr.io -u <usuario> --password-stdin        # PAT con read:packages (o paquetes publicos)
```

Los comandos de SSM corren como `root`: el `docker login` y el clone deben hacerse con
ese usuario (o con `sudo`, como arriba).

**GitHub** (Settings -> Environments)

| Environment | Variables | Secrets | Proteccion |
|---|---|---|---|
| `staging` / `production` | `AWS_REGION`, `AWS_DEPLOY_ROLE_ARN`, `EC2_INSTANCE_ID`, opcional `DEPLOY_DIR` | -- | `production`: *Required reviewers* y *deployment branches* = `main` |

Ademas: Settings -> Actions -> General -> Workflow permissions -> **Read and write**
(semantic-release, back-merge y GHCR). Si `main`/`develop` tienen branch protection,
permitir que `github-actions[bot]` haga push (commit del changelog y back-merge).

**Cloudflare Pages (fuera de GitHub Actions, configuracion nativa):**

1. Dashboard de Cloudflare -> Workers & Pages -> conectar el repositorio de GitHub.
2. Configuracion de build del proyecto: *Root directory* `frontend`, *Build command*
   `pnpm run build` (ya incluye `tsc -b`, `vite build` y la sustitucion de
   `frontend/public/_headers`, ver `frontend/package.json`), *Build output directory*
   `dist`.
3. *Production branch*: `main`. Cualquier otra rama con push (incluida `develop`) y
   cada Pull Request generan sus propios *preview deployments* automaticamente --
   Cloudflare ya lo hace, no hace falta nada en `ci.yml`/`cd.yml` para esto.
4. Variables de entorno del build, **por entorno** (Settings -> Environment variables,
   separado en *Production* y *Preview*): `VITE_API_BASE_URL` (la URL real de la API de
   ese entorno, p. ej. `https://api.midominio.com/api/v1` en Production y la de staging
   en Preview) y, opcional, `VITE_MAP_TILES_URL` (si no se define, el build usa
   `https://tile.openstreetmap.org/{z}/{x}/{y}.png`; en Pages no existe el proxy
   `/map-tiles` de `frontend/nginx.conf`).
5. `CORS_ALLOWED_ORIGINS` del backend de ese entorno debe admitir el dominio de
   produccion **y** el patron de las previews (`https://*.<proyecto>.pages.dev`) --
   `SecurityConfig` ya acepta patrones de origen, no solo exactos.

**Caveat de GitHub Actions:** `workflow_run` solo empieza a dispararse una vez que el
workflow escuchado (`ci.yml`) ya existe en la rama por defecto del repositorio; la
primera vez que este cambio se mezcle a `main`, CD no corre retroactivamente para ese
push, arranca desde el siguiente.

Para probar `semantic-release` localmente (sin publicar nada, requiere pnpm):

```bash
corepack enable && pnpm install --frozen-lockfile
GITHUB_TOKEN=dummy pnpm exec semantic-release --dry-run --no-ci
```

Las Actions de terceros estan fijadas por hash de commit (no por tag mutable), con
la version legible en un comentario al final de la linea, siguiendo la practica de
seguridad recomendada para builds reproducibles.

## Documentación

`docs/` incluye el documento de negocio (`Fase1_Vision_Producto_Modelo_Negocio_API_Final.md`),
el de arquitectura y patrones (`Fase2_Arquitectura_Patrones_API.md`) y el modelo C4. La
version **vigente** del modelo C4 es `docs/sistema-pruebas-entrega-1.4.dsl` (las
revisiones `-1.0`, `-1.1`, `-1.2` y `-1.3` se conservan solo como historial de diseño, ya
superadas). `docs/EVALUACION_TECNICA.md` es la evaluación técnica del repositorio.
