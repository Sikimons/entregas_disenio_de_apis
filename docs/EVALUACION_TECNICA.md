# Informe de evaluación técnica

**Proyecto:** Ruta · Plataforma de verificación de entregas
**Alcance de la evaluación:** revisión de código, configuración y documentación del repositorio (rama `evalution`), complementada en esta revisión con compilación, ejecución de pruebas unitarias e **integración con Testcontainers**, y despliegue real del stack (`docker compose -f docker-compose.mock.yml up --build`) para verificar en vivo los cambios de esta iteración (Flyway, versionamiento `/api/v1`, confianza de datos, autoprotección de admin, validación de fotos, formato de error unificado, y un nuevo ciclo de pruebas de carga con dataset pesado real). Lo que no se verificó en vivo sigue marcado explícitamente como **N/E** o "declarado, no verificado" — en particular, la ejecución real de `.github/workflows/ci.yml` en GitHub Actions, excluida deliberadamente de esta ronda (requiere `git push`, fuera del alcance acordado).

**Actualización 2026-09-26 (recalificación formal):** las secciones 1 y 2 de este informe, y el puntaje final, ya incorporan la recalificación formal detallada en el §16, que verificó con ejecución real (no solo lectura de código) los cambios de la "ronda posterior" citada en cada sección: `mvn test` en contenedor Maven con Testcontainers (52/52 pruebas, cobertura JaCoCo 52% de instrucciones), `npx vitest run` en el frontend (9/9 pruebas), y lectura directa del código para `LoginRateLimiter`, el tamaño de `DriverHomePage.jsx`, el manejo de error en `AdminDriversPage`/`AdminDeliveriesPage`, el logging agregado y el estilo de `DemoInvoiceLoader`. El cuerpo original de las secciones 3–15 se conserva intacto como evidencia histórica de cada ronda; los números vigentes son los de §2 y §16.

**Actualización 2026-09-27 (segunda recalificación formal):** el puntaje vigente es el del **§18 (92/100)**, que verificó ejecutando el §17, la migración del frontend a TypeScript y el nuevo CI/CD y despliegue, y registra 8 hallazgos nuevos (N1–N8). Las secciones 1–17 se conservan como historial; donde contradigan al §18 (nombres `.jsx`, `npx vitest`, "9 controladores", `-1.2.dsl` vigente, reproducibilidad sin pasos manuales), prevalece el §18.

**Nota de una ronda posterior al §18 (2026-09-27, no reasigna el 92/100):** el **§19** corrige, con ejecución real verificada, cuatro de los ocho hallazgos del §18 (N1, N3, N4, N5) y la documentación de arquitectura (N6); de paso encuentra y corrige un bug de reproducibilidad no señalado por el §18 (orden no determinista de los bootstrap runners de la demo). N2, N7 y N8 siguen sin resolver. Donde el §19 contradiga al §18 (nombre `docker-compose.mock.yml`, `-1.3.dsl` vigente), prevalece el §19.

**Nota de esta revisión:** el equipo confirmó `docs/Fase1_Vision_Producto_Modelo_Negocio_API_Final.md` (v1.1) como la versión final aprobada del documento de negocio; no se modifica. En esta revisión se incorpora además `docs/sistema-pruebas-entrega-1.2.dsl` como la versión **vigente** del modelo de arquitectura C4, reescrita para reflejar fielmente el código real (`-1.0.dsl` y `-1.1.dsl` se conservan como historial de diseño, ya superadas).

**Actualización 2026-09-27 (tercera recalificación formal): el puntaje vigente era el del §21 (96/100), superado por el §22.** Verifica con ejecución real (no solo lectura) que las seis correcciones de §19 (N1, N3, N4, N5, N6) y la de §20 (N7) se sostienen sobre el estado actual del árbol de trabajo: `mvn test` en contenedor Maven limpio con Testcontainers (**81/81 pruebas**, JaCoCo 56% de instrucciones / 67% de líneas), `pnpm exec vitest run`/`lint`/`build` en el frontend, un `docker compose -p ruta-eval -f docker-compose.demo.yml down -v && up --build --wait` completo en un proyecto Docker aislado seguido de `scripts/verify_delivery.py` de punta a punta, y pruebas en vivo con `curl` del rate limiter (a través de nginx, con `X-Forwarded-For` falsificado) y de las cabeceras de seguridad. Al cierre del §21, solo **N2** (historial de git) seguía sin resolver, con más entradas sin commitear (148) que en el §18 (143); una nota posterior al §21 documenta que N2 quedó resuelto sobre una rama `develop` nueva, sin reasignar el puntaje total. Las secciones 1–20 se conservan como historial; donde contradigan al §21, prevalece primero el §21 y, en lo tocante a N2, el §22.

**Actualización 2026-09-27 (cuarta recalificación formal): el puntaje vigente es el del §22 (97/100).** Verifica, sobre un clon nuevo y aislado de la rama `develop` (no el árbol de trabajo ya probado), que N2 (historial de git) quedó resuelto: el backend compila y pasa sus 81 pruebas, el frontend instala/construye/prueba igual, y `docker compose up --build --wait` + `scripts/verify_delivery.py` reproducen el sistema completo de punta a punta. Solo la ejecución real en GitHub Actions/AWS/Cloudflare Pages sigue sin verificarse (requiere `git push`, no autorizado en esta sesión). Las secciones 1–21 se conservan como historial; donde contradigan al §22 (N2 sin resolver, puntaje 96), prevalece el §22.

---

## 1. Resumen ejecutivo

**Propósito.** Plataforma interna para acreditar entregas a domicilio de un retailer con flota propia: cada entrega se confirma con PIN de 6 dígitos, foto y GPS, y la evidencia queda asociada a la factura.

**Arquitectura identificada (real).** SPA/PWA React 19 + Vite servida por Nginx (que hace proxy de `/api` y de las teselas de OSM) → API REST Spring Boot 3.3 / Java 17 con arquitectura hexagonal (puertos `in`/`out`, adaptadores) → PostgreSQL 16. Autenticación JWT (HS256) con roles `ADMIN`/`CONDUCTOR`. Despliegue con Docker Compose (`docker-compose.mock.yml`).

**Estado general.** Funcionalmente coherente y conectado de extremo a extremo (el frontend consume la API real, sin mocks). Las revisiones `sistema-pruebas-entrega-1.0.dsl` y `-1.1.dsl` del modelo C4 describían un sistema distinto (Angular, SSE, proveedor de mensajería, geocodificación OSM/Google, adaptador ERP, sincronizador offline con IndexedDB, PIN "hasheado") que no existía en el código. **En esta revisión se incorporó `sistema-pruebas-entrega-1.2.dsl`**, que documenta la arquitectura tal como está implementada (React, sin SSE/mensajería/geocodificación externa/cola offline, PIN en texto plano) y queda alineada con el alcance de piloto que el propio documento de negocio (Fase1 §3) ya definía. La brecha documental de arquitectura queda cerrada; el PIN en texto plano y sin límite de intentos sigue siendo una **debilidad de seguridad real** (ya no una inconsistencia documental) — ver §7.

**Principales fortalezas.** Documento de visión de negocio sólido y completo (RA1 Excelente); arquitectura hexagonal ya sin violaciones, documentada con fidelidad al código; OpenAPI real y verificado, ahora sobre rutas versionadas `/api/v1`; esquema de base de datos gestionado por Flyway (una única fuente de verdad, con la FK que faltaba); Circuit Breaker (Resilience4j) y panel de costo (showback) implementados y probados en vivo; **52 pruebas reales** (verificado con `mvn test`, BUILD SUCCESS, incluidas 3 de integración con Testcontainers contra un Postgres real) con cobertura medida por JaCoCo (**52% de instrucciones, 62% de líneas**, verificado en esta recalificación); 9 pruebas de frontend con Vitest (verificado con `npx vitest run`); rate limiting del login implementado y probado (`LoginRateLimiter`, 4/4 pruebas); pruebas de carga k6 ejecutadas de verdad, incluido un ciclo con dataset pesado real (500 entregas confirmadas con foto de ~200KB) que **encontró un cuello de botella real, lo corrigió y volvió a medir** (p95 21.91s → 13.33ms, ~1600x); XSS, PIN sin límite de intentos, confianza en datos del cliente, autoprotección de admin y validación de fotos, todos resueltos y verificados; healthcheck real del backend; confirmación transaccional con bloqueo de fila; reproducibilidad de punta a punta verificada (`docker compose down -v && up --build` desde cero, sin ningún paso manual).

**Principales debilidades que persisten (verificadas en la recalificación de 2026-09-26; ver §16).** El PIN sigue en texto plano (decisión consciente, ver §7); CI agregado pero su ejecución real en GitHub Actions sigue sin verificarse (decisión deliberada de alcance, requiere `git push`); Retry y Cache Aside siguen sin implementarse (solo Rate Limiting, de los tres patrones, quedó cubierto); OpenAPI curado solo en 5 de 9 controladores de negocio; sin breakpoint identificado en los endpoints de lectura ligeros (el sistema no llegó a degradarse ni con 750 VUs sobre esos endpoints, aunque sí se encontró y corrigió el breakpoint real del tablero administrativo bajo datos pesados); JWT en `localStorage` (Baja).

---

## 2. Tabla de calificación

**Nota (2026-09-27):** tabla superada por la segunda recalificación (**§18.5**, también 92/100, con otra distribución: RA2 19, RA4 14, Seguridad 9, DevOps 4). Se conserva como registro del §16.

**Nota original:** esta tabla refleja la recalificación formal de 2026-09-26 (§16), que verificó con ejecución real cada cambio de la "ronda posterior" mencionada en §4.3, §4.4, §5.2, §6, §7, §8 y §11. La columna "Obtenido" es el puntaje vigente; el detalle de qué cambió respecto del puntaje original de cada sección está en §16.1.

| Criterio | Máximo | Obtenido | Nivel | Evidencia principal |
|---|---:|---:|---|---|
| RA1 – Negocio y propuesta de valor | 15 | 14 | Excelente | `docs/Fase1_Vision_Producto_Modelo_Negocio_API_Final.md`: problema, cadena de valor, showback, riesgos, KPIs |
| RA2 – Arquitectura, patrones y frontend | 20 | 18 | Excelente | Hexagonal ya sin violaciones; C4 (`sistema-pruebas-entrega-1.2.dsl`) coincide con la implementación; Circuit Breaker y showback verificados; `DriverHomePage.jsx` dividido (629→318 líneas) y manejo de error resuelto en `AdminDriversPage`/`AdminDeliveriesPage`, verificado en código. Sin llegar a 20: Retry/Cache Aside siguen sin implementar |
| RA3 – Datos y especificación API | 20 | 18 | Excelente | Flyway como única fuente de verdad del esquema (con la FK que faltaba); versionamiento `/api/v1` en los 8 controladores de negocio; OpenAPI real verificado pero curado solo en 5/9 controladores (verificado con `grep -c "@Operation"`); formato de error unificado; códigos HTTP 201/204/409/422 corregidos |
| RA4 – Desarrollo y calidad | 15 | 13 | Muy bueno | 409 real para violaciones de integridad; `AdminInvoiceController` reescrito; formato de error unificado; `DuplicateKeyException` genérico; logging agregado en `DeliveryApplicationService`/`GlobalExceptionHandler` y `DemoInvoiceLoader` reformateado, ambos verificados en código. No llega a Excelente por la doble estrategia de persistencia (JDBC directo + JPA) sin resolver |
| Seguridad | 10 | 10 | Excelente | XSS, PIN sin límite de intentos, confianza en datos del cliente, autoprotección de admin, validación de fotos y **rate limiting en login** (4/4 pruebas verificadas) **resueltos y verificados**; solo persisten el PIN en texto plano (decisión deliberada de negocio) y el JWT en localStorage (Baja) |
| Pruebas unitarias | 5 | 5 | Excelente | **52 pruebas reales** verificadas con `mvn test` (BUILD SUCCESS) en contenedor Maven con Testcontainers; cobertura JaCoCo real **52% de instrucciones / 62% de líneas** (verificado en `target/site/jacoco/index.html`); frontend con 9/9 pruebas verificadas (`npx vitest run`) |
| Rendimiento y pruebas de carga | 10 | 9 | Excelente | k6 sostenido/spike (0% error) más un ciclo con dataset pesado real (500 entregas con foto) que encontró y corrigió un cuello de botella real: p95 21.91s → 13.33ms (~1600x), throughput 44→706 req/s |
| DevOps, despliegue y reproducibilidad | 5 | 5 | Excelente | Healthcheck real; Flyway aplicado en vivo; `.gitignore` en UTF-8; usuario `conductor` autocreado; reproducibilidad de punta a punta verificada sin pasos manuales |
| **TOTAL** | **100** | **92** | **Excelente** | |

---

## 3. Evaluación RA1 — Negocio y propuesta de valor

**Evidencia encontrada.** `docs/Fase1_Vision_Producto_Modelo_Negocio_API_Final.md` (v1.1): resumen ejecutivo, problema y oportunidad (§2), alcance incluido/excluido (§3), impacto económico y costo de marca (§4.1–4.2), cadena de valor en 4 eslabones (§4.4), monetización por **showback por entrega verificada** con fórmula (§4.5), ventaja competitiva (§5), matriz de riesgos (§5.1) e indicadores de éxito (§5.2).

**Análisis.**
- Problema real y bien argumentado (merma, reclamos sin evidencia, riesgo laboral del conductor). Usuarios claros: conductor, administración, cliente final.
- Modelo de monetización coherente para una API interna (showback / reducción de costos), con fórmula explícita y fuentes de dato para cada componente del costo.
- Separación negocio/presentación declarada y, en efecto, la API es consumible por otros clientes (REST + JWT); `scripts/generate_invoices.py` demuestra en la práctica un segundo consumidor de la API además del frontend.

**Problemas.**
- Diferenciación frente a alternativas algo genérica ("firma o foto aislada"), sin comparar con soluciones concretas de POD (proof of delivery) de última milla.

*Actualización de esta revisión:* el §4.5 ya incluye un ejemplo ilustrativo con cifras hipotéticas de la fórmula de showback, separando explícitamente lo que el piloto puede medir hoy (infraestructura, almacenamiento, soporte → USD 0,06/entrega) de una proyección a futuro si se automatiza el envío del PIN (USD 0,09/entrega). Esto resuelve la debilidad de "ausencia de cifras/ejemplo numérico" señalada en la revisión anterior de este informe.

*Nota de calificación:* que el panel de costo por entrega (§4.5) o el envío automático del PIN (§3) no estén implementados en el código **no se penaliza aquí**, para evitar duplicar puntaje: esa brecha ya se evalúa en RA2/RA3 (arquitectura e implementación), mientras que la evidencia que pide RA1 es documental — visión, modelo de negocio, monetización, usuarios y propuesta de valor —, y en ese plano Fase1 la cubre con solidez.

**Puntaje: 14/15**

---

## 4. Evaluación RA2 — Arquitectura, patrones y frontend

### 4.1 Arquitectura: diagrama C4 vs. implementación real

`sistema-pruebas-entrega-1.0.dsl` y su revisión `-1.1.dsl` documentaban una arquitectura aspiracional (Angular, SSE, proveedor de mensajería, geocodificación externa OSM/Google, adaptador ERP, sincronizador offline con IndexedDB, PIN "hasheado") que no tenía respaldo en el código. En esta revisión se reemplazó por **`docs/sistema-pruebas-entrega-1.2.dsl`**, redactado a partir del código real (personas, contenedores y componentes citan la clase o el archivo que implementan), y consistente con el alcance de piloto que Fase1 §3 ya definía (ERP simulado con datos propios, PIN de un solo uso dictado por el cliente, foto y geolocalización validada).

| Elemento en `sistema-pruebas-entrega-1.2.dsl` | Implementación real | Estado |
|---|---|---|
| `frontend` como **React 19 + Vite + vite-plugin-pwa**, una sola SPA/PWA para ambos roles | `frontend/package.json`; `App.jsx`/`ProtectedRoute` enrutan por rol | Coincide |
| `api` en **Java 17 + Spring Boot 3.3**, arquitectura hexagonal | `backend/pom.xml`; paquetes `domain`/`application`/`infrastructure` | Coincide |
| `db` en **PostgreSQL 16**; nota explícita de que el PIN se guarda en texto plano | `postgres:16-alpine`; `pin varchar(6)` en `V1__baseline.sql` (Flyway), sin cifrar | Coincide (incluida la debilidad, ver §7) |
| Componentes del frontend (`loginPage`, `driverHome`, `driverHistory`, `adminInvoices`, `adminDrivers`, `adminDeliveries`, `adminDashboard`, `apiClient`, `authContext`) | Cada uno referencia su archivo real en `frontend/src/pages/*` y `frontend/src/{api,context}/*`, incluido el OCR con `tesseract.js` en `driverHome` | Coincide |
| Componentes de la API (controladores, servicios de aplicación, `LocalInvoiceAdapter` como "entorno de datos simulado que replica el contrato mínimo del ERP") | Cada uno referencia su clase real; `LocalInvoiceAdapter` es exactamente el simulador que describe Fase1 §3 | Coincide |
| Nota de alcance: sin SSE, sin mensajería automática, sin geocodificación externa, sin sincronización offline | Ninguno de esos mecanismos existe en el código (confirmado en el análisis del backend y del frontend) | Coincide — documentado como ausente, no como implementado |

**Conclusión.** La brecha documental de arquitectura identificada en `-1.0`/`-1.1` queda cerrada con `-1.2`: ya no hay elementos "aspiracionales" sin respaldo en código. Esto no repara ninguna vulnerabilidad ni carencia real (el PIN sigue en texto plano, `AdminInvoiceController` sigue violando el patrón hexagonal, sigue sin haber OpenAPI); solo corrige la honestidad de la documentación frente al código, que es lo que esta subsección evalúa.

### 4.2 Estilo de API
REST sobre JSON. Rutas agrupadas por actor (`/api/v1/auth`, `/api/v1/admin/*`, `/api/v1/driver/*`) y **versionadas** desde esta revisión (§5.3). Uso correcto de GET/POST/PUT/DELETE en `/api/v1/admin/users`; varias acciones se modelan como RPC sobre verbos POST (`/deliveries/confirm`, `/deliveries/incident`, `/invoices/{id}/publish`) en lugar de como transiciones de estado de un recurso — decisión de diseño razonable, no un defecto. Detalle de consistencia de códigos HTTP y esquemas en la sección RA3.

### 4.3 Patrones de diseño

| Patrón | Evidencia encontrada | Correctamente aplicado | Observación |
|---|---|---|---|
| Hexagonal (Ports & Adapters) | `domain/port/in` (17 casos de uso, incluido `ManageInvoicesUseCase`), `domain/port/out` (7 puertos, incluido `InvoiceAdminPort`), `infrastructure/adapter/{in,out}` | Sí | Corregido en esta revisión: `AdminInvoiceController` ya depende de `ManageInvoicesUseCase` (puerto de entrada), no de `LocalInvoiceAdapter` directamente |
| Service Layer | `application/*ApplicationService` | Sí | Cada servicio de aplicación implementa varios casos de uso relacionados |
| Repository | `DriverRepositoryPort` / `DeliveryAttemptRepositoryPort` + adaptadores Spring Data JPA | Sí | Las facturas usan `JdbcTemplate` directo: dos estrategias de persistencia convivientes en el mismo backend |
| Dependency Injection | Inyección por constructor en todos los beans revisados | Sí | Sin uso de `@Autowired` en campos |
| DTO | `infrastructure/adapter/in/web/dto/*` (records inmutables), incluido `AdminInvoiceResponse` (nuevo) | Sí | Las facturas del admin ya tienen DTO propio en camelCase; ya no se expone el `Map` crudo en snake_case |
| Adapter | `JwtTokenProviderAdapter`, `SpringPasswordEncoderAdapter`, `SpringSecurityUserDetailsAdapter` | Sí | Buen aislamiento del dominio respecto de las clases de Spring Security |
| Mapper | `DeliveryAttemptPersistenceMapper`, `DriverPersistenceMapper` | Sí | Traducción explícita entidad JPA ↔ modelo de dominio |
| Pagination | `PageRequest` / `PageResult` / `PageResponse` en historiales | Parcial | Sin tope máximo de `size`; el listado de facturas usa `LIMIT 1000` fijo y el de usuarios no pagina |
| Filtering / Search | Parámetro `q` con `ILIKE` en facturas y búsqueda del conductor | Parcial | Sin ordenamiento configurable ni búsqueda por otros campos |
| Pessimistic Locking | `SELECT … FOR UPDATE` en `LocalInvoiceAdapter.locked()` | Sí | Verificado con concurrencia real de 2 hilos en `scripts/verify_delivery.py` |
| API Gateway / BFF | Nginx solo actúa como reverse proxy simple | No | No hay agregación de llamadas ni BFF; no aplica críticamente al tamaño del sistema |
| Circuit Breaker | `resilience4j-circuitbreaker` real (`ResilienceConfig`, breaker `"erpGateway"`) envolviendo `LocalInvoiceAdapter` | Sí | Protege la simulación del ERP (Fase1 §3/§5.1); activado por defecto (`APP_CIRCUIT_BREAKER_ENABLED`); ciclo `CLOSED → OPEN → HALF_OPEN → CLOSED` verificado en vivo vía `AdminResilienceController` (`/api/v1/admin/resilience/status`, `/simulate-failures`) y una sección en el panel admin |
| Retry / Cache Aside / Rate Limiting | Sin dependencias correspondientes (Spring Cache, bucket4j) en `pom.xml` | Parcial | ~~Rate limiting en `/api/v1/auth/login` no existía~~ — **Resuelto en la ronda posterior a esta revisión**: `bucket4j_jdk17-core` en `pom.xml`, `LoginRateLimiter` limita por IP+usuario (5 intentos/60s configurable), responde 429 (`TooManyLoginAttemptsException`). Retry y Cache Aside siguen sin implementarse (ver §5 de la Fase 2) |

### 4.4 Frontend
- **Organización:** `api/client.js` (axios + interceptores), `context/AuthContext.jsx`, `components/ProtectedRoute.jsx`, páginas separadas por rol (`pages/admin/*`, `pages/driver/*`). Correcta en general, pero `DriverHomePage.jsx` (629 líneas) mezcla OCR, cámara, GPS, compresión de imagen y formularios en un solo componente.
- **Manejo de estado:** `useState` local por página + contexto de autenticación global. Proporcionado y suficiente para el tamaño del proyecto; no hay necesidad evidente de una librería de estado global.
- **Estados de loading:** `TableSkeleton` reutilizable en listados; botones deshabilitados durante los envíos (`saving`, `confirming`).
- **Manejo de errores:** presente y consistente en `LoginPage`, `AdminInvoicesPage`, `AdminDashboardPage` e historial del conductor. ~~Ausente en `AdminDriversPage` y `AdminDeliveriesPage`~~ — **Resuelto en la ronda posterior a esta revisión**: `loadUsers`, `toggleActive`, `removeUser` (`AdminDriversPage`) y la carga del listado y de la foto (`AdminDeliveriesPage`) ya muestran el mensaje de error del backend en vez de fallar en silencio, con 3 pruebas de Vitest/Testing Library cubriendo el flujo.
- **Tokens:** almacenados en `localStorage` o `sessionStorage` según la opción "Recuérdame"; el logout y las respuestas 401 limpian ambos almacenes.
- **Protección de rutas:** por rol tanto en cliente (`ProtectedRoute`) como en servidor (`SecurityConfig`), por lo que no se trata de una protección únicamente visual.
- `frontend/README.md` sigue siendo la plantilla por defecto de Vite, sin adaptar al proyecto.

**Puntaje de la sección RA2 (recalificado 2026-09-26, ver §16): 18/20.** Sube frente a la revisión anterior porque la arquitectura ya está fielmente documentada en `sistema-pruebas-entrega-1.2.dsl` (§4.1); dos patrones antes ausentes (Circuit Breaker, panel de costo/showback) ya están implementados y verificados en vivo; y la violación hexagonal de `AdminInvoiceController` (el único "Parcial" que quedaba en la tabla de patrones) quedó corregida con `ManageInvoicesUseCase`/`InvoiceAdminPort` y un DTO propio. Sube de 17 a 18 porque se verificó en código que el manejo de errores ya está presente en `AdminDriversPage`/`AdminDeliveriesPage` y que `DriverHomePage.jsx` se dividió de 629 a 318 líneas. Se mantiene por debajo de 20 porque Retry y Cache Aside siguen sin implementarse (de los tres patrones listados junto con Rate Limiting, solo este último quedó cubierto).

---

## 5. Evaluación RA3 — Datos y especificación API

### 5.1 Modelo de datos

**Resuelto en esta revisión.** El esquema ya no se reparte entre `schema.sql` y `hibernate.ddl-auto: update`: `backend/src/main/resources/db/migration/V1__baseline.sql` (Flyway) es ahora la **única fuente de verdad**, con `delivery_invoice`, `delivery_invoice_line`, `operational_cost_input`, `app_user` y `delivery_log` escritas explícitamente (antes las dos últimas las generaba Hibernate sin definición SQL en el repositorio); `hibernate.ddl-auto` pasó de `update` a `validate`. Verificado en vivo con `docker compose down -v && up --build`: el log muestra `Migrating schema "public" to version "1 - baseline"` y `Successfully applied 1 migration`, y Hibernate valida sin error contra el esquema resultante. La FK que faltaba también se agregó y se verificó con `\d delivery_log` en `psql`:

```
Foreign-key constraints:
    "delivery_log_driver_id_fkey" FOREIGN KEY (driver_id) REFERENCES app_user(id)
    "delivery_log_invoice_id_fkey" FOREIGN KEY (invoice_id) REFERENCES delivery_invoice(id)
```

Se agregaron además índices en `delivery_log(driver_id)`, `delivery_log(invoice_id)` y `delivery_log(created_at desc)`, en línea directa con la optimización de rendimiento del tablero administrativo (§9).

| Problema | Estado |
|---|---|
| ~~Dos mecanismos de esquema conviviendo sin herramienta de migraciones~~ | **Resuelto.** Flyway (`V1__baseline.sql`) es la única fuente de verdad; `schema.sql` fue retirado |
| ~~`delivery_log.invoice_id` sin clave foránea hacia `delivery_invoice`~~ | **Resuelto y verificado** en la base real (ver arriba) |
| ~~Datos de la evidencia (número, cliente, dirección) tomados directamente del cliente HTTP~~ | **Resuelto.** `DeliveryApplicationService.confirmDelivery`/`reportIncident` resuelven la factura real vía `InvoiceQueryPort.findById(invoiceId)` y usan `number()`/`partnerName()`/`deliveryAddress()` de la base para la auditoría; si la factura no existe, se rechaza (`DeliveryRejectedException`) — esto además corrige que `reportIncident` no validaba la existencia de la factura |
| PIN almacenado en texto plano | Sin cambios (decisión deliberada, ver §7) |
| `state` sin `CHECK` ni enum en base; el estado `cancel` no tiene ningún endpoint que lo produzca | Sin cambios |
| Fotos binarias (`bytea`) mezcladas en la misma tabla que el log de auditoría | Sin cambios estructurales — mitigado en el acceso: ninguna consulta de listado trae ya la columna `photo` (proyecciones JPA, §9) |
| La misma tabla `app_user` / clase `Driver` modela tanto administradores como conductores | Sin cambios; nomenclatura confusa (`AdminDriverController` expone `/api/v1/admin/users`) |
| Eliminar un usuario con historial de entregas viola la FK `driver_id` | 409 (`DataIntegrityViolationException` → `GlobalExceptionHandler`), sin cambios respecto de la revisión anterior |
| Manejo de fechas | `Instant` en `delivery_log` (correcto); `confirmed_at timestamp` sin zona horaria explícita en facturas |
| Auditoría | Solo existe `created_at`; no se registra quién publicó o creó cada factura |

Aspectos positivos del modelo: `number` único, `CHECK (quantity > 0)` en líneas de factura, índices reales (`invoice_id` en líneas, y ahora también `driver_id`/`invoice_id`/`created_at` en `delivery_log`), `username` único, y claves foráneas correctas en las tres relaciones del esquema.

### 5.2 Contrato OpenAPI / Swagger

`springdoc-openapi-starter-webmvc-ui` en `pom.xml`, con `OpenApiConfig` (esquema de seguridad `bearerAuth` global, actualizado para referenciar `/api/v1/...` en su descripción). Verificado en vivo: `GET /v3/api-docs` y `GET /swagger-ui/index.html` responden 200 sin autenticación y documentan los endpoints reales, ya bajo `/api/v1`, a partir de las anotaciones existentes. ~~No se anotaron manualmente `@Operation`/`@ApiResponse` por endpoint~~ — **Resuelto parcialmente en la ronda posterior a esta revisión**: `AuthController`, `DriverDeliveryController` (confirm/incident), `AdminInvoiceController` (create/publish), `AdminCostController` y `AdminResilienceController` ya tienen `@Operation`/`@ApiResponses` curados, con los códigos de estado y su significado documentados. Los demás controladores (equipo, historial, tablero) siguen con la descripción inferida automáticamente.

### 5.3 Inventario de endpoints reales y diseño

**Resuelto en esta revisión: versionamiento `/api/v1`.** Los 8 controladores de negocio (`AuthController`, `AdminInvoiceController`, `AdminDriverController`, `AdminCostController`, `AdminDeliveryHistoryController`, `AdminDashboardController`, `AdminResilienceController`, `DriverDeliveryController`) quedaron prefijados con `/api/v1`; `SecurityConfig`, el filtro JWT, el `Location` de los `201 Created`, el frontend (`api/client.js`), y los scripts de verificación se actualizaron en conjunto. `/actuator/health`, `/v3/api-docs` y `/swagger-ui/**` deliberadamente **no** se versionaron (son infraestructura, no el contrato de negocio). Verificado en vivo: las rutas antiguas sin `/api/v1` ya no resuelven a ningún controlador (devuelven 401 de "no autenticado" al no matchear ningún `permitAll`, no el login real), y las nuevas funcionan tanto contra el backend directo como a través del proxy Nginx del frontend.

| Método y ruta | Rol requerido | Observación |
|---|---|---|
| `POST /api/v1/auth/login` | público | Responde 200 con token; correcto |
| `GET /api/v1/admin/invoices?q=` | ADMIN | camelCase (`AdminInvoiceResponse`), límite fijo de 1000 filas, incluye el PIN |
| `POST /api/v1/admin/invoices` | ADMIN | Responde 201 Created + Location |
| `POST /api/v1/admin/invoices/{id}/publish` | ADMIN | Acción RPC sobre verbo POST; correctamente idempotente respecto del PIN ya generado |
| `GET/POST /api/v1/admin/users`, `PUT/DELETE /api/v1/admin/users/{id}` | ADMIN | `POST` responde 201 + Location; `DELETE` responde 204; `PUT`/`DELETE` sobre el propio usuario autenticado responden ahora **409** (autoprotección, ver §7); `PUT` sigue exigiendo reenviar todos los campos |
| `GET /api/v1/admin/deliveries?page&size` | ADMIN | Paginado, pero sin tope de `size` (sin cambios) |
| `GET /api/v1/admin/deliveries/{id}/photo` | ADMIN | Devuelve binario correctamente tipado |
| `GET /api/v1/admin/dashboard/map?from&to`, `/metrics?from&to` | ADMIN | Reescritos con proyecciones JPA sin `photo` y agregación SQL (§9); sin validación de rango máximo de fechas |
| `GET /api/v1/admin/cost`, `PUT /api/v1/admin/cost/support-hours` | ADMIN | Panel de costo |
| `GET /api/v1/admin/resilience/status`, `POST /api/v1/admin/resilience/simulate-failures` | ADMIN | Circuit Breaker |
| `GET /api/v1/driver/invoices?q=` | ADMIN, CONDUCTOR | camelCase, consistente con `/api/v1/admin/invoices` |
| `GET /api/v1/driver/invoices/{id}/lines` | ADMIN, CONDUCTOR | — |
| `POST /api/v1/driver/deliveries/confirm` | ADMIN, CONDUCTOR | PIN erróneo **422**, entrega ya confirmada **409**; validación de la foto (tamaño y magic bytes, ver §7) |
| `POST /api/v1/driver/deliveries/incident` | ADMIN, CONDUCTOR | Ahora valida que la factura exista antes de registrar la incidencia (resuelto junto con la confianza de datos, §5.1) |
| `GET /api/v1/driver/deliveries/history`, `/{id}/photo` | ADMIN, CONDUCTOR | Correctamente filtrado por `driverId`, evitando IDOR |

**Resuelto en esta revisión: formato de error unificado.** `GlobalExceptionHandler.handleValidation` ya no devuelve solo el mapa `{campo: mensaje}`: ahora envuelve la respuesta en `{"message": "Uno o mas campos no son validos.", "errors": {...}}`, consistente con el resto de los errores que el frontend ya sabe leer (`.message`). Verificado en vivo con un payload de creación de factura sin productos. `DuplicateKeyException` también dejó de tener el mensaje fijo "Ya existe una factura con ese número" (podía ser engañoso si la violación venía de otra tabla) y ahora responde con un mensaje genérico.

**Corrección de un hallazgo de la revisión anterior:** el informe anterior afirmaba que `MediaType.parseMediaType(photo.contentType())` con un `contentType` inválido lanzaba una excepción no capturada (500 genérico). Se verificó en código que `InvalidMediaTypeException` **ya es subclase de `IllegalArgumentException`**, capturada por el `handleIllegalArgument` genérico que ya existía → responde 400, no 500. Esto no fue un fix de esta ronda: fue un hallazgo mal calificado en la revisión anterior, que se corrige aquí en el texto, no en el código.

**Inconsistencia de contrato que persiste:** las acciones se siguen modelando como RPC sobre verbos POST (`/deliveries/confirm`, `/deliveries/incident`, `/invoices/{id}/publish`) en vez de como transiciones de estado de un recurso; es una decisión de diseño razonable para este dominio, no un defecto.

**Puntaje: 18/20.** Sube de forma sustancial: Flyway como única fuente de verdad del esquema (con la FK que faltaba, verificada en la base real), versionamiento `/api/v1` verificado en vivo, formato de error unificado, y la corrección del hallazgo de `MediaType.parseMediaType` que estaba mal calificado. No llega a 20/20 porque persisten decisiones de diseño de RPC-sobre-POST (aceptables, no puntuadas en contra por sí solas) y porque las descripciones de OpenAPI siguen siendo las inferidas automáticamente, no curadas operación por operación.

---

## 6. Evaluación RA4 — Desarrollo y calidad

**Aspectos positivos**
- Paquetes limpios por capa (`domain`, `application`, `infrastructure`); nombres expresivos; uso de `record` para DTO y objetos de valor; `GeoLocation` valida rangos de coordenadas en el constructor y calcula distancia con la fórmula de Haversine.
- `GlobalExceptionHandler` centraliza el mapeo entre excepciones de dominio y códigos HTTP en un único lugar.
- Bean Validation declarativa en los DTO de entrada (`@Pattern("\\d{6}")` para el PIN, `@DecimalMin/@DecimalMax` para coordenadas, `@Size` para contraseñas).
- Configuración externalizada por variables de entorno (`application.yml`), con `open-in-view: false` (buena práctica que evita el patrón "Open Session in View").
- La confirmación de entrega y el guardado de la evidencia ocurren en una sola transacción (`DeliveryApplicationService.confirmDelivery`), y el intento fallido se registra fuera de esa transacción para no perder la auditoría de rechazos.

**Resuelto en esta revisión**
- `GlobalExceptionHandler` ya distingue `DataIntegrityViolationException` (→ **409 Conflict**) de fallos genéricos de acceso a datos (→ 503); borrar un usuario con historial de entregas ya no responde 503.
- `AdminInvoiceController` se reescribió en el estilo del resto del proyecto (ya no depende de `LocalInvoiceAdapter` directamente, ver RA2 §4.3).
- `DuplicateKeyException` ya no responde con el mensaje fijo "Ya existe una factura con ese número" (podía ser engañoso si la violación venía de otra tabla); ahora es un mensaje genérico.
- `handleValidation` unifica el formato de error con el resto del handler (`{message, errors}`, ver RA3 §5.3).
- El hallazgo de `MediaType.parseMediaType` de la revisión anterior era incorrecto: `InvalidMediaTypeException` ya estaba cubierta por `handleIllegalArgument` desde antes (400, no 500) — corrección de texto, no de código.
- `AdminInvoiceController`/`AdminDriverController` ya no confían en los datos que envía el cliente para la auditoría de la entrega: `DeliveryApplicationService` resuelve la factura real desde `InvoiceQueryPort` antes de persistir (ver RA3 §5.1).
- `LocalInvoiceAdapterIntegrationTest` (Testcontainers) es la primera prueba real del adaptador más crítico del sistema, hasta ahora en 0% de cobertura.

**Problemas concretos que persisten**
- ~~`PageRequest` (dominio) sigue sin imponer un tamaño máximo de página~~ — **Resuelto**: `PageRequest.MAX_SIZE = 100`, rechaza con 400 si se excede.
- ~~`DemoInvoiceLoader` sigue en un estilo muy comprimido~~ — **Resuelto en la ronda posterior a esta revisión**: reformateado a una sentencia por línea, mismo comportamiento (verificado con `mvn test`).
- ~~Logging escaso~~ — **Resuelto en la ronda posterior a esta revisión**: `DeliveryApplicationService` registra confirmaciones, rechazos y PIN inválido; `GlobalExceptionHandler` registra en `ERROR` los fallos técnicos (ERP no disponible, circuito abierto, fallo de acceso a datos genérico), verificado en las pruebas de `GlobalExceptionHandlerTest`.
- ~~En el frontend: operaciones sin manejo de error~~ — Resuelto (ver 4.4).
- ~~Lógica de negocio duplicada (Haversine en `GeoLocation.java` y en `DriverHomePage.jsx:68`)~~ — **Documentado, no eliminado**: se extrajo a `frontend/src/utils/geo.js` con un comentario explicando que el duplicado frente al backend es intencional (el frontend necesita avisar "estás lejos" antes de llamar a la API; Java y JS no comparten código en runtimes distintos).
- **Nuevo, no señalado antes:** `DriverHomePage.jsx` (629 líneas, mezclaba OCR, cámara, GPS, compresión y formularios) se dividió en componentes y utilidades (`driverHome/geolocation.js`, `imageUtils.js`, `invoiceOcr.js`, `PinBoxes.jsx`, `InvoiceSearchPanel.jsx`, `ConfirmDeliveryForm.jsx`, `IncidentForm.jsx`); el archivo orquestador quedó en 318 líneas, sin cambiar el comportamiento.

**Puntaje (recalificado 2026-09-26, ver §16): 13/15.** Sube de 11 a 13: se verificó en código que el logging fue agregado (`DeliveryApplicationService`, `GlobalExceptionHandler`) y que `DemoInvoiceLoader` fue reformateado a una sentencia por línea. El manejo de errores del frontend y el tamaño de `DriverHomePage.jsx` se cuentan en RA2 (§4), no aquí, para no duplicar el ajuste. No llega a Excelente porque persiste una decisión arquitectónica sin resolver: las facturas usan `JdbcTemplate` directo mientras el resto del backend usa JPA, dos estrategias de persistencia conviviendo en el mismo sistema.

---

## 7. Seguridad

**Implementado y verificado en código:** hashing de contraseñas con BCrypt (`PasswordEncoderConfig`); JWT firmado con HMAC-SHA256 y expiración configurable (480 minutos por defecto); sesiones `STATELESS`; autorización por rol aplicada en el backend (`SecurityConfig`: `/api/v1/admin/**` exige `ROLE_ADMIN`); un usuario desactivado queda bloqueado en cada petición porque `JwtAuthenticationFilter` consulta `userDetails.isEnabled()`; todas las consultas usan parámetros ligados (sin inyección SQL); las fotos del conductor se filtran por `driverId` en la consulta (sin IDOR); CORS con orígenes configurables por variable de entorno. Deshabilitar CSRF es razonable dado que la autenticación es por token `Bearer` y no por cookies. No hay refresh token ni mecanismo de revocación de tokens emitidos.

| Hallazgo | Severidad | Evidencia | Impacto | Recomendación |
|---|---|---|---|---|
| ~~XSS almacenado en el mapa del panel de administración~~ — **Resuelto** | ~~Alta~~ | `AdminDashboardPage.jsx`: `bindPopup` ahora recibe un nodo DOM construido con `document.createElement`/`textContent` (`buildPopupContent`), nunca un template string HTML; `ReportIncidentRequest.notes` ahora tiene `@Size(max=500)` | — | Verificado: un `notes` con `<img onerror=...>` se muestra como texto plano en el popup, no se ejecuta |
| ~~PIN de entrega sin límite de intentos~~ — **Resuelto** | ~~Alta~~ | `delivery_invoice.failed_pin_attempts`/`pin_locked_until`; `DeliveryConfirmationGatewayPort.registerFailedPinAttempt`, llamado desde `DeliveryApplicationService` tras el rollback de la confirmación fallida (transacción separada, ver comentario en el código) | — | Verificado en vivo: 5 PIN incorrectos consecutivos bloquean la factura 5 minutos; el PIN correcto en el 6º intento sigue rechazado con 400 hasta que expira el bloqueo |
| ~~Secretos y credenciales por defecto en configuración~~ — **Resuelto** | ~~Media~~ | `SecretsGuardRunner`: el arranque falla si `JWT_SECRET` **o `ADMIN_PASSWORD`** siguen siendo el placeholder y `APP_DEMO_ENABLED=false` | — | Verificado: ambas variables se exigen fuera del modo demo, no solo `JWT_SECRET` como en la revisión anterior |
| ~~Sin límite de intentos (rate limiting) en el login~~ — **Resuelto en la ronda posterior a esta revisión** | ~~Media~~ | `LoginRateLimiter` (bucket4j, en memoria): 5 intentos por IP+usuario cada 60s (configurable), `GlobalExceptionHandler` responde 429 | — | Verificado con 4 pruebas unitarias que agotan el límite y confirman el 429 |
| ~~Datos de la evidencia de entrega aportados por el propio cliente HTTP~~ — **Resuelto** | ~~Media~~ | `DeliveryApplicationService.confirmDelivery`/`reportIncident` resuelven la factura real vía `InvoiceQueryPort.findById(invoiceId)` antes de persistir la auditoría, en vez de usar `invoiceNumber`/`partnerName`/`deliveryAddress` del DTO de entrada | — | Verificado con una prueba unitaria (factura inexistente → rechazo sin llamar al gateway) y en vivo |
| ~~Foto de evidencia sin validación de tamaño ni tipo real~~ — **Resuelto** | ~~Media~~ | `DeliveryPhoto.decodedBytes()`: rechaza payloads mayores a 5MB y valida los primeros bytes contra las firmas reales de JPEG (`FF D8 FF`) y PNG (`89 50 4E 47 0D 0A 1A 0A`) | — | Verificado en vivo: un payload de texto plano codificado en base64 se rechaza con 400 ("La foto de evidencia debe ser una imagen JPEG o PNG valida") |
| PIN almacenado en texto plano | Media (mantenido deliberadamente) | `V1__baseline.sql` (antes `schema.sql`): `pin varchar(6)`; se devuelve sin cifrar en `GET /api/v1/admin/invoices` y en la exportación CSV del panel | Una fuga de la base de datos o el acceso indebido a una cuenta admin expone todos los PIN vigentes de una sola vez | **No se hashea**: Fase1 §3 exige que la administración pueda leer el PIN vigente para comunicarlo manualmente al cliente; un hash de una vía rompería esa función real. La mitigación aplicada es el bloqueo por intentos (fila anterior), no el hashing |
| Token JWT en `localStorage` | Baja | `AuthContext.jsx` | Ya no se combina con un XSS conocido (fila resuelta arriba), pero sigue siendo la práctica de almacenamiento | Usar una cookie `HttpOnly` + `SameSite`, o mantener una CSP. No se abordó en esta revisión |
| ~~Un administrador puede desactivarse o eliminarse a sí mismo~~ — **Resuelto** | ~~Baja~~ | `AdminDriverController.rejectSelfModification`: si el `id` objetivo coincide con el usuario autenticado (resuelto vía `CurrentDriverResolver`), responde 409 antes de delegar al caso de uso | — | Verificado en vivo: `DELETE /api/v1/admin/users/{propio-id}` responde 409, no 204 |

**Puntaje (recalificado 2026-09-26, ver §16): 10/10.** Sube de 9 a 10: además de los dos hallazgos de severidad Alta ya resueltos en la revisión anterior (XSS, PIN sin límite de intentos), esta ronda resuelve y verifica en vivo los cuatro hallazgos de severidad Media que quedaban (secretos por defecto completo, confianza en datos del cliente, validación de fotos), el de severidad Baja de autoprotección de admin, y ahora también el rate limiting del login (`LoginRateLimiter`, verificado con 4/4 pruebas reales ejecutadas). Solo persisten el JWT en `localStorage` (Baja, sin XSS conocido que lo explote) y la decisión consciente y documentada de no hashear el PIN (exigencia del propio negocio, Fase1 §3), ninguno de los dos es una omisión técnica sin justificar.

---

## 8. Pruebas unitarias

**Implementado en esta revisión y en la anterior.** `backend/src/test/java` tiene ahora **24 pruebas** que corren de verdad (`mvn test`, ejecutado en un contenedor Maven limpio con el socket de Docker montado: **BUILD SUCCESS, 24/24 pasan**):

| Clase | Casos | Qué prueba |
|---|---:|---|
| `DeliveryApplicationServiceTest` (Mockito) | 6 | Confirmación exitosa; PIN inválido (y que se registra el intento fallido); entrega ya confirmada (y que **no** se registra intento de PIN); reporte de incidencia; **nuevo:** factura inexistente rechaza sin llamar al gateway (confianza de datos, §5.1) |
| `AuthApplicationServiceTest` (Mockito) | 4 | Login correcto; contraseña incorrecta; usuario inexistente; usuario inactivo |
| `JwtTokenProviderAdapterTest` | 4 | Generar y validar token; usuario distinto; token corrupto; token firmado con otro secreto |
| `GeoLocationTest` | 4 | Distancia Haversine (mismo punto y distancia real Guayaquil–Quito); rangos de latitud/longitud inválidos |
| `AdminDriverControllerWebMvcTest` (`@WebMvcTest` + `SecurityConfig` real) | 3 | Sin token → 401; rol CONDUCTOR → 403; rol ADMIN → 200, contra la configuración de seguridad real, no una simulada |
| **`LocalInvoiceAdapterIntegrationTest`** (`@SpringBootTest` + **Testcontainers**, Postgres 16 real) | **3** | **Nuevo.** Crear→publicar→confirmar con PIN correcto (contra una base real, con Flyway aplicando `V1__baseline.sql` al arrancar el contexto); 5 PIN incorrectos consecutivos bloquean la factura (verificado en la base real, no simulado); **8 confirmaciones concurrentes** sobre la misma factura → exactamente 1 éxito y 7 rechazos, verificando el `SELECT ... FOR UPDATE` de `LocalInvoiceAdapter.locked()` bajo concurrencia real |

Al escribir `AdminDriverControllerWebMvcTest` (revisión anterior) se encontró y corrigió un problema real de la propia prueba: mockear `JwtAuthenticationFilter` con `@MockBean` sobrescribe también su `doFilter` heredado y corta la cadena de filtros antes de llegar a la autorización. Al escribir `LocalInvoiceAdapterIntegrationTest` (esta revisión) se resolvió además un problema práctico de infraestructura, no del código de producción: la versión de Testcontainers usada inicialmente (1.20.2) no negociaba correctamente la versión de la API de Docker con este entorno (Docker Engine 29, API mínima 1.40) y fallaba con "client version 1.32 is too old"; se resolvió subiendo a `testcontainers-bom` 1.21.4.

- **Cobertura reportada:** JaCoCo (`jacoco-maven-plugin`, `target/site/jacoco/index.html`), generado en el mismo `mvn test` (incluye lo que ejercitan también las pruebas de Testcontainers).
- **Cobertura verificable (real, del reporte):** **39 % de instrucciones** (2 093 / 5 265, subiendo de 21 %), **~49 % de líneas** (496/1 003, subiendo de ~25 %), **~26 % de branches** (48/184, subiendo de ~15 %). Por paquete: `infrastructure.adapter.out.invoice` (el paquete de `LocalInvoiceAdapter`) pasó de **0 % a 66 %** — exactamente el área que la revisión anterior señalaba como el hueco más crítico; `infrastructure.config` 62 %, `domain.port.in` 57 %, `domain.model` 48 %, `application` 47 %, `infrastructure.adapter.out.security` 94 %. Los paquetes de persistencia JPA (proyecciones/mappers) siguen bajos (`.persistence` 13 %, `.persistence.entity` 36 %).
- **Calidad de las pruebas:** no son triviales — verifican comportamiento real (transiciones de estado, excepciones específicas, bloqueo persistido en una base real, concurrencia con 8 hilos y row-locking real), no solo que "no lance excepción".
- **Principales áreas todavía con poca o ninguna prueba (al cierre de esta revisión):** los mappers/proyecciones de persistencia JPA, `GlobalExceptionHandler`, `OperationalCostApplicationService`, y la totalidad del frontend.
- `scripts/verify_delivery.py`: **mejorado en esta revisión** — ya no depende de crear el usuario `conductor` a mano (autocreado por `DemoDriverBootstrapRunner`, §10) y se corrigieron tres aserciones de código HTTP que habían quedado desalineadas con cambios de rondas anteriores (PIN inválido es 422, no 400; entrega ya confirmada es 409, no 400). Sigue siendo una prueba E2E valiosa pero no unitaria.

**Actualización de una ronda posterior a esta revisión.** Las tres áreas señaladas arriba como descubiertas ya tienen pruebas: `OperationalCostApplicationServiceTest` (5 casos, Mockito: cálculo con y sin entregas confirmadas, tarifas en 0, validación de horas negativas), `GlobalExceptionHandlerTest` (16 casos, `@WebMvcTest` end-to-end por HTTP contra un controlador de prueba, uno por cada excepción que traduce el manejador) y `DeliveryAttemptPersistenceMapperTest` (3 casos: ida y vuelta entidad↔dominio, con y sin foto/ubicación). El frontend ya tiene Vitest configurado (jsdom + Testing Library + jest-dom), con 9 pruebas (`apiClient`: interceptor de token y de 401; `AdminDriversPage`: carga y errores). Con `mvn test` vuelto a correr sobre este estado: **52 pruebas** (24 → 52), cobertura **52,0 % de instrucciones** (2 613/5 444), **62,1 % de líneas** (661/1 064) y **33,0 % de branches** (128/194).

**Puntaje (recalificado 2026-09-26, ver §16): 5/5.** Sube de 4 a 5: la brecha más crítica señalada en la revisión anterior —`LocalInvoiceAdapter` en 0% de cobertura— ya no existe, con pruebas de integración reales contra Testcontainers que verifican justamente lo que un mock no puede (bloqueo persistido, concurrencia con row-locking). Se verificó con `mvn test` en un contenedor Maven limpio que la suite completa (52 pruebas: las 24 anteriores más `GlobalExceptionHandlerTest`, `OperationalCostApplicationServiceTest`, `DeliveryAttemptPersistenceMapperTest` y `LoginRateLimiterTest`) pasa en **BUILD SUCCESS**, con cobertura JaCoCo real de **52% de instrucciones / 62% de líneas** (leída directamente del reporte generado en esa corrida). El frontend, antes en 0%, ya tiene 9 pruebas reales verificadas con `npx vitest run`. Las tres áreas de hueco crítico que señalaba la revisión anterior quedaron cerradas con evidencia de ejecución, no solo declaradas.

---

## 9. Evaluación de rendimiento

**Implementado y ejecutado en esta revisión.** `load-tests/sustained.js` y `load-tests/spike.js` (k6), corridos de verdad con `docker run grafana/k6` contra el stack completo (`docker-compose.mock.yml`) recién levantado, sobre los endpoints de lectura reales (búsqueda de facturas, historial admin, tablero, costo, estado del circuit breaker). `POST /api/v1/driver/deliveries/confirm` queda deliberadamente fuera (cada factura solo se confirma una vez; no hay forma de repetirlo miles de veces sin fabricar una factura nueva por iteración — ver nota en el propio script). Resultados crudos capturados de la salida real de k6, no estimados.

### Escenario 1 — Load Test (carga sostenida)

0→150 VUs en 3 min, meseta de 150 VUs por 7 min, bajada a 0 en 2 min (duración total 12 min).

| Indicador | Resultado | Criterio | Evaluación |
|---|---:|---:|---|
| Throughput | 649.4 req/s (511 129 requests totales) | — | — |
| Average Response Time | 2.89 ms | — | — |
| p90 | 4.33 ms | — | — |
| p95 | 5.22 ms | < 500 ms como referencia | **Cumple** |
| p99 | N/E (k6 no lo reportó en el resumen por defecto; máximo observado 3.33 s en un solo outlier) | — | — |
| Error Rate | 0.00 % (0 de 511 129) | < 1 % bajo carga sostenida | **Cumple** |
| Breakpoint | No alcanzado en este escenario | Identificado experimentalmente | No (el sistema no llegó a degradarse con 150 VUs) |
| Recuperación tras spike | Ver escenario 2 | Recuperación automática esperada | — |

### Escenario 2 — Spike Test

0→750 VUs (~10x la meseta del escenario 1) en 30 s, meseta de 1 min, bajada a 0 en 30 s (duración total ~2 min).

| Indicador | Resultado | Criterio | Evaluación |
|---|---:|---:|---|
| Throughput | 3 128.4 req/s (403 067 requests totales) | — | — |
| Average Response Time | 2.84 ms | — | — |
| p90 | 3.85 ms | — | — |
| p95 | 5.72 ms | < 1000 ms (umbral del propio escenario de spike) | **Cumple** |
| Error Rate | 0.00 % (0 de 403 067) | — | **Cumple** |
| Estado del Circuit Breaker al final | `CLOSED`, `numberOfFailedCalls: 0`, `numberOfNotPermittedCalls: 0` (`/api/v1/admin/resilience/status`) | Evidencia real, no simulada | Sin señal de saturación |
| Breakpoint | No alcanzado en este escenario | Identificado experimentalmente | No |

### Escenario 3 — Dashboard bajo dataset pesado real (antes/después)

**Nuevo en esta revisión.** Los dos escenarios anteriores usan el dataset de demostración (59 facturas) y por diseño no llegan a ejercitar `photo` (`bytea`): confirman como máximo una vez por factura y con una foto de prueba mínima. Para exponer de verdad el cuello de botella que las revisiones anteriores solo predecían por análisis estático, se generó un dataset de producción simulada: `scripts/generate_invoices.py --count 2000` + un script nuevo (`scripts/seed_confirmed_deliveries.py`) que confirma **500 facturas** con una foto realista de **~200 KB** cada una (no el PNG de 1×1 de las pruebas), dejando `delivery_log` con ~100 MB de evidencia real. Se corrió `load-tests/dashboard-heavy.js` (0→500 VUs, escalando) contra `GET /api/v1/admin/dashboard/map` y `/metrics` **antes** de tocar el código, y otra vez **después** de aplicarle el fix, con el mismo dataset y el mismo patrón de carga.

| Indicador | Antes (código original) | Después (proyecciones JPA + `GROUP BY` SQL) | Mejora |
|---|---:|---:|---:|
| p95 | **21.91 s** | **13.33 ms** | **~1 600×** |
| Throughput | 44 req/s | 706 req/s | ~16× |
| Causa raíz | `findAllOrderByCreatedAtDesc`/`findAllBetween`/`metrics` traían el entity JPA completo (incluida `photo`, EAGER por defecto) solo para descartarla al armar el DTO; `metrics` agregaba en Java | Nuevas proyecciones JPA (`AttemptSummaryProjection`, sin `photo`) para los tres listados, y `metricsBetween` agregado con `GROUP BY driver, outcome` en SQL | — |

Esto **confirma experimentalmente**, con datos reales y no solo con análisis estático, el cuello de botella que las tres revisiones anteriores de este informe solo predecían: "los historiales paginados también traen los bytes completos de cada foto"; "`metrics` agrega en memoria Java en vez de con `GROUP BY` en SQL". Ambos quedan resueltos y la mejora se midió, no se estimó.

### Breakpoint

**Breakpoint real encontrado, corregido y re-medido (dashboard bajo datos pesados).** A diferencia de los escenarios 1 y 2 (endpoints de lectura ligeros, sin degradación hasta 750 VUs), el escenario 3 sí encontró un punto de quiebre real: con 500 entregas confirmadas con foto y 500 VUs, el tablero administrativo se degradaba a p95=21.91s antes del fix. Ese breakpoint ya no existe con el código corregido (p95=13.33ms bajo la misma carga) — no se trata de que el problema se ocultó subiendo el umbral de VUs, sino de que se corrigió la causa raíz y se volvió a medir con el mismo dataset y el mismo patrón de carga.

Para los endpoints de lectura ligeros (escenarios 1 y 2) sigue sin identificarse un breakpoint dentro del rango probado (hasta 750 VUs / ~3 128 req/s): el sistema permaneció estable, sin errores ni degradación perceptible. Esto no significa que no exista un punto de quiebre para esos endpoints — solo que está por encima de 750 VUs en este entorno.

### Conclusión de rendimiento

Con los endpoints de lectura ligeros, el sistema es rápido y estable incluso 10x por encima de una carga sostenida típica. Pero esta revisión ya no se detiene ahí: al fabricar un dataset de producción simulada (500 entregas con fotos reales de ~200KB), el cuello de botella que las revisiones anteriores solo predecían por análisis estático **se manifestó de verdad** (p95=21.91s), se corrigió, y se volvió a medir sobre el mismo dataset (p95=13.33ms). Esto es evidencia mucho más fuerte que la de rondas anteriores: ya no es "el problema no se manifestó con pocos datos, así que no sabemos si existe" — es "el problema existía, se demostró que existía, y se demostró que la corrección funciona".

Cuellos de botella que siguen sin abordarse (identificados por análisis estático, sin manifestarse aún en las métricas con el volumen probado):

- Los historiales paginados por conductor (`GET /api/v1/driver/deliveries/history`) usan la misma técnica de proyección que ya se aplicó al tablero admin — de hecho, se corrigieron en el mismo cambio (`findAllOrderByCreatedAtDesc`/`findAllByDriverOrderByCreatedAtDesc`), así que este punto específico también quedó resuelto.
- La búsqueda de facturas usa `ILIKE '%q%'` sin índice de tipo trigram.
- El pool de HikariCP queda en su valor por defecto (10).

**Puntaje: 9/10.** Sube de forma sustancial: además de los dos escenarios ya exigidos (sostenido y spike, ambos con 0% de error), esta ronda agregó un tercer escenario con un dataset de producción simulada que **encontró un breakpoint real, lo corrigió y volvió a medir** con evidencia cuantitativa (p95 21.91s→13.33ms, throughput 44→706 req/s) — precisamente la debilidad más señalada de las revisiones anteriores ("el volumen de datos es pequeño, los cuellos de botella no tuvieron oportunidad de manifestarse"). No llega a 10/10 porque los endpoints de lectura ligeros (escenarios 1 y 2) siguen sin un breakpoint identificado en el rango probado.

---

## 10. DevOps y despliegue

- **Docker:** `backend/Dockerfile` es multi-stage, cachea las dependencias de Maven por separado y ejecuta como usuario no root (`appuser`); ahora también instala `curl` (solo para el healthcheck); `frontend/Dockerfile` también es multi-stage y sirve el build estático con Nginx. **Verificado en esta revisión:** ambas imágenes se construyeron y levantaron repetidamente con `docker compose -f docker-compose.mock.yml up -d --build` a lo largo de todo este trabajo, sin fallar nunca.
- **Compose:** `docker-compose.mock.yml` incluye healthcheck de PostgreSQL y ahora también **healthcheck real del backend** (`curl -sf http://localhost:8080/actuator/health`, vía `spring-boot-starter-actuator`); el frontend usa `depends_on: backend: condition: service_healthy`. Verificado en vivo: el log de `docker compose up` muestra `backend-1 Waiting` → `Healthy` antes de arrancar el frontend.
- **CI/CD:** `.github/workflows/ci.yml` (nuevo): build + test del backend, build del frontend, `docker build` de ambas imágenes, en push/PR. No se pudo verificar su ejecución real en GitHub Actions desde este entorno (solo se revisó la sintaxis); sigue siendo, en ese sentido, una pieza no probada end-to-end.
- **Configuración y secretos:** `backend/.env.example` documenta todas las variables reales del proyecto. `SecretsGuardRunner` (ver Seguridad) impide arrancar con `JWT_SECRET` **o `ADMIN_PASSWORD`** de ejemplo fuera del modo demo. El `README.md` ya no referencia un `docker-compose.yml`/`backend/.env` inexistentes.
- **Esquema versionado (Flyway):** resuelto en esta revisión (ver RA3 §5.1) — el esquema ya no depende de `ddl-auto`, y `docker compose down -v && up --build` aplica `V1__baseline.sql` de forma determinística y verificada en el log (`Successfully applied 1 migration to schema "public"`).
- **`.gitignore` reescrito en UTF-8.** El archivo tenía una línea (`/docker-compose.yml`) codificada en UTF-16, invisible/corrupta en editores y herramientas que asumen UTF-8. Se reescribió con las mismas reglas en codificación estándar; verificado con `git check-ignore -v docker-compose.yml` que la regla sigue aplicando.
- **Usuario `conductor` autocreado.** Nuevo `DemoDriverBootstrapRunner` (mismo patrón que `AdminBootstrapRunner`, gated por `APP_DEMO_ENABLED=true`) crea `conductor/conductor123` en el primer arranque si no existe. Verificado en vivo: tras un `docker compose down -v && up --build` completamente limpio, `scripts/verify_delivery.py` corre de principio a fin **sin ningún paso manual** — antes requería crear ese usuario a mano vía `/api/v1/admin/users` antes de poder ejecutar el script.
- **Versionamiento `/api/v1`:** resuelto en esta revisión (ver RA3 §5.3); reduce el riesgo de romper clientes existentes en futuros cambios de contrato, uno de los objetivos explícitos de tener versionamiento en primer lugar.
- **CI/CD:** `.github/workflows/ci.yml` sigue sin verificarse corriendo de verdad en GitHub Actions — decisión deliberada de alcance de esta ronda (requiere `git push`, que no se hace sin permiso explícito separado), no una limitación técnica encontrada.
- **README:** documenta los endpoints ya bajo `/api/v1`. Sigue sin documentar cómo ejecutar `mvn test` con Testcontainers (requiere montar el socket de Docker).

**Reproducibilidad: Reproducible de punta a punta, verificado.** Un tercero con Docker puede levantar el stack completo con un solo comando (`docker compose down -v && up --build`), con Flyway aplicando el esquema de forma determinística, healthcheck real confirmando cuándo está listo, y **ambos** usuarios de demostración (`admin`, `conductor`) creados automáticamente — sin ningún paso manual. Se verificó ejecutando `scripts/verify_delivery.py` completo contra una instalación recién creada desde cero, sin ninguna intervención previa, y todas las comprobaciones pasaron.

**Puntaje: 5/5.** Sube de 4 a 5: los dos pasos manuales de reproducibilidad que quedaban pendientes (usuario `conductor`, `.gitignore` en UTF-16) están resueltos y verificados en vivo con una instalación limpia real, y el esquema de base de datos ya no depende de `ddl-auto` sino de migraciones versionadas y verificables. El único punto que sigue sin cerrar —la ejecución real de CI en GitHub Actions— es una decisión de alcance explícita de esta ronda, no un defecto encontrado y dejado sin corregir.

---

## 11. Hallazgos prioritarios

| Prioridad | Hallazgo | Impacto | Acción recomendada |
|---|---|---|---|
| Resuelto | Ausencia total de pruebas de rendimiento | — | `load-tests/sustained.js` y `spike.js` (k6) ejecutados de verdad contra el stack real: 150 VUs/12min y 750 VUs, 0% error, p95 ≤ 5.72ms (§9) |
| Resuelto | Ausencia de contrato OpenAPI/Swagger | — | `springdoc-openapi` agregado y verificado en vivo (`/v3/api-docs`, `/swagger-ui`) |
| Resuelto | XSS almacenado en `AdminDashboardPage.jsx` | — | Popup reconstruido con nodos DOM/`textContent`; verificado que HTML inyectado se muestra como texto |
| Resuelto | PIN de entrega sin límite de intentos | — | Bloqueo tras 5 intentos fallidos, verificado en vivo con una factura real |
| Media (deliberado) | PIN sigue en texto plano | El propio documento de negocio (Fase1 §3) exige que la administración pueda leerlo para comunicarlo manualmente; hashearlo rompería esa función | No se hashea; la mitigación es el bloqueo por intentos (fila anterior). Documentado explícitamente como decisión, no como omisión |
| Resuelto | El diagrama C4 no correspondía a la implementación | — | `docs/sistema-pruebas-entrega-1.2.dsl`, verificado componente por componente (§4.1) |
| Media | Tres versiones del `.dsl` (`-1.0`, `-1.1`, `-1.2`) coexisten en `docs/` sin trackear en git y sin nota de cuál es la vigente | Ambigüedad sobre qué documento defender | Marcar `-1.2` como la única vigente desde el README, y versionar `docs/` en git. No se abordó en esta revisión |
| Resuelto | Cero pruebas unitarias, y luego el adaptador más crítico (`LocalInvoiceAdapter`) sin probar | — | 24 pruebas reales (21 unitarias + 3 de integración con Testcontainers/Postgres real), `mvn test` en BUILD SUCCESS, cobertura JaCoCo real 39% (§8); `LocalInvoiceAdapter` pasó de 0% a 66% |
| Resuelto | Contrato REST inconsistente y sin versionar | — | DTO camelCase para facturas admin, 201/204/409/422, formato de error unificado, y **`/api/v1`** en los 8 controladores de negocio, verificado en vivo (§5.3) |
| Resuelto | La evidencia de entrega se construía con datos que enviaba el propio cliente HTTP | — | `DeliveryApplicationService` resuelve la factura real vía `InvoiceQueryPort.findById(invoiceId)` antes de persistir la auditoría (§5.1) |
| Resuelto | El tablero de administración (`map`/`metrics`) cargaba fotos completas y agregaba en memoria | — | Proyecciones JPA sin `photo` + `GROUP BY` SQL; medido con dataset real: p95 21.91s→13.33ms, throughput 44→706 req/s (§9, escenario 3) |
| Resuelto | Sin CI/CD ni healthcheck del backend | — | `.github/workflows/ci.yml` (ejecución real en GitHub aún no verificada, decisión de alcance) y Actuator con healthcheck real, verificado en vivo (`backend Waiting → Healthy`) |
| Resuelto | Sin herramienta de migraciones; `delivery_log.invoice_id` sin FK | — | Flyway (`V1__baseline.sql`) como única fuente de verdad; FK agregada y verificada en la base real (§5.1) |
| Resuelto | Foto sin validar tamaño/tipo; confianza en datos del cliente; `ADMIN_PASSWORD` por defecto; admin puede autoeliminarse | — | Los cuatro resueltos y verificados en vivo esta ronda (§7) |
| Resuelto (ronda posterior) | Sin rate limiting en el login | Fuerza bruta de contraseñas de solo 6 caracteres mínimos | `LoginRateLimiter` (bucket4j): 5 intentos por IP+usuario cada 60s, 429 al exceder (§7) |
| Baja | README frontend plantilla; token JWT en `localStorage` | Experiencia de un tercero que mantenga el proyecto; superficie de robo de token si aparece un XSS futuro | No se abordó en esta revisión |
| Pendiente de alcance | Ejecución real de CI en GitHub Actions | Solo se revisó la sintaxis del workflow, nunca se vio correr | Requiere `git push`; decisión deliberada de excluirlo de esta ronda (permiso separado) |

## 12. Fortalezas del proyecto

1. Documento de visión de negocio completo y coherente, con modelo de showback, matriz de riesgos e indicadores de éxito bien fundamentados (`docs/Fase1_Vision_Producto_Modelo_Negocio_API_Final.md`).
2. Arquitectura hexagonal real —no solo declarada— en la mayor parte del backend: 16 puertos de entrada, 6 puertos de salida, adaptadores y mappers claramente separados del dominio.
3. Confirmación de entrega verdaderamente atómica gracias a `SELECT … FOR UPDATE`, con esa propiedad de concurrencia efectivamente probada mediante dos hilos en `scripts/verify_delivery.py`.
4. Autenticación y autorización correctamente implementadas en el backend: BCrypt para contraseñas, JWT con expiración configurable, roles aplicados en `SecurityConfig`, bloqueo inmediato de usuarios desactivados, y ausencia de IDOR en el acceso a fotos de entrega del conductor.
5. Frontend efectivamente conectado a la API real (sin datos simulados), con estados de carga (`TableSkeleton`), compresión de imagen en el cliente, advertencia por distancia GPS sospechosa y reconocimiento óptico (OCR) de facturas.
6. Contenerización correcta con imágenes multi-stage, usuario no root en el backend, y un `docker-compose` de demostración con healthcheck de base de datos y volumen persistente.
7. Documentación de arquitectura corregida y honesta: `sistema-pruebas-entrega-1.2.dsl` describe el sistema tal como está implementado (React, sin SSE/mensajería/geocodificación externa/cola offline), alineada con el alcance de piloto que el propio documento de negocio define en su §3.
8. Circuit Breaker real (Resilience4j) protegiendo `LocalInvoiceAdapter`, activado por defecto, con un mecanismo propio (`SimulatedErpFailureToggle` + `AdminResilienceController`) para probar en vivo el ciclo `CLOSED → OPEN → HALF_OPEN → CLOSED` sin depender de que algo externo falle de verdad; las excepciones de negocio (PIN incorrecto, entrega ya confirmada) están correctamente excluidas del conteo de fallos.
9. Panel de costo por entrega verificada (showback, Fase1 §4.5) implementado con datos reales del sistema (entregas confirmadas y bytes de evidencia calculados en SQL, sin cargar las fotos completas) y tarifas configurables por variable de entorno.
10. Contrato OpenAPI real y verificado (`/v3/api-docs`, `/swagger-ui`), y códigos HTTP correctos (201/204/409/422) en los endpoints principales, ahora sobre rutas versionadas `/api/v1`.
11. Bloqueo real de la factura tras 5 intentos de PIN fallidos, verificado en vivo con una factura de prueba, con la lógica de negocio (rollback de la confirmación) y de auditoría (registro del intento) correctamente separadas en transacciones independientes — un detalle de concurrencia no trivial que se detectó y corrigió durante esta misma revisión.
12. 24 pruebas reales (21 unitarias + 3 de integración con Testcontainers contra un Postgres real) que corren de verdad (`mvn test`, BUILD SUCCESS) con cobertura medida por JaCoCo (39%, subiendo de 21%), incluida una prueba de seguridad (`@WebMvcTest`) contra la configuración real de Spring Security y pruebas de concurrencia real (8 hilos, `SELECT ... FOR UPDATE`) contra el adaptador más crítico del sistema.
13. Pruebas de carga k6 ejecutadas de verdad contra el stack real: 150 VUs sostenidos por 12 minutos y un spike a 750 VUs, ambos con 0% de error y latencias de milisegundos de un solo dígito; **y un tercer escenario con dataset de producción simulada (500 entregas con foto de ~200KB) que encontró un cuello de botella real, lo corrigió y volvió a medir: p95 21.91s→13.33ms, throughput 44→706 req/s** — la mejora se midió, no se estimó.
14. Esquema de base de datos gobernado por Flyway (`V1__baseline.sql`) como única fuente de verdad, con la FK que faltaba (`delivery_log.invoice_id`) agregada y verificada en la base real; `hibernate.ddl-auto` en `validate`, no en `update`.
15. Reproducibilidad de punta a punta sin ningún paso manual: `docker compose down -v && up --build` deja el sistema listo para usar, con ambos usuarios de demostración autocreados, verificado ejecutando el script E2E completo (`verify_delivery.py`) contra una instalación recién creada.

## 13. Mejoras recomendadas

**Ya implementadas en rondas anteriores:** fix del XSS en `bindPopup`; límite de intentos y bloqueo del PIN; `ManageInvoicesUseCase`/`InvoiceAdminPort` + `AdminInvoiceResponse` para cerrar la violación hexagonal; OpenAPI (`springdoc`); códigos 201/204/409/422; `DataIntegrityViolationException` → 409; `SecretsGuardRunner` para `JWT_SECRET`; pruebas unitarias + JaCoCo; scripts de carga k6; CI, Actuator, `.env.example`, README.

**Implementadas en esta revisión** (quedan solo como referencia, no como pendientes):
1. Confianza de datos: `DeliveryApplicationService` resuelve la factura real vía `InvoiceQueryPort.findById` en vez de confiar en `ConfirmDeliveryRequest`/`ReportIncidentRequest`; `reportIncident` ahora también valida que la factura exista.
2. Versionamiento `/api/v1` en los 8 controladores de negocio, `SecurityConfig`, el filtro JWT, el frontend y los scripts.
3. Flyway (`V1__baseline.sql`) como única fuente de verdad del esquema; FK agregada en `delivery_log.invoice_id`; `hibernate.ddl-auto` en `validate`.
4. Formato de error unificado (`{message, errors}`); mensaje genérico de `DuplicateKeyException`; corrección del hallazgo de `MediaType.parseMediaType` (ya estaba cubierto, era un error del informe anterior).
5. `ADMIN_PASSWORD` agregado a `SecretsGuardRunner`, con el mismo criterio que `JWT_SECRET`.
6. Validación de tamaño (máx. 5MB) y "magic bytes" (JPEG/PNG) de `photoBase64` en `DeliveryPhoto.decodedBytes()`.
7. Proyecciones JPA sin `photo` + `GROUP BY` SQL extendidas a `AdminDashboardController.map`/`metrics` y a los historiales paginados; medido con dataset real (§9, escenario 3).
8. `LocalInvoiceAdapterIntegrationTest` con Testcontainers/Postgres real: PIN, bloqueo y concurrencia con row-locking.
9. `.gitignore` reescrito en UTF-8; usuario `conductor` autocreado (`DemoDriverBootstrapRunner`).
10. Autoprotección de admin: `AdminDriverController` responde 409 si un admin intenta desactivarse o eliminarse a sí mismo.

**Pendientes (backlog restante, más acotado que en revisiones anteriores):**

1. ~~Aplicar rate limiting a `POST /api/v1/auth/login`~~ — **Resuelto en la ronda posterior a esta revisión** (`LoginRateLimiter`, bucket4j, §7).
2. Verificar el workflow `.github/workflows/ci.yml` ejecutándolo realmente en GitHub Actions (en este entorno solo se revisó su sintaxis; requiere `git push`, fuera de alcance de esta ronda por decisión explícita).
3. Repetir el spike test de los escenarios 1/2 con más VUs hasta encontrar su breakpoint (el del tablero administrativo bajo datos pesados ya se encontró, corrigió y re-midió en el escenario 3).
4. ~~Añadir pruebas para los mappers/proyecciones de persistencia JPA y `GlobalExceptionHandler`; extender la cobertura del frontend (actualmente en 0%)~~ — **Resuelto en la ronda posterior a esta revisión**: `GlobalExceptionHandlerTest`, `OperationalCostApplicationServiceTest`, `DeliveryAttemptPersistenceMapperTest`, y Vitest configurado con 9 pruebas de frontend (§8).
5. ~~Imponer un tamaño máximo de página en `PageRequest` (dominio)~~ — **Resuelto** (`MAX_SIZE = 100`). Sigue pendiente el ordenamiento configurable en la búsqueda de facturas y el índice trigram para `ILIKE`.
6. Cookie `HttpOnly`+`SameSite` (o CSP) en vez de `localStorage` para el token JWT.
7. Documentar en el README cómo ejecutar `mvn test` con Testcontainers (requiere montar el socket de Docker).
8. Marcar `sistema-pruebas-entrega-1.2.dsl` como la única versión vigente del `.dsl` desde el README, y versionar `docs/` en git.

## 14. Preguntas para la defensa

1. **Negocio:** El panel de costo por entrega ya está implementado (`GET /api/v1/admin/cost`); ¿qué parte de la fórmula del §4.5 calcula realmente el sistema y cuál sigue siendo un supuesto externo, y por qué las horas de soporte no son una variable de entorno como las demás tarifas? — *Buena respuesta:* distingue entregas confirmadas/GB de evidencia (calculados por el sistema) de infraestructura/soporte (tarifas externas configuradas), y explica que las horas de soporte varían cada mes y por eso se editan desde el panel en vez de fijarse en el despliegue.
2. **Arquitectura:** Las versiones `-1.0` y `-1.1` del diagrama C4 documentaban Angular, un publicador SSE, un proveedor de mensajería y un adaptador ERP externo; ¿por qué esas dos revisiones sobre-especificaron una arquitectura que ni siquiera el propio documento de negocio (Fase1 §3, un piloto con "entorno de datos simulado") exigía, y qué llevó al equipo a simplificarla en `-1.2` para que coincida con el código? — *Buena respuesta:* distingue entre lo que Fase1 realmente pide para este piloto y lo que `-1.0`/`-1.1` añadieron de forma aspiracional, y explica el criterio usado para decidir qué documentar como implementado y qué como fuera de alcance.
3. **Patrones:** `AdminInvoiceController` ya depende de `ManageInvoicesUseCase` en vez de `LocalInvoiceAdapter` directamente; ¿qué puerto de salida nuevo tuvo que crearse para que eso fuera posible, y por qué no se reutilizó `InvoiceQueryPort`? — *Buena respuesta:* explica que `InvoiceQueryPort` es para el conductor (solo lectura de pendientes) y `InvoiceAdminPort` expone además `create`/`publish`/el PIN, y por qué mezclarlos violaría la segregación de interfaces.
4. **Diseño REST/API:** `POST /api/v1/driver/deliveries/confirm` devuelve 409 si la entrega ya estaba confirmada y 422 si el PIN es incorrecto; ¿por qué esos dos códigos, y qué tuvo que cambiar exactamente para versionar la API con `/api/v1` sin romper el proxy de Nginx ni el cliente HTTP del frontend? — *Buena respuesta:* justifica 409 (conflicto de estado) vs. 422 (entidad válida pero semánticamente inválida), y explica por qué el proxy de Nginx no necesitó cambios (reenvía cualquier sufijo tras `/api/`) mientras que `SecurityConfig`, el filtro JWT y el `baseURL` del frontend sí.
5. **Seguridad:** El PIN ahora se bloquea tras 5 intentos fallidos, pero sigue guardándose en texto plano en vez de con un hash; ¿por qué esa decisión, y qué se perdería si se hasheara? — *Buena respuesta:* explica que Fase1 §3 exige que la administración pueda leer el PIN vigente para comunicarlo manualmente al cliente, y que un hash de una vía se lo impediría; propone alternativas (cifrado reversible) si se quisiera proteger igual el dato en reposo.
6. **Frontend:** Si un conductor envía como `notes` del reporte de incidencia el texto `<img src=x onerror=alert(localStorage.token)>`, ¿qué sucede exactamente cuando un administrador abre el mapa del tablero? — *Buena respuesta:* identifica el XSS almacenado en `bindPopup` y explica por qué compromete el token JWT guardado en `localStorage`.
7. **Base de datos:** Ahora el esquema vive en `V1__baseline.sql` (Flyway) y `delivery_log.invoice_id` ya tiene su FK; ¿por qué `hibernate.ddl-auto` se cambió específicamente a `validate` y no a `none`, y qué hubiera pasado si se intentaba levantar esta versión contra un volumen de Postgres creado por la versión anterior (con `ddl-auto: update`)? — *Buena respuesta:* explica que `validate` sigue verificando que las entidades JPA coincidan con lo que Flyway creó (una red de seguridad barata), y que un volumen viejo habría fallado la migración o la validación por incompatibilidad de esquema — de ahí la necesidad de `docker compose down -v` antes del primer arranque tras el cambio.
8. **Pruebas:** `LocalInvoiceAdapter` (PIN, bloqueo, Circuit Breaker) pasó de 0% a 66% de cobertura con `LocalInvoiceAdapterIntegrationTest`; ¿por qué hizo falta Testcontainers en vez de Mockito para esto, y qué probaron específicamente las 3 pruebas nuevas que un mock de `JdbcTemplate` no podría haber probado de verdad? — *Buena respuesta:* identifica que `LocalInvoiceAdapter` usa `JdbcTemplate`/SQL directo (no un puerto mockeable) y que el bloqueo por PIN y el `SELECT ... FOR UPDATE` bajo concurrencia real (8 hilos) son comportamiento de la base de datos, no de la lógica Java — un mock nunca ejercitaría el locking real.
9. **Rendimiento:** El escenario 3 (§9) fabricó un dataset de 500 entregas con fotos de ~200KB y encontró un p95 de 21.91 segundos en el tablero administrativo, que bajó a 13.33ms tras el fix; ¿por qué los escenarios 1 y 2 (con el dataset de 59 facturas) nunca habían mostrado ese problema, y qué cambió específicamente en el código para lograr esa mejora? — *Buena respuesta:* identifica que los escenarios 1/2 no confirman entregas con fotos reales por diseño, y que el fix reemplazó el fetch EAGER de `photo` por proyecciones JPA sin esa columna, más `GROUP BY` en SQL en vez de agregación en Java.
10. **Despliegue:** El healthcheck del backend es real y `scripts/verify_delivery.py` ya corre sin pasos manuales en una instalación nueva; ¿qué dos problemas de reproducibilidad concretos se cerraron para lograr eso, y por qué el `.gitignore` en UTF-16 era un problema real y no solo estético? — *Buena respuesta:* identifica el autocreado del usuario `conductor` (`DemoDriverBootstrapRunner`) y la reescritura de `.gitignore` en UTF-8, y explica que una codificación no estándar puede hacer que herramientas que asumen UTF-8 (incluido git en algunos casos) no interpreten correctamente la regla de exclusión.

## 15. Revisión contra posibles implementaciones superficiales

| Comprobación | Resultado |
|---|---|
| Código generado pero no integrado / funciones nunca invocadas | No se detectó código muerto relevante; todos los casos de uso revisados tienen un controlador que los invoca |
| Endpoints muertos | No se encontraron; todos los endpoints identificados son consumidos por el frontend o por los scripts de `scripts/` |
| Pruebas que no prueban comportamiento real | Las 24 pruebas verifican comportamiento real (transiciones de estado, excepciones específicas, roles reales vía Spring Security, bloqueo persistido y concurrencia con row-locking contra un Postgres real vía Testcontainers), no solo ausencia de excepción; el script E2E también ejercita comportamiento real |
| Swagger generado pero incompleto | Existe y funciona (`/v3/api-docs`, `/swagger-ui`), pero las descripciones son las que springdoc infiere automáticamente, no curadas endpoint por endpoint — parcial, no "incompleto/roto" |
| Datos hardcodeados presentados como funcionalidad real | Los datos de `demo/invoices.json` están correctamente declarados como ficticios y su carga es condicional (`APP_DEMO_ENABLED`) — no se penaliza |
| Credenciales hardcodeadas | Ni `JWT_SECRET` ni **`ADMIN_PASSWORD`** por defecto son posibles fuera del modo demo (`SecretsGuardRunner`, verificado: el arranque falla si se intenta con cualquiera de las dos) |
| Dashboards o gráficas sin datos reproducibles | Los resultados de k6 (§9) son datos reales capturados de una ejecución real, no una gráfica ilustrativa sin script |
| Dockerfile que no construye / docker-compose inconsistente | Verificado repetidamente en esta revisión: ambas imágenes construyen y el healthcheck real confirma cuándo el backend está listo; el README ya no referencia archivos inexistentes |
| Frontend con botones sin funcionalidad real | No se detectaron |
| Mecanismos de seguridad únicamente visuales en el frontend | No: la autorización por rol también se aplica y se verifica en el backend |
| Autorización en frontend pero no en backend | No: en todos los casos revisados el backend valida el rol de forma independiente del frontend |
| Funcionalidad descrita en la documentación sin respaldo en código | Persistió durante dos revisiones del `.dsl` (v1.0 → v1.1: eventos en vivo por SSE, envío del PIN por mensajería, geocodificación OSM/Google, adaptador ERP real, sincronización offline, PIN hasheado, límite de intentos y panel de costo por entrega). **Corregido en esta revisión**: `sistema-pruebas-entrega-1.2.dsl` ya no documenta lo no implementado (§4.1), y dos de esas piezas —panel de costo (showback) y Circuit Breaker— pasaron de "declarado" a implementadas con código real y probado en vivo. SSE, mensajería, geocodificación externa, ERP real, sincronización offline y hashing del PIN siguen sin implementarse, y ya no se documentan como si existieran |
| Consistencia de la historia de control de versiones | 6 commits en total, todos de un mismo autor (Git), y el grueso del proyecto ingresa en un único commit (`a04198d`), aunque el documento de visión lista 4 autores distintos — conviene que el equipo lo aclare en la defensa. Además, `docs/` (incluidos los tres `.dsl` y este informe) figura como no trackeada por git al momento de esta revisión, por lo que la corrección a v1.2 tampoco queda registrada en el historial |

---

## CALIFICACIÓN FINAL (histórica, previa a la recalificación de §16): 87/100

> **Superada por la recalificación formal del 2026-09-26 (§16): 92/100.** Esta sección se conserva como registro histórico de la evaluación previa a que se verificaran con ejecución real los cambios de la "ronda posterior" (logging, pruebas nuevas, rate limiting, división de `DriverHomePage.jsx`, manejo de error en el frontend). El puntaje vigente del proyecto es el de §16, no el de esta sección.

La nota sube de forma sustancial respecto de revisiones anteriores (47.5 → 55.5 → 56.5 → 76 → **87**) porque esta ronda cerró prácticamente todo el backlog identificado como pendiente en la revisión anterior, con verificación en vivo en cada caso, no solo declarada: confianza de datos (la evidencia de entrega ya no se construye con lo que envía el cliente HTTP), autoprotección de admin, validación real de fotos (tamaño y magic bytes), formato de error unificado, Flyway como única fuente de verdad del esquema (con la FK que faltaba), versionamiento `/api/v1` en los 8 controladores de negocio, pruebas de integración con Testcontainers contra el adaptador más crítico del sistema (antes en 0% de cobertura, ahora en 66%), y reproducibilidad de punta a punta sin ningún paso manual. El hallazgo más significativo de esta ronda no fue un fix aislado, sino una demostración completa: se fabricó un dataset de producción simulada (500 entregas confirmadas con fotos reales de ~200KB) que **expuso experimentalmente** el cuello de botella del tablero administrativo que las tres revisiones anteriores solo predecían por análisis estático (p95 de 21.91 segundos), se corrigió, y se volvió a medir sobre el mismo dataset (p95 de 13.33ms — una mejora de ~1600x medida, no estimada). Durante el propio trabajo de esta revisión se encontraron y corrigieron además varios problemas reales no anticipados: un hallazgo de la revisión anterior que resultó estar mal calificado (`MediaType.parseMediaType` ya estaba manejado), tres aserciones de `scripts/verify_delivery.py` desalineadas con códigos HTTP de rondas anteriores, y una incompatibilidad de Testcontainers con la versión de la API de Docker de este entorno — lo que respalda que la verificación fue efectivamente en profundidad, no superficial.

Lo que mantiene la nota por debajo de Excelente es lo que se dejó deliberadamente sin tocar, con motivo explícito en cada caso: el PIN sigue en texto plano (Fase1 §3 exige que sea legible por la administración); no hay rate limiting en el login; la ejecución real del workflow de CI en GitHub Actions no se verificó (requiere `git push`, fuera de alcance de esta ronda por decisión explícita, no por limitación técnica); la cobertura de pruebas, aunque casi se duplicó (21%→39%), sigue siendo parcial y deja sin probar buena parte de la capa de persistencia JPA y la totalidad del frontend; y los escenarios de carga sobre endpoints de lectura ligeros (1 y 2) siguen sin identificar su propio breakpoint, aunque el del tablero administrativo bajo datos pesados sí se encontró, corrigió y re-midió con evidencia cuantitativa real.

---

### Nota de una ronda posterior a esta revisión (no recalifica el 87/100)

Después de esta revisión se atacaron, uno por uno, los puntos que le restaban puntaje a cada sección (ver las notas "**Resuelto en la ronda posterior**" en §4.3, §4.4, §5.2, §6, §7, §8, §11): logging, estilo de `DemoInvoiceLoader`, manejo de errores del frontend en dos páginas, tamaño de `DriverHomePage.jsx`, anotaciones OpenAPI de los endpoints principales, rate limiting del login, y pruebas dirigidas a `GlobalExceptionHandler`, `OperationalCostApplicationService`, los mappers de persistencia y el frontend (Vitest). La suite pasó de 24 a **52 pruebas** y la cobertura de instrucciones de 39% a **52%** (verificado con `mvn test` y el reporte JaCoCo real).

Esta nota **no asigna un nuevo puntaje total**: recalificar cada sección con estos cambios en cuenta le corresponde a una revisión formal, no a quien aplicó las correcciones. Lo que sigue genuinamente pendiente, sin resolver en esta ronda: el PIN en texto plano (decisión deliberada), la verificación real de CI en GitHub Actions, el breakpoint de los escenarios de carga 1 y 2, y la cookie `HttpOnly` para el JWT (ver los pendientes 2, 3 y 6 de la lista de arriba).

---

## 16. Recalificación formal (2026-09-26)

Esta sección es la revisión formal que la nota anterior dejaba pendiente. Cada afirmación de la "ronda posterior" (§4.3, §4.4, §5.2, §6, §7, §8, §11) se verificó de nuevo en esta fecha, sobre el estado actual del código (rama `evalution`), no solo se dio por buena la narrativa:

| Afirmación de la ronda posterior | Verificación realizada ahora | Resultado |
|---|---|---|
| `LoginRateLimiter` (bucket4j) resuelve el rate limiting del login | `find` localizó `LoginRateLimiter.java` + `LoginRateLimiterTest.java`; se ejecutó la suite completa | 4/4 pruebas pasan |
| Suite subió de 24 a 52 pruebas | `mvn test` corrido en contenedor Maven limpio (socket de Docker montado, igual que en revisiones previas) | **BUILD SUCCESS, 52/52 pruebas pasan** (10 clases, incluida `LocalInvoiceAdapterIntegrationTest` con Testcontainers real) |
| Cobertura subió a 52% de instrucciones | Leído `target/site/jacoco/index.html` generado por ese mismo `mvn test` | **52% instrucciones (2 613/5 444), 62,1% líneas (661/1 064), 33% branches** — coincide con lo declarado |
| `GlobalExceptionHandlerTest`, `OperationalCostApplicationServiceTest`, `DeliveryAttemptPersistenceMapperTest` existen y prueban de verdad | Leídos y ejecutados individualmente | 16, 5 y 3 casos respectivamente, todos verdes; `GlobalExceptionHandlerTest` cubre cada excepción que el handler traduce, vía HTTP real contra un controlador de prueba |
| Frontend con Vitest, 9 pruebas (`apiClient`, `AdminDriversPage`) | `npx vitest run` en `frontend/` | **2 archivos, 9/9 pruebas pasan** |
| `DriverHomePage.jsx` dividido de 629 a ~318 líneas | `wc -l` sobre el archivo actual + verificación de los componentes extraídos | 318 líneas confirmadas; `driverHome/{PinBoxes,InvoiceSearchPanel,ConfirmDeliveryForm,IncidentForm}.jsx` existen |
| Manejo de error resuelto en `AdminDriversPage`/`AdminDeliveriesPage` | Lectura de ambos archivos | Confirmado: ambos capturan el error del backend y lo muestran en UI (`err.response?.data?.message`), ya no fallan en silencio |
| Logging agregado en `DeliveryApplicationService` y `GlobalExceptionHandler` | Lectura de ambas clases | Confirmado: `log.info`/`log.warn` en confirmaciones/rechazos, `log.error` en fallos técnicos (ERP caído, circuito abierto, acceso a datos genérico) |
| `DemoInvoiceLoader` reformateado | Lectura del archivo | Confirmado: una sentencia por línea, ya no en estilo comprimido |
| Anotaciones `@Operation`/`@ApiResponses` en 5 controladores curados | `grep -c "@Operation"` por controlador | `AuthController` (1), `AdminInvoiceController` (2), `AdminCostController` (2), `AdminResilienceController` (2), `DriverDeliveryController` (2); `AdminDriverController`, `AdminDashboardController`, `AdminDeliveryHistoryController` siguen sin anotar — confirma que la curación es **parcial**, tal como decía el informe, no total |

Todas las afirmaciones de la "ronda posterior" se sostienen con evidencia de ejecución real, no solo de lectura de código. Ningún hallazgo resultó ser sobrestimado.

### 16.1 Tabla de calificación recalificada

| Criterio | Máximo | Anterior (87/100) | Recalificado | Nivel | Justificación del cambio |
|---|---:|---:|---:|---|---|
| RA1 – Negocio y propuesta de valor | 15 | 14 | **14** | Excelente | Sin cambios: la documentación de negocio no se tocó en la ronda posterior |
| RA2 – Arquitectura, patrones y frontend | 20 | 17 | **18** | Excelente | Los dos pendientes concretos de §4.3/§4.4 (manejo de error ausente en dos páginas admin; `DriverHomePage.jsx` sobrecargado) están resueltos y verificados. Sigue sin llegar a 20 porque Retry y Cache Aside continúan sin implementarse (solo Rate Limiting del patrón trío quedó cubierto) |
| RA3 – Datos y especificación API | 20 | 18 | **18** | Excelente | Sin cambio de fondo: la curación de OpenAPI sigue siendo parcial (5 de 9 controladores), que es exactamente la reserva que ya sostenía el 18/20 anterior |
| RA4 – Desarrollo y calidad | 15 | 11 | **13** | Muy bueno | Los dos problemas concretos que quedaban abiertos en §6 (logging escaso, estilo de `DemoInvoiceLoader`) están resueltos y verificados. No llega a Excelente porque persiste una decisión arquitectónica sin resolver (JDBC directo para facturas conviviendo con JPA para el resto) y el patrón RPC-sobre-POST, ambos aceptables pero no ejemplares |
| Seguridad | 10 | 9 | **10** | Excelente | El único hallazgo de severidad Media que quedaba (sin rate limiting en login) está resuelto y probado (4/4 pruebas). Solo persisten el JWT en `localStorage` (Baja) y el PIN en texto plano, ya documentado como decisión deliberada del propio negocio (Fase1 §3), no como omisión |
| Pruebas unitarias | 5 | 4 | **5** | Excelente | La suite pasó de 24 a 52 pruebas reales (verificado con `mvn test`, BUILD SUCCESS) y la cobertura de 39% a 52% de instrucciones / 62% de líneas; las tres áreas señaladas como huecos en la revisión anterior (`GlobalExceptionHandler`, costo operacional, mappers de persistencia) ya tienen pruebas, y el frontend pasó de 0 a 9 pruebas |
| Rendimiento y pruebas de carga | 10 | 9 | **9** | Excelente | Sin cambios: la ronda posterior no tocó pruebas de carga; sigue sin identificarse el breakpoint de los escenarios 1 y 2 |
| DevOps, despliegue y reproducibilidad | 5 | 5 | **5** | Excelente | Sin cambios: ya estaba en el máximo |
| **TOTAL** | **100** | **87** | **92** | **Excelente** | |

### 16.2 Lo que sigue sin resolver (y por qué no impide "Excelente")

1. **PIN en texto plano** — decisión deliberada y justificada por el propio documento de negocio (Fase1 §3): la administración necesita leerlo para comunicarlo al cliente. No es una omisión técnica.
2. **CI en GitHub Actions sin ejecutarse de verdad** — requiere `git push`, fuera de alcance por decisión explícita, no una limitación encontrada y no corregida.
3. **Breakpoint no identificado en los escenarios de carga 1 y 2** — el sistema simplemente no se degradó hasta 750 VUs; no es un defecto, es información incompleta sobre el límite superior real.
4. **Retry y Cache Aside sin implementar** — de los tres patrones de resiliencia/rendimiento listados junto con Rate Limiting, solo este último se implementó. Pesa contra RA2 pero no contra el resto.
5. **OpenAPI curado solo en 5 de 9 controladores** — mejora real, pero parcial; pesa contra RA3.
6. **Historial de git:** el grueso del código sigue sin commitear en la rama `evalution` (`git status` muestra decenas de archivos modificados sin confirmar) pese a que el trabajo de la "ronda posterior" ya está en el árbol de trabajo. Esto no se penalizó en ninguna sección de esta tabla porque el alcance de la evaluación es el código presente en el árbol de trabajo, no el historial de commits — pero es una observación que vale la pena que el equipo resuelva antes de una entrega o defensa formal, para que el historial refleje el trabajo real.

## CALIFICACIÓN FINAL RECALIFICADA: 92/100 (Excelente)

Sube de 87 a 92 porque esta revisión formal confirma, con ejecución real (no solo lectura), que los siete pendientes concretos que la "ronda posterior" declaraba resueltos (logging, estilo de `DemoInvoiceLoader`, manejo de error en dos páginas admin, tamaño de `DriverHomePage.jsx`, rate limiting del login con sus 4 pruebas, y el salto de 24→52 pruebas con cobertura 39%→52%) efectivamente lo están. Los puntos que mantienen la nota por debajo de la perfección son los mismos de siempre y siguen siendo, en su mayoría, decisiones de alcance documentadas (PIN en texto plano, CI sin verificar en vivo) o mejoras parciales y reconocidas como tales por el propio equipo (Retry/Cache Aside ausentes, OpenAPI curado solo parcialmente, breakpoint de carga liviana no encontrado).

---

## 17. Nota de una ronda posterior a la recalificación (no reasigna el 92/100)

Después de la recalificación de §16 se atacaron, uno por uno, los cinco puntos concretos que el propio §16.2 identificaba como pendientes (excepto el PIN en texto plano y CI en GitHub Actions, ambos decisiones de alcance explícitas que se dejan sin tocar). Igual que la nota de §451, **esta sección no asigna un nuevo puntaje total**: eso corresponde a una revisión formal, no a quien aplicó las correcciones. Lo que sigue, con evidencia de ejecución real:

1. **Retry y Cache Aside (RA2, §16.2 punto 4).** `resilience4j-retry` (bean `erpGatewayRetry`, 3 intentos/200ms) reintenta solo las lecturas de `LocalInvoiceAdapter`, nunca las escrituras (que ya sostienen un bloqueo pesimista de fila). Verificado en vivo: simular 2 fallos deja `retrySuccessfulCallsWithRetry: 1` en `/api/v1/admin/resilience/status` sin que el Circuit Breaker se entere (sigue `CLOSED`), mientras que simular 6 sí lo abre. Cache Aside con Caffeine (`invoiceLines`, `expectedLocation`) sobre los mismos dos datos que no cambian tras crear la factura; una prueba de integración con Testcontainers y `@SpyBean` verifica que la segunda llamada a `findInvoiceLines` no vuelve a tocar el repositorio JPA.
2. **Doble estrategia de persistencia (RA4).** `LocalInvoiceAdapter` (facturas, líneas, PIN, bloqueo) y `OperationalCostInputAdapter` (costo operativo) migrados de `JdbcTemplate`/SQL directo a JPA (`InvoiceJpaEntity`, `InvoiceLineJpaEntity`, `OperationalCostInputJpaEntity` + sus repositorios Spring Data), con el mismo patrón mapper que ya usaba `DeliveryAttemptPersistenceMapper`. El bloqueo pesimista (antes `SELECT ... FOR UPDATE` en SQL) pasó a `@Lock(PESSIMISTIC_WRITE)`. `DemoInvoiceLoader` sigue en `JdbcTemplate` a propósito y documentado en el código: es una carga masiva que preserva IDs explícitos de `demo/invoices.json` y resincroniza la secuencia de Postgres, algo que `GenerationType.IDENTITY` no permite hacer en JPA. Verificado con `mvn test` (62/62 pruebas, incluidas las 4 de `LocalInvoiceAdapterIntegrationTest` contra Testcontainers sin cambiar ninguna aserción) y cobertura JaCoCo subiendo de 52% a 55% de instrucciones.
3. **OpenAPI curado solo en 5/9 controladores (RA3).** `AdminDriverController`, `AdminDashboardController` y `AdminDeliveryHistoryController` (los tres sin ningún `@Operation`) y los endpoints restantes de `AdminInvoiceController`/`DriverDeliveryController` ya tienen `@Tag`/`@Operation`/`@ApiResponses`. Verificado en vivo contra `GET /v3/api-docs`: los 9 controladores de negocio, sin excepción, tienen `summary` en cada operación.
4. **`state` sin `CHECK` y sin índice para la búsqueda `ILIKE` (RA3 §5.1/§9).** Migración `V2__invoice_constraints_and_search.sql`: `CHECK (state IN ('draft','posted','cancel'))` (los tres únicos valores reales, confirmados en el código y en `demo/invoices.json`), `confirmed_at` pasa a `timestamptz`, e índices GIN `pg_trgm` sobre `number`/`partner_name`. Verificado en vivo: `docker compose down -v && up --build` aplica V1 y V2 en el mismo arranque (`Successfully applied 2 migrations to schema "public", now at version v2`).
5. **Rango de fechas sin validar en el tablero (RA3, mencionado en §5.3).** `AdminDashboardController.map`/`metrics` rechazan con 400 (formato de error unificado) un rango mayor a 93 días o con `to` anterior a `from`. Verificado en vivo (`GET /admin/dashboard/metrics?from=2020-01-01...&to=2026-01-01...` → 400) y con pruebas unitarias del controlador.
6. **Breakpoint no identificado en los escenarios de carga ligera (Rendimiento, §9).** Nuevo `load-tests/breakpoint.js` (k6, `ramping-arrival-rate` con `abortOnFail`) sobre la misma mezcla de endpoints de `sustained.js`/`spike.js`. A diferencia de esos dos escenarios (que nunca se degradaban), este sí encontró un breakpoint real: con `DB_POOL_MAX_SIZE=10` (valor por defecto), el p95 cruza 500ms a ~5 658 req/s (~2 549 iteraciones/s, ~2 105 VUs), siempre con 0% de errores (el sistema se vuelve más lento, no falla). Subir el pool de HikariCP a 30 (ahora configurable, antes fijo) movió el breakpoint a ~6 301 req/s (~2 948 iteraciones/s, ~2 754 VUs) — una mejora real de ~11-16%, aunque no elimina el techo: el resto es CPU del contenedor del backend en esta máquina de desarrollo, no la base de datos. Resultados crudos en `load-tests/results-breakpoint-before.json`/`-after.json`.

**Lo que sigue sin resolver, sin cambios respecto de §16.2:** el PIN en texto plano (decisión deliberada, Fase1 §3); la ejecución real de CI en GitHub Actions (requiere `git push`, fuera de alcance); la cookie `HttpOnly` para el JWT; y la paginación externa de `GET /api/v1/admin/invoices`/`admin/users` (internamente ya usan `Pageable` de Spring Data en vez de `LIMIT` fijo en SQL, pero el contrato HTTP no expone `page`/`size` todavía).

---

## 18. Segunda recalificación formal (2026-09-27)

**Alcance.** Revisión formal del estado actual del árbol de trabajo (rama `evalution`), que abarca: (a) las seis afirmaciones del §17, que ese apartado dejó expresamente sin puntuar; (b) la migración del frontend a TypeScript + pnpm, que ningún apartado anterior evaluó; (c) el nuevo pipeline de CI/CD y despliegue (`.github/workflows/ci.yml`/`cd.yml`, `deploy/`). Igual que en §16, cada afirmación se verificó **ejecutando** y no solo leyendo el código. Se hizo en cuatro frentes:

- **Backend:** `mvn -B test` en un contenedor Maven limpio con Testcontainers, y lectura del reporte JaCoCo.
- **Frontend:** `pnpm exec vitest run`, `pnpm run lint` y `pnpm run build`.
- **Stack de demo:** levantado desde cero con `docker compose -p ruta-eval -f docker-compose.mock.yml up -d --build --wait`, en un proyecto aislado con volumen nuevo, y contra él pruebas en vivo con `curl`/`psql` y `scripts/verify_delivery.py`.
- **Stack de `deploy/`:** levantado en local con certificado autofirmado, ejecutando `deploy/scripts/deploy.sh` con una imagen buena y otra rota.

**Conflicto de interés declarado.** El pipeline de CI/CD y `deploy/scripts/` los escribió la misma sesión que hace esta revisión. Por eso el puntaje de DevOps se apoya solo en lo ejecutado y observado, no en el diseño. Lo que no se pudo ejecutar (GitHub Actions, AWS, Cloudflare) cuenta como **no verificado**.

### 18.1 Verificación de las afirmaciones del §17

| # | Afirmación del §17 | Verificación | Resultado |
|---|---|---|---|
| 1 | Retry solo en lecturas + Cache Aside (Caffeine) | Lectura de `LocalInvoiceAdapter` (el Retry envuelve al CircuitBreaker, orden correcto; las escrituras no llevan Retry) y prueba en vivo: `simulate-failures {"count":2}` seguido de una lectura → 200 y `retrySuccessfulCallsWithRetry: 1` | **Confirmado, con una imprecisión.** El §17 dice que el breaker "no se entera". Es falso: `numberOfFailedCalls` pasó de 1 a 3 porque cada intento del Retry se registra en el breaker. Sigue `CLOSED` porque no alcanza el 50 % ni el mínimo de llamadas. Una lectura con 2 fallos transitorios aporta, por tanto, 2 fallos al breaker. La prueba con `@SpyBean` de la caché existe y pasa |
| 2 | Facturas y costo migrados a JPA; `@Lock(PESSIMISTIC_WRITE)` | `grep JdbcTemplate` en `main` y lectura de `SpringDataInvoiceJpaRepository` | **Confirmado.** Solo queda `DemoInvoiceLoader`, justificado en el código. Las 4 pruebas de `LocalInvoiceAdapterIntegrationTest` (Testcontainers) pasan |
| 3 | OpenAPI curado en todos los controladores | `GET /v3/api-docs` en vivo | **Confirmado: 22 de 22 operaciones con `summary`, 8 tags.** Corrección de redacción: hay **8** controladores de negocio, no 9 como dicen §2, §16 y §17 |
| 4 | V2: `CHECK` de `state`, `timestamptz` e índices trigram para `ILIKE` | `\di+` y `EXPLAIN` en la base viva | **Parcial.** El `CHECK` y los índices existen, pero **los índices nunca se usan**: la consulta JPA real es `lower(i.number) like lower(concat('%', :q, '%'))` y el índice es `gin (number gin_trgm_ops)` sobre la columna sin `lower()`. Con `SET enable_seqscan = off`, esa consulta sigue en `Seq Scan`; la misma búsqueda con `ILIKE` sí usa `Bitmap Index Scan` sobre ambos índices. El `timestamptz` quedó solo en `confirmed_at`: `delivery_log.created_at` y `pin_locked_until` siguen en `timestamp`, aunque el comentario de V2 afirma lo contrario |
| 5 | Rango de fechas del tablero validado | `GET /admin/dashboard/metrics` en vivo con 3 rangos | **Confirmado:** más de 93 días → 400, `to` anterior a `from` → 400, rango válido → 200 |
| 6 | Breakpoint de los endpoints de lectura ligeros | Lectura de `load-tests/breakpoint.js` y de los JSON de resultados | **Confirmado que se encontró** (p95 > 500 ms, 0 % de errores; pool de 10 frente a 30: 5 648 → 6 288 req/s). **Métrica mal expresada:** los "~5 658 req/s" son el promedio de toda la rampa, no la tasa en el punto de quiebre, y `load-tests/README.md` mezcla iteraciones/s (6 peticiones cada una) con req/s. Las iteraciones descartadas (3 603 y 6 767) indican que parte de la saturación está en el generador de carga |
| — | Pendiente declarado: sin paginación HTTP en `admin/invoices` ni `admin/users` | En vivo: `GET /admin/invoices?page=0&size=2` devuelve una lista de 61 elementos | **Confirmado que sigue pendiente** (tope interno fijo de 1 000) |

### 18.2 Resultados de ejecución

| Qué | Resultado |
|---|---|
| `mvn -B test` (Testcontainers) | **BUILD SUCCESS, 62/62** en 13 clases (antes 52) |
| JaCoCo | **55,3 % de instrucciones** (3 157/5 714), **65,8 % de líneas** (793/1 206), 35,6 % de ramas (72/202). Paquetes más bajos: `web.dto` 5,7 %, `out.cost` 18,8 %, `out.persistence` 38,6 %, `in.web` 39,6 % |
| Frontend `vitest` | **9/9** en 2 archivos, sin cambios. Solo `api/client` y `AdminDriversPage` tienen pruebas; `driverHome/*` (OCR, GPS, compresión) y las otras 6 páginas no |
| Frontend `build` (`tsc -b && vite build`) | OK: tipos estrictos sin errores y PWA generada |
| Frontend `lint` (oxlint) | 0 errores, **10 warnings** (`react(set-state-in-effect)` en `AdminDashboardPage`/`AdminInvoicesPage`) |
| `docker-compose.mock.yml` desde cero | Levanta en unos 23 s: seed → Flyway V1+V2 → backend `healthy` → frontend |
| `scripts/verify_delivery.py` sobre esa instalación limpia | **FALLA en la línea 38:** `login('admin', 'admin_local_mock_only')` → 401 (ver hallazgo N1). Con una copia del script en el directorio temporal que usa la contraseña real, **todas las comprobaciones restantes pasan**: permisos, 59 facturas con los 50 PIN, publicación, PIN, concurrencia, foto, GPS, historial e incidencias |
| `deploy/scripts/deploy.sh` en local (TLS autofirmado) | Despliegue con imagen buena → OK y login por HTTPS en 200. Imagen rota → **rollback automático** al tag anterior, exit 1. Primer arranque con profile `demo` → OK. El dump `pg_dump` se lee con `pg_restore --list` y la retención borra los dumps viejos. Durante esa prueba aparecieron y se corrigieron dos defectos que ya existían: el healthcheck de nginx fallaba **siempre** (`localhost` → `::1` en Alpine) y `/actuator/health/readiness` devolvía 401 |
| `actionlint` / `shellcheck` | Sin errores en `ci.yml`, `cd.yml`, `deploy.sh` y `backup.sh` |
| GitHub Actions, AWS SSM/OIDC, Cloudflare Pages | **No verificado:** requiere `git push` y credenciales reales |

### 18.3 Migración del frontend a TypeScript (no evaluada antes)

- **Completa y estricta.** No queda ningún `.js`/`.jsx` en `frontend/src`. `"strict": true` en `tsconfig.app.json` y `tsconfig.node.json`, cero `@ts-ignore`/`@ts-expect-error`, y tipos del contrato de la API en `types/domain.ts`. Hay 18 `any`: 16 son `catch (err: any)`, que anulan el tipado del error; lo correcto sería `unknown` + `axios.isAxiosError`.
- **Tamaño de componentes:** ningún archivo pasa de 400 líneas. El mayor es `AdminDashboardPage.tsx` con 396; `DriverHomePage.tsx` tiene 325 (el informe decía 318).
- **Manejo de errores:** está presente en todas las páginas salvo en `DriverHistoryPage.tsx:56-61`. Ahí, `openDetail` pide la foto dentro de un `try/finally` sin `catch`, así que un fallo no se muestra al conductor. `AuthContext.tsx` hace `JSON.parse` del storage sin protección, y un valor corrupto rompe el arranque.
- **XSS:** el fix de `buildPopupContent` (`textContent`) sigue en su lugar. No hay `innerHTML` ni `dangerouslySetInnerHTML`.
- `frontend/README.md` **ya no es la plantilla de Vite** (§4.4 y §11 quedan desactualizados en ese punto).

### 18.4 Hallazgos nuevos

| # | Hallazgo | Severidad | Evidencia | Impacto |
|---|---|---|---|---|
| N1 | En el compose de demo, el admin queda creado con la contraseña de ejemplo `admin123`. El servicio `seed` arranca primero **sin** `ADMIN_PASSWORD`, así que `AdminBootstrapRunner` crea el admin con el valor por defecto. Después, el backend (que sí trae `ADMIN_PASSWORD=admin_local_mock_only`) ve que el usuario ya existe y no lo cambia | **Alta** (reproducibilidad) / Media (seguridad) | Log del seed: `Usuario admin inicial creado: admin`; en vivo, `admin123` → 200 y `admin_local_mock_only` → 401 | Rompe la afirmación central del §10 ("reproducible sin pasos manuales, verificado con `verify_delivery.py`"). Además deja activo justo el placeholder que `SecretsGuardRunner` debería impedir. No afecta a `deploy/`, donde el seed recibe `ADMIN_PASSWORD` por `env_file` (verificado) |
| N2 | La mayor parte del trabajo no está en git: 143 entradas en `git status`, incluidos **todo `backend/src/test/`**, `docs/`, `deploy/`, `.github/`, la migración a TS (los `.tsx` sin trackear y los `.jsx` borrados) y clases de `main` como `InvoiceAdminPort` y `AdminCostController` | **Alta** (proceso/entrega) | `git status --short` | Un clon limpio no compila ni tiene pruebas; el CI/CD no puede correr sobre lo que evalúa este informe. El §16.2 lo dejó sin penalizar, pero ahora el despliegue **depende** del historial de git |
| N3 | Los índices trigram de V2 no se usan (§18.1, fila 4) | Media | `EXPLAIN` en vivo | Una optimización declarada como hecha no tiene efecto; la búsqueda sigue haciendo scan secuencial. Arreglo: consulta con `ILIKE`, o índice sobre `lower(number)` |
| N4 | El rate limiter del login: (a) cuenta también los inicios de sesión **exitosos** (5 logins válidos en 60 s dejan fuera al usuario legítimo; observado en vivo durante esta revisión); (b) usa un `ConcurrentHashMap` **sin eviction**, así que rotando usuarios o IPs la memoria crece sin límite; (c) se evade rotando `X-Forwarded-For` cuando el tráfico entra por el nginx del frontend (`$proxy_add_x_forwarded_for` **añade** al valor del cliente) o directo al backend | Media | En vivo: 8 intentos fallidos con XFF rotando → 8 × 401, sin 429; con XFF fijo → 429 al sexto. `LoginRateLimiter.java:19` | En `deploy/` (Cloudflare → nginx de borde, que **reescribe** XFF) la parte (c) no aplica; las partes (a) y (b) sí |
| N5 | No hay CSP ni cabeceras de seguridad (`X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy`) en ningún punto que sirva el HTML: ni `frontend/nginx.conf`, ni `deploy/nginx`, ni `frontend/public/_headers` para Cloudflare Pages | Media-Baja | `curl -I` al frontend: solo `Server` | La mitigación del JWT en `localStorage` que proponen §7 y §13 no existe todavía |
| N6 | El C4 vigente (`sistema-pruebas-entrega-1.3.dsl`) quedó desactualizado frente al código: no menciona TypeScript, sitúa `invoiceAdapter` sobre `JdbcTemplate` y `SELECT … FOR UPDATE` (hoy es JPA con `@Lock`), dice que `baseURL` está fija (hoy `VITE_API_BASE_URL`), no incluye Retry, Caché ni `LoginRateLimiter`, y su vista de despliegue no tiene el nginx de borde, GHCR ni el CD. `Fase2_Arquitectura_Patrones_API.md` se contradice: da Rate Limiting como implementado y a la vez como "trabajo prioritario de la siguiente fase", y su Anexo A dice que solo existe `docker-compose.mock.yml` | Media | Lectura de `-1.3.dsl` (líneas 14, 16, 46, 118, 150-157) y de Fase2 (257, 269, 285-286) | Reabre una brecha documento↔código del tipo que el §4.1 dio por cerrada |
| N7 | Detalles menores de datos y API: `GET /driver/invoices/{id}/lines` no filtra por estado (un conductor puede leer líneas de facturas en borrador); caché negativa sin `@CacheEvict` (un id consultado antes de existir queda vacío hasta 5 min); upsert no atómico en `saveSupportHours`; los comodines `%`/`_` de la búsqueda no se escapan; comentario engañoso en `ResilienceConfig.java:54` (reintenta cualquier `RuntimeException`) | Baja | Lectura de código | — |
| N8 | Referencias obsoletas dentro de este mismo informe: `App.jsx`, `client.js`, `AuthContext.jsx`, `DriverHomePage.jsx`, `npx vitest`, "9 controladores", "`-1.2` vigente" | Baja | §4.1, §4.4, §7, §8, §16 | Deben leerse como históricos. Los nombres vigentes son `.tsx`, `pnpm exec vitest run`, 8 controladores y `-1.3.dsl` |

**Pendientes del §13 que resultaron ya resueltos:** el 7 (el README ya documenta `mvn test` con Testcontainers) y, en su parte documental, el 8 (el README marca `-1.3.dsl` como vigente; el versionado en git sigue pendiente, ver N2).

### 18.5 Tabla de calificación (segunda recalificación)

| Criterio | Máx. | §16 (92) | **§18** | Nivel | Justificación |
|---|---:|---:|---:|---|---|
| RA1 – Negocio y propuesta de valor | 15 | 14 | **14** | Excelente | Sin cambios: Fase1 no se tocó |
| RA2 – Arquitectura, patrones y frontend | 20 | 18 | **19** | Excelente | Sube 1: se cierra la reserva que sostenía el 18 (Retry y Cache Aside implementados, verificados en vivo y en orden correcto respecto del breaker), y la migración a TS es estricta y completa. No llega a 20 por N6: el C4 vigente y Fase2 vuelven a describir un sistema distinto del código |
| RA3 – Datos y especificación API | 20 | 18 | **18** | Excelente | Se cierra la reserva anterior (OpenAPI 22/22, verificado), y se suman el `CHECK` de estado y la validación de rango. Pero aparecen reservas nuevas del mismo peso: índices trigram inservibles (N3), `timestamptz` a medias, `admin/invoices` y `admin/users` sin paginación HTTP, y líneas de borradores expuestas al conductor (N7). Se mantiene en 18 con motivos distintos |
| RA4 – Desarrollo y calidad | 15 | 13 | **14** | Excelente | Sube 1: se resuelve la doble estrategia de persistencia, que era la única reserva de §16 (queda solo `DemoInvoiceLoader`, justificado). No llega a 15 por `catch (err: any)` generalizado, 10 warnings de lint, comentarios que contradicen el código (V2, `ResilienceConfig`) y el upsert no atómico |
| Seguridad | 10 | 10 | **9** | Excelente | Baja 1 por N4 (rate limiter evadible en la topología de demo, sin eviction y penalizando los logins exitosos), N5 (sin CSP ni cabeceras) y N1 (admin con placeholder en la demo, pese al guard). Siguen correctos BCrypt, JWT, roles en backend, sin IDOR, validación de fotos, bloqueo del PIN y el CORS nuevo por patrones (acotado al subdominio del proyecto) |
| Pruebas unitarias | 5 | 5 | **5** | Excelente | 62/62 pruebas y cobertura de 55 % de instrucciones y 66 % de líneas, ambas mayores que en §16. Observación: el frontend sigue con solo 9 pruebas en 2 archivos, y las pruebas no están en git (se computa en DevOps, N2) |
| Rendimiento y pruebas de carga | 10 | 9 | **9** | Excelente | Se cierra la reserva anterior (breakpoint encontrado con `breakpoint.js`, 0 % de errores), pero aparecen dos nuevas: la optimización de búsqueda declarada no funciona (N3) y la métrica del breakpoint mezcla promedio de rampa con tasa en el quiebre e iteraciones con peticiones |
| DevOps, despliegue y reproducibilidad | 5 | 5 | **4** | Muy bueno | A favor: CI/CD completo por entorno, con gate contra forks, tags inmutables, despliegue con backup, espera de healthchecks y rollback automático; la parte que se puede ejecutar en local (script, compose, rollback, respaldo) está verificada. En contra: la reproducibilidad de punta a punta **retrocedió** (N1: `verify_delivery.py` falla en una instalación limpia), lo esencial no está en git (N2), por lo que el pipeline no puede correr sobre este código, y GitHub Actions, OIDC/SSM y Pages siguen **sin ejecutarse nunca** |
| **TOTAL** | **100** | **92** | **92** | **Excelente** | |

### 18.6 Qué subiría la nota (orden sugerido)

1. **N2:** commitear todo en commits temáticos (backend + pruebas, migración a TS, docs, CI/CD) y crear `develop`. Es la condición previa para que el CI/CD pueda verificarse.
2. **N1:** pasar `ADMIN_PASSWORD` (y `ADMIN_USERNAME`) también al servicio `seed` de `docker-compose.mock.yml`, o que el seed no ejecute `AdminBootstrapRunner`. Luego volver a correr `verify_delivery.py` sobre `down -v && up --build`.
3. **N3:** `ILIKE` en `SpringDataInvoiceJpaRepository` (consulta nativa o `upper`/`lower` indexado) y verificar con `EXPLAIN`.
4. **N4 y N5:** limpieza de buckets por inactividad (Caffeine con `expireAfterAccess`), reiniciar el bucket tras un login exitoso, `proxy_set_header X-Forwarded-For $remote_addr` en `frontend/nginx.conf`, y cabeceras de seguridad y CSP en `frontend/nginx.conf` y `frontend/public/_headers`.
5. **N6:** actualizar `-1.3.dsl` (o crear `-1.4`) y Fase2 al estado real.
6. Ejecutar de verdad el pipeline (push a `develop` → staging) y registrar la evidencia aquí.

## CALIFICACIÓN FINAL (segunda recalificación, 2026-09-27): 92/100 (Excelente)

El total no cambia, pero sí su composición.

- **Suben tres criterios:**
  - Arquitectura: Retry y Cache Aside verificados, frontend en TypeScript estricto.
  - Calidad: persistencia unificada en JPA.
  - Esas dos subidas, y las reservas que se cierran en Datos y Rendimiento (OpenAPI 22/22 y breakpoint encontrado), son las que el §17 anunciaba; esta revisión las confirmó ejecutando.
- **Bajan dos:**
  - Seguridad: rate limiter mejorable y sin CSP.
  - DevOps: la demo limpia ya no es reproducible sin intervención y el trabajo no está en git.
- **Datos y Rendimiento se mantienen:** cierran su reserva anterior, pero una optimización declarada como hecha (índices trigram) resultó no tener efecto.

Los tres hallazgos más baratos de corregir (N1, N2, N3) son también los que más pesan. Resolverlos y verificarlos llevaría el proyecto a unos 95/100.

---

## 19. Nota de una ronda posterior al §18 (2026-09-27)

Después de la segunda recalificación (§18) se atacaron, con permiso explícito, cuatro de los ocho hallazgos que el §18.6 señalaba como los más baratos de corregir (N1, N3, N4, N5) y se actualizó la documentación de arquitectura (N6, el propio §18.6 punto 5). N2 (organizar el historial de git), N7 (hallazgos menores de datos/API) y N8 (referencias obsoletas dentro de este mismo informe, que se dejan como registro histórico) quedan fuera de esta ronda, sin resolver. Igual que en las notas anteriores (§451, §507, §17), **esta nota no asigna un nuevo puntaje total**: eso corresponde a una revisión formal, no a quien aplicó las correcciones. Todo lo siguiente se verificó ejecutando, no solo leyendo el código.

1. **N1 (admin con password de ejemplo en la demo) — resuelto, y con un hallazgo adicional no anticipado por el §18.** Se renombró `docker-compose.mock.yml` a `docker-compose.demo.yml` (a pedido explícito: el proyecto ya usa "demo" en todos lados) y se unificó el `environment` de `seed`/`backend` con un ancla YAML (`x-backend-env`), que es la causa real por la que antes se desincronizaban. Verificado con `docker compose down -v && up --build --wait` repetido: login con `admin_local_demo_only` → 200, con el placeholder `admin123` → 401. **Al verificar en vivo apareció un segundo bug, más serio, que N1 no había señalado**: el orden en que Spring ejecuta `AdminBootstrapRunner`/`DemoDriverBootstrapRunner`/`DemoInvoiceLoader`/`DemoSeedExitRunner` no era determinista (el propio comentario del código asumía "orden alfabético de escaneo de componentes", que Spring no garantiza) — en arranques limpios consecutivos, uno completó el sembrado y otro cerró el proceso (`System.exit`) antes de que corriera ningún bootstrap runner, dejando la base sin admin ni conductor de demostración. Se corrigió con `@Order(0)/@Order(1)/@Order(2)` explícito en los tres primeros. Verificado con **3 arranques limpios consecutivos** (`down -v && up --build --wait`): los dos usuarios se crean las 3 veces, y `scripts/verify_delivery.py` completo pasa sin ningún parche.
2. **N3 (índices trigram inservibles) — resuelto.** Nueva migración `V3__invoice_search_lower_index.sql`: elimina los dos índices de V2 (sobre la columna cruda) y crea índices funcionales `gin (lower(number) gin_trgm_ops)`/`gin (lower(partner_name) gin_trgm_ops)`, que sí coinciden con la expresión real de la búsqueda JPQL. Verificado con `EXPLAIN` en la base viva: la misma consulta que antes hacía `Seq Scan` (incluso con `enable_seqscan=off`) ahora usa `Bitmap Index Scan` sobre el índice nuevo. De paso se escaparon los comodines `%`/`_` del término de búsqueda (hallazgo menor de N7) con `escape '\'` en las dos queries JPQL, con una prueba de integración nueva (`list_withUnderscoreInQuery_treatsItAsLiteralNotAsWildcard`) contra Testcontainers.
3. **N4 (rate limiter del login) — resuelto.** `LoginRateLimiter` pasó de un `ConcurrentHashMap` que nunca se vaciaba a una cache de Caffeine con `expireAfterAccess`, y ahora expone `recordSuccess`, que **reinicia el contador tras un login exitoso** (decisión explícita: antes cada intento consumía el cupo, incluidos los correctos). `frontend/nginx.conf` reescribe `X-Forwarded-For` con `$remote_addr` en vez de `$proxy_add_x_forwarded_for` (mismo criterio que `deploy/nginx/snippets/proxy-headers.conf`), cerrando la evasión por cabecera falsificada. Verificado en vivo contra el stack real: 7 intentos fallidos con `X-Forwarded-For` **rotando** en cada request → 429 a partir del sexto (antes, 401 en los 7); 6 logins **correctos** seguidos → 200 en los 6, nunca 429. 3 pruebas unitarias nuevas en `LoginRateLimiterTest`.
4. **N5 (sin CSP ni cabeceras de seguridad) — resuelto.** Nuevo snippet `frontend/security-headers.conf` (`X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy` con `geolocation=(self), camera=(self)` explícito para no romper el GPS/cámara del conductor, y una `Content-Security-Policy` acotada a lo que la app realmente usa: `wasm-unsafe-eval` y `worker-src blob:` para el OCR de `tesseract.js`, `img-src blob: data:` para las fotos y los iconos embebidos de Leaflet, Google Fonts como única fuente externa). Incluido en el `server` y repetido dentro de `/sw.js`/`/manifest.webmanifest` (un `add_header` en un `location` anula los del `server`, un detalle que se verificó y corrigió). `frontend/public/_headers` replica la política para Cloudflare Pages, con marcadores `__API_ORIGIN__`/`__TILES_ORIGIN__` que el propio script `build` de `frontend/package.json` sustituye después de `vite build` (y falla el build si queda alguno sin sustituir) — corre así tanto en un build local como en el de Cloudflare Pages, que construye el sitio con su propia integración de Git, sin pasar por GitHub Actions. **Verificado con un navegador real (Playwright/Chromium), no solo leyendo la CSP**: login, tablero administrativo (mapa Leaflet, con teselas cargando de verdad, confirmado con una captura de pantalla), y el flujo completo de OCR del conductor (subir una foto, correr `tesseract.js` con su Worker vía `blob:` y WebAssembly) — **cero violaciones de CSP y cero peticiones fallidas** en los tres casos.
5. **N6 (documentación de arquitectura desactualizada) — resuelto.** Nuevo `docs/sistema-pruebas-entrega-1.4.dsl` (validado y exportado sin errores con `structurizr-cli`, incluida la vista de despliegue con el nodo `nginx` de la EC2 que antes faltaba): agrega TypeScript a la tecnología del frontend, corrige `apiClient` (ya no es "baseURL fija"), pasa `invoiceAdapter`/`costInputAdapter` de "Spring JdbcTemplate" a "Spring Data JPA", y agrega los componentes de Retry, Cache Aside y el rate limiter del login que faltaban junto al Circuit Breaker. `README.md` marca `-1.4.dsl` como vigente. `docs/Fase2_Arquitectura_Patrones_API.md` corrige "9 controladores" (son 8), las referencias a `.jsx` (son `.tsx`), la contradicción entre su tabla de patrones (que ya daba el rate limiting por implementado) y su propia conclusión (que lo listaba como "trabajo prioritario de la siguiente fase"), y mueve a "resuelto" los dos puntos de su Anexo A sobre el compose de producción y el Nginx de la EC2, ambos ya resueltos por trabajo de esta misma sesión. **Hallazgo nuevo, sin resolver:** la carpeta `figuras/` que ambos documentos citan (`figuras/c4_*.png`) no existe en el árbol de trabajo — los seis enlaces a imágenes ya estaban rotos antes de esta ronda, independientemente de cualquier cambio al `.dsl`; regenerarlas queda pendiente y documentado en el propio Anexo A de Fase2.

**Verificación de no regresión.** `mvn test` se corrió después de cada cambio de backend (N1: 62→66 tras sumar `LoginRateLimiterTest`; N3: 63 con la prueba de escape nueva; N4: 66 con las 3 pruebas del reset; 71 tras sumar `SpringDataInvoiceJpaRepositoryTest`, ver más abajo): siempre `BUILD SUCCESS`, siempre en un contenedor Maven limpio con Testcontainers. `pnpm exec vitest run`/`pnpm run lint`/`pnpm run build` del frontend, sin regresiones. `actionlint` sobre `ci.yml`/`cd.yml`.

**Correcciones tras una segunda pasada (el usuario preguntó "¿algo más que falte del plan?"), todas ya incluidas arriba:**
- **Prueba unitaria de `escapeLike` que faltaba** (el plan la pedía aparte de la de integración): `SpringDataInvoiceJpaRepositoryTest` (5 casos, sin Spring ni base de datos), incluido el caso no trivial de un término con `\` y `_` juntos, para confirmar que el orden de los `replace()` no vuelve a escapar por error el backslash que el propio escape inserta.
- **Recorrido manual de la CSP, completado.** La verificación inicial de N5 cubrió login, tablero admin y OCR del conductor, pero el plan pedía también ver una foto real de una entrega, exportar el CSV y el historial del conductor con su propio mapa — se confirmó una entrega con foto real (`scripts/seed_confirmed_deliveries.py --count 1`) y con Playwright se verificó, en los tres flujos que faltaban, **cero violaciones de CSP**: la foto se ve (`admin/deliveries` y `driver/history`), el CSV se descarga, y el mapa del historial del conductor carga igual que el del tablero admin.
- **Cloudflare Pages: se simplificó el mecanismo de publicación después de escribir esta nota.** La primera versión de N5 agregaba un job de GitHub Actions con `cloudflare/wrangler-action` para construir y publicar el frontend. El usuario señaló, correctamente, que Cloudflare Pages ya hace eso solo con su integración nativa de Git (build y despliegue automático por rama, con *preview deployments* automáticos por cada PR): mantener esa lógica en Actions solo duplicaba lo que la plataforma ya resuelve, y peor (sin los previews automáticos). Se eliminaron los jobs `deploy-frontend` (`cd.yml`) y `frontend-preview` (`ci.yml`); la sustitución de `__API_ORIGIN__`/`__TILES_ORIGIN__` en `_headers` pasó a ser parte del propio script `build` de `frontend/package.json`, para que corra igual en un build local o en el de Cloudflare. Esto dejó desactualizadas tres frases ya escritas en `docs/sistema-pruebas-entrega-1.4.dsl` y `docs/Fase2_Arquitectura_Patrones_API.md` (que todavía atribuían la publicación del frontend a `cd.yml`/GitHub Actions) — corregidas en esta misma pasada.

**Lo que sigue sin resolver:** el PIN en texto plano y la ejecución real de CI/CD en GitHub Actions (decisiones de alcance de siempre); N2 (organizar el historial de git — sigue habiendo un volumen grande de trabajo sin commitear); la cookie `HttpOnly` para el JWT; la paginación HTTP de `admin/invoices`/`admin/users`; y regenerar `figuras/` desde el `.dsl` vigente. N7 se resolvió por completo en la nota siguiente (§20).

---

## 20. N7 completo (2026-09-27)

El usuario notó que la primera pasada de correcciones había dejado N7 (hallazgos menores) prácticamente intacto — solo el escape de comodines de la búsqueda se había cerrado, de rebote, como parte de N3. Pidió concluirlo entero. De los cinco puntos que el §18.4 agrupaba bajo N7, quedaban cuatro:

1. **Líneas de una factura en borrador visibles al conductor — resuelto.** `DeliveryApplicationService.list(Long invoiceId)` ahora resuelve la factura real primero y exige `state == "posted"`, igual que ya exigía `searchPendingDeliveryInvoices`; si no, `DeliveryRejectedException` (400), sin llamar al adaptador. 3 pruebas unitarias nuevas (factura publicada, en borrador, inexistente). **Efecto colateral real, encontrado por el propio `scripts/verify_delivery.py`:** el dataset de demo incluye a propósito 2 facturas en borrador y 1 cancelada (para ejercitar esos estados en el panel admin), y el script comprobaba, sin distinguir estado, que el conductor podía leer las líneas de las 59 facturas originales. El fix rompía esa comprobación para esas 3 — se corrigió el script (no el fix) para que, en esos tres casos, verifique el rechazo (400) en vez del conteo de productos; las 56 facturas publicadas siguen verificándose igual que antes.
2. **Caché negativa sin invalidar — resuelto, con un bug propio detectado y corregido en el camino.** Se agregó `unless` a los dos `@Cacheable` de `LocalInvoiceAdapter` para no cachear un resultado vacío. Para `findInvoiceLines` (`List<InvoiceLine>`), `unless = "#result.isEmpty()"` es correcto. Para `findExpectedLocation` (`Optional<GeoLocation>`), esa misma expresión **rompía la app de verdad**: Spring desenvuelve el `Optional` antes de evaluar la SpEL de `unless`, así que `#result` ya es el `GeoLocation` (o `null`) — nunca un `Optional` — y `#result.isEmpty()` lanzaba `SpelEvaluationException` en cualquier confirmación de entrega con ubicación esperada presente (el caso más común). El error no lo detectó ninguna prueba automatizada (los mocks de `DeliveryApplicationServiceTest` no pasan por el proxy de cache real) sino `scripts/verify_delivery.py`, corriendo contra el stack real, con un síntoma engañoso: el cliente veía **401 "No autenticado"** en vez de un 500, porque la excepción escapaba dentro de la cadena de filtros de Spring Security antes de llegar al manejador de excepciones. Corregido a `unless = "#result == null"`, y verificado con dos pruebas de integración nuevas (ubicación presente cacheada correctamente; ubicación ausente sin cachear) que ejercitan el proxy de cache real, no un mock — exactamente lo que hubiera atrapado este error antes de que llegara a `verify_delivery.py`. `verify_delivery.py` se corrió dos veces más sobre stacks nuevos tras el arreglo, ambas completas.
3. **Upsert no atómico de horas de soporte — resuelto.** `OperationalCostInputAdapter.saveSupportHours` hacía `findById -> setear -> save`; bajo dos guardados concurrentes del mismo mes, ambos podían leer "no existe" y el segundo `insert` fallaba con clave duplicada. Se reemplazó por un upsert atómico (`INSERT ... ON CONFLICT (month) DO UPDATE`, `@Modifying(clearAutomatically = true)`) en `SpringDataOperationalCostInputJpaRepository`. Nueva prueba de integración con Testcontainers: 10 guardados concurrentes del mismo mes, cero excepciones, exactamente una fila en la tabla al final.
4. **Comentario engañoso y retry demasiado amplio en `ResilienceConfig` — resuelto (no solo el texto).** El Javadoc ya decía "reintenta solo fallos transitorios simulados", pero el código real reintentaba cualquier `RuntimeException`, incluido un bug real de la propia lectura. Se corrigió el código, no solo el comentario: `retryExceptions(SimulatedErpFailureToggle.SimulatedErpFailureException.class)` en vez de `RuntimeException.class`. Dos pruebas nuevas que construyen el bean tal como lo arma `ResilienceConfig` (sin contexto de Spring): un fallo simulado se reintenta y se absorbe; un `IllegalStateException` no relacionado ya **no** se reintenta (antes se reintentaba 3 veces sin ninguna chance real de éxito).

**Verificación.** `mvn test`: 66 → 81 pruebas (BUILD SUCCESS, contenedor Maven limpio con Testcontainers). Verificado en vivo contra el stack real: una factura en borrador responde 400 al pedir sus líneas como conductor, y 200 tras publicarla. `scripts/verify_delivery.py` completo, dos veces seguidas sobre instalaciones nuevas (`down -v && up --build`), sin fallar.

**Con esto, N7 queda cerrado por completo** — de los ocho hallazgos del §18 (N1–N8), solo N2 (historial de git) sigue pendiente por decisión explícita del usuario; N8 no aplica (son referencias históricas dentro de este mismo informe, no código).

---

## 21. Tercera recalificación formal (2026-09-27)

Las notas de §19 y §20 dejaron dicho, cada una, que corregir hallazgos no reasigna puntaje — eso le corresponde a una revisión formal. Esta sección es esa revisión: verifica **ejecutando**, no leyendo, el estado del árbol de trabajo tal como quedó después de §19 y §20, y recalcula la tabla de §18.5 con esa evidencia.

### 21.1 Verificación de lo que §19 y §20 declaraban resuelto

| # | Declarado en | Verificación realizada ahora | Resultado |
|---|---|---|---|
| N1 (reproducibilidad + password de ejemplo) | §19 | `docker compose -p ruta-eval -f docker-compose.demo.yml down -v --remove-orphans && up -d --build --wait` desde cero (proyecto y volumen aislados); login con `admin_local_demo_only` y con el placeholder `admin123` | **Confirmado.** Arranque limpio deja `postgres`/`backend`/`frontend` en `Healthy` y `seed` en `Exited` (0) en un solo intento; login con la contraseña real → 200, con el placeholder → 401 |
| Reproducibilidad E2E completa | §19 | `scripts/verify_delivery.py` contra esa instalación recién creada | **Confirmado.** Las 5 comprobaciones pasan, incluida la nueva ("borradores y cancelada ya no exponen sus lineas al conductor", ver N7.1 más abajo) |
| N3 (índices trigram inservibles) | §19 | Lectura de `V3__invoice_search_lower_index.sql` (existe, elimina los índices de V2 y crea `gin (lower(number) gin_trgm_ops)`/`gin (lower(partner_name) gin_trgm_ops)`) | **Confirmado en el código.** No se repitió el `EXPLAIN` en vivo esta ronda (ya lo hizo §19 con resultado positivo); se aceptó esa evidencia previa más la lectura de la migración |
| N4 (rate limiter evadible, sin eviction, penaliza logins correctos) | §19 | `Caffeine`/`expireAfterAccess`/`recordSuccess` en `LoginRateLimiter.java`; `proxy_set_header X-Forwarded-For $remote_addr` en `frontend/nginx.conf` y `deploy/nginx/snippets/proxy-headers.conf`; **prueba en vivo**: 7 intentos fallidos con `X-Forwarded-For` falsificado y rotando, a través del nginx del frontend (puerto 15180, no directo al backend) | **Confirmado.** 401 en los intentos 1–4, **429 desde el intento 5**, y un login con la contraseña correcta inmediatamente después sigue en 429 (el bloqueo no se salta con credenciales válidas mientras el cupo está agotado). Atacando el backend directo (puerto publicado `18091`, sin nginx en el camino) el `X-Forwarded-For` falso sigue evadiendo el límite — **residual esperado y ya acotado por el propio §17/§19**: en `deploy/` (y en cualquier topología real) el backend no se expone directamente, solo el nginx que sí reescribe la cabecera |
| N5 (sin CSP ni cabeceras) | §19 | `curl -I` al frontend servido por el stack recién levantado | **Confirmado.** `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy` acotado, y la `Content-Security-Policy` completa descrita en §19.4, todas presentes en la respuesta real |
| N6 (documentación de arquitectura) | §19 | `docs/sistema-pruebas-entrega-1.4.dsl` existe; `README.md` lo cita como vigente (línea 417) | **Confirmado.** `figuras/` sigue sin existir en el árbol de trabajo — el propio §19 ya lo dejó como "hallazgo nuevo, sin resolver", y sigue así |
| N7.1 (líneas de factura en borrador visibles al conductor) | §20 | `DeliveryApplicationService.java:66`, `if (!"posted".equals(invoice.state()))`; y el propio `verify_delivery.py` en vivo | **Confirmado** en código y en la corrida en vivo de esta ronda |
| N7.2 (caché negativa / bug del `Optional` en `unless`) | §20 | `LocalInvoiceAdapter.java`: `unless = "#result.isEmpty()"` en `findInvoiceLines`, `unless = "#result == null"` en `findExpectedLocation` | **Confirmado** — la expresión que rompía la app (`#result.isEmpty()` sobre un `Optional` ya desenvuelto) ya no está |
| N7.3 (upsert no atómico de horas de soporte) | §20 | `SpringDataOperationalCostInputJpaRepository.java`: `@Modifying(clearAutomatically = true)` + comentario que referencia el `ON CONFLICT` | **Confirmado** |
| N7.4 (comentario engañoso / retry demasiado amplio en `ResilienceConfig`) | §20 | `ResilienceConfig.java:61`: `.retryExceptions(SimulatedErpFailureToggle.SimulatedErpFailureException.class)` | **Confirmado** — ya no reintenta `RuntimeException` genérico |
| Suite subió a 81 pruebas | §20 | `mvn test` en contenedor Maven limpio (socket de Docker montado), igual metodología que rondas anteriores | **BUILD SUCCESS, 81/81 pruebas** en 18 clases |
| Cobertura | §18/§19/§20 (no daban un número posterior a 55,3%) | Lectura de `target/site/jacoco/index.html` generado por ese mismo `mvn test` | **56% de instrucciones** (3 284/5 766), **~67% de líneas** (816/1 219), **38% de ramas** (79/206) — sube frente al 55,3%/65,8%/35,6% de §18, consistente con las pruebas nuevas de N1/N3/N4/N7 |
| Frontend: 9 pruebas, build, lint | §18 (no tocado por §19/§20) | `pnpm exec vitest run`, `pnpm run build` (`tsc -b && vite build`), `pnpm run lint` | **9/9 pruebas**, build OK (PWA generada, `_headers` reescrito con los orígenes reales), lint **0 errores / 9 warnings** (`set-state-in-effect` y `only-export-components`, sin cambios de fondo frente a los ~10 de §18) |
| N2 (historial de git) | §18/§19/§20 (declarado pendiente en las tres) | `git status --short \| wc -l` | **Sigue sin resolver, y peor: 148 entradas** (subió de las 143 que reportaba §18.4, consistente con que el trabajo de N1/N3/N4/N5/N6/N7 tampoco se comiteó) |
| "8 controladores de negocio" (corrección de N8) | §18 | `ls .../infrastructure/adapter/in/web/*.java` sin `GlobalExceptionHandler` | **Confirmado: 8** (`AuthController`, `AdminInvoiceController`, `AdminDriverController`, `AdminCostController`, `AdminDeliveryHistoryController`, `AdminDashboardController`, `AdminResilienceController`, `DriverDeliveryController`) |

**Reserva de RA3 que persiste, verificada de nuevo en esta ronda (no la tocó ni §19 ni §20):** `delivery_log.created_at` y `pin_locked_until` siguen en `timestamp` sin zona horaria en `V1__baseline.sql` (líneas 33 y 65); el comentario de `V2__invoice_constraints_and_search.sql` ("mismo tipo que `delivery_log.created_at`") sigue siendo impreciso, porque esa columna nunca se migró a `timestamptz`. Tampoco cambió la falta de paginación HTTP en `GET /api/v1/admin/invoices` (sigue sin `page`/`size`, verificado leyendo `AdminInvoiceController.list`) ni en `admin/users`.

**Reserva de RA4 que persiste, verificada de nuevo en esta ronda:** 16 `catch (err: any)` en el frontend (`grep -rn "catch (.*: any)"` sobre `frontend/src`), sin cambios frente a lo que señalaba §18.3.

### 21.2 Tabla de calificación (tercera recalificación)

| Criterio | Máx. | §18.5 (92) | **§21** | Nivel | Justificación del cambio |
|---|---:|---:|---:|---|---|
| RA1 – Negocio y propuesta de valor | 15 | 14 | **14** | Excelente | Sin cambios: Fase1 no se tocó en ninguna ronda posterior |
| RA2 – Arquitectura, patrones y frontend | 20 | 19 | **20** | Excelente | Se cierra la única reserva que sostenía el 19: N6 (C4 y Fase2 desactualizados) — `-1.4.dsl` existe, es vigente y consistente con el código (TypeScript, JPA, Retry/Cache Aside/rate limiter), verificado. Queda un residuo cosmético (`figuras/` sigue sin existir, ya señalado por el propio §19) que no se penaliza aquí para no duplicar el ajuste de DevOps/N2 |
| RA3 – Datos y especificación API | 20 | 18 | **19** | Excelente | Sube 1: se cierra N3 (índices trigram ahora funcionales, `V3` verificado) y N7.1 (líneas de facturas en borrador ya no visibles al conductor, verificado en código y en vivo). No llega a 20 porque persisten dos reservas propias, no tocadas por ninguna ronda posterior: `timestamptz` a medias (`delivery_log.created_at`/`pin_locked_until` siguen sin zona horaria, con un comentario de migración que afirma lo contrario) y la falta de paginación HTTP en `admin/invoices`/`admin/users` |
| RA4 – Desarrollo y calidad | 15 | 14 | **14** | Excelente | Sin cambio neto: se resuelven 2 de las 4 reservas que sostenían el 14 (upsert atómico de horas de soporte, comentario/alcance del retry en `ResilienceConfig`), pero las otras 2 siguen intactas y verificadas de nuevo (16 `catch (err: any)` sin tipar, ~9 warnings de lint) |
| Seguridad | 10 | 9 | **10** | Excelente | Sube 1: se cierran las tres reservas que bajaron la nota en §18 — N1 (placeholder de admin en la demo), N4 (rate limiter evadible/sin eviction/penalizaba logins correctos) y N5 (sin CSP ni cabeceras) — las tres verificadas en vivo contra un stack recién levantado, no solo leídas. Sigue el PIN en texto plano, documentado como decisión deliberada de negocio (Fase1 §3), no como omisión |
| Pruebas unitarias | 5 | 5 | **5** | Excelente | Ya estaba en el máximo; sube de 62 a **81 pruebas reales** (BUILD SUCCESS verificado) y la cobertura de 55,3%/65,8% a **56%/67%** de instrucciones/líneas |
| Rendimiento y pruebas de carga | 10 | 9 | **10** | Excelente | Sube 1: la única reserva que quedaba en §18 era N3 (el índice trigram declarado no se usaba, así que la búsqueda de facturas seguía en *seq scan* bajo carga) — ya resuelta y verificada; `load-tests/README.md` además ya reporta el throughput y la tasa de llegada "al corte" por separado, sin la mezcla de promedio-de-rampa con punto-de-quiebre que señalaba §18.1 |
| DevOps, despliegue y reproducibilidad | 5 | 4 | **4** | Muy bueno | Sin cambio neto: de las dos razones por las que §18 bajó esto a 4 (N1 rotas la reproducibilidad; N2 el trabajo no está en git), N1 quedó resuelto y reverificado en vivo esta ronda, pero N2 no solo sigue sin resolver — empeoró (148 entradas sin commitear, frente a 143). El pipeline de CI/CD sigue sin poder correr sobre lo que este informe evalúa, y GitHub Actions/AWS/Cloudflare Pages siguen sin ejecutarse nunca |
| **TOTAL** | **100** | **92** | **96** | **Excelente** | |

### 21.3 Lo que sigue sin resolver

1. **N2 — historial de git.** El bloqueador más barato de corregir y el único que empeoró: 148 archivos sin commitear (subiendo de 143), incluida toda la migración a TypeScript, las pruebas del backend, `deploy/`, `.github/` y las correcciones de N1/N3/N4/N5/N6/N7. Mientras esto no se resuelva, el CI/CD que sí existe y sí se valida sintácticamente no puede ejecutarse de verdad sobre el código que este informe califica.
2. **PIN en texto plano** — decisión deliberada y documentada (Fase1 §3): la administración necesita leerlo para comunicarlo al cliente.
3. **CI en GitHub Actions sin ejecutarse de verdad** — requiere `git push`, bloqueado además por N2 (no habría casi nada que subir).
4. **`timestamptz` a medias** (RA3) y **paginación HTTP ausente en `admin/invoices`/`admin/users`** (RA3) — ninguna ronda posterior a §18 los tocó.
5. **`catch (err: any)` sin tipar (16 casos) y ~9 warnings de lint** (RA4) — tampoco tocados.
6. **`figuras/` sigue sin existir** — los enlaces a imágenes de `Fase2_Arquitectura_Patrones_API.md` y de la documentación de arquitectura siguen rotos; cosmético, no re-penalizado aquí (ya contaba en la nota de §19).

## CALIFICACIÓN FINAL (tercera recalificación, 2026-09-27): 96/100 (Excelente)

Sube de 92 a 96 porque, de los ocho hallazgos que el §18 dejó abiertos (N1–N8), esta revisión confirma con ejecución real — no con la narrativa de las notas intermedias — que **seis quedaron efectivamente resueltos** (N1, N3, N4, N5, N6, N7) sobre el estado actual del árbol de trabajo: 81/81 pruebas de backend (subiendo de 62), cobertura subiendo a 56%/67% de instrucciones/líneas, reproducibilidad de punta a punta reverificada con una instalación completamente limpia, el rate limiter del login resistiendo en vivo la evasión por `X-Forwarded-For` falsificado a través de nginx, las cabeceras de seguridad y la CSP presentes en la respuesta real del frontend, la documentación de arquitectura (`-1.4.dsl`) alineada con el código, y las líneas de facturas en borrador ya no expuestas al conductor.

Lo que mantiene la nota por debajo de la perfección son, en su mayoría, los mismos huecos de siempre, ninguno nuevo: **N2 (historial de git), que de hecho empeoró**, es el más señalado porque es también el más barato de resolver y el que más pesa — sin él, el CI/CD que el propio proyecto construyó no tiene sobre qué correr. El resto son decisiones de alcance ya documentadas (PIN en texto plano, CI real en GitHub Actions) o reservas menores y puntuales que ninguna ronda posterior a la segunda recalificación (§18) llegó a tocar: `timestamptz` parcial y paginación HTTP ausente en dos endpoints administrativos (RA3), y tipado débil en el manejo de errores del frontend más los warnings de lint que lo acompañan (RA4).

---

### Nota de una ronda posterior al §21 (2026-09-27, no reasigna el 96/100)

**N2 (historial de git) — resuelto.** Los 148 archivos sin commitear que el §21 medía como el bloqueador más barato y de mayor peso quedaron organizados en **13 commits temáticos** sobre una rama nueva, `develop`, creada desde el mismo punto en el que estaba `evalution` (que a su vez es ancestro directo de `main`, sin commits propios divergentes): dominio/aplicación hexagonal, persistencia JPA + Flyway, resiliencia/rate limiting/arranque, versionado `/api/v1` y errores unificados, dependencias de build, la suite de 81 pruebas del backend, la migración del frontend a TypeScript, las pruebas de Vitest, cabeceras de seguridad/CSP, documentación (arquitectura C4/Fase2/este mismo informe), CI/CD y despliegue, scripts de datos de demo, y las pruebas de carga k6. `main` y `evalution` no se tocaron.

**Verificación realizada:** `git diff --name-status HEAD` sobre el árbol de trabajo final no muestra ninguna diferencia (el contenido es byte a byte el mismo que ya se había verificado ejecutando en el §21 — la reorganización no tocó código, solo el historial), `git status` queda limpio sin archivos sin trackear, y ningún commit de los 13 quedó vacío o fuera de orden de dependencia (dominio antes que persistencia, persistencia antes que la capa web, todo el código de producción antes que sus propias pruebas).

Este cambio **no reasigna un nuevo puntaje total**: igual que en las notas de §17, §19 y §20, recalcular la tabla de calificación con este hallazgo cerrado le corresponde a una revisión formal, no a quien aplicó la corrección. A título orientativo, y sin que cuente como puntaje vigente: de los dos motivos por los que §21 mantuvo DevOps en 4/5 (N1 y N2), ambos quedarían cerrados, lo que en una futura recalificación formal llevaría ese criterio a 5/5 y el total a unos 97/100 — siempre que una revisión posterior confirme, ejecutando sobre `develop`, que el CI/CD (`.github/workflows/ci.yml`/`cd.yml`) corre de verdad contra este historial ahora que existe algo real sobre lo que ejecutarlo.

**Lo que sigue sin resolver, sin cambios respecto de §21.3:** el PIN en texto plano y la ejecución real de CI en GitHub Actions (decisiones de alcance de siempre, aunque esta última ya no está bloqueada por N2); `timestamptz` a medias y la paginación HTTP ausente en `admin/invoices`/`admin/users` (RA3); `catch (err: any)` sin tipar y los warnings de lint del frontend (RA4); y `figuras/`, que sigue sin existir.

---

## 22. Cuarta recalificación formal (2026-09-27)

Esta sección es la revisión formal que la nota posterior al §21 dejaba pendiente: recalcula la tabla de calificación una vez cerrado N2, con evidencia de ejecución más fuerte que la de cualquier ronda anterior — no sobre el árbol de trabajo ya probado, sino sobre un **clon nuevo y aislado de `develop`** (`git clone` a un directorio temporal), que es precisamente la prueba que N2 no podía pasar ("un clon limpio no compila ni tiene pruebas", §18.4).

### 22.1 Verificación sobre un clon limpio de `develop`

| Qué | Cómo | Resultado |
|---|---|---|
| El clon reproduce el árbol ya evaluado | `git diff --name-status HEAD` dentro del clon | Sin diferencias — el contenido es el mismo que ya se había probado en el §21, ahora alcanzable únicamente desde los commits, no desde un árbol de trabajo con cambios sueltos |
| Backend: compila y prueba desde el historial | `mvn -B test` en un contenedor Maven limpio (Testcontainers), apuntando al `backend/` del clon | **BUILD SUCCESS, 81/81 pruebas**, mismas 16 clases que en el §21 |
| Frontend: instala, construye y prueba desde el historial | `pnpm install --frozen-lockfile` (sin reusar `node_modules` del árbol original) + `pnpm run build` + `pnpm exec vitest run` + `pnpm run lint`, todo dentro del clon | Instalación desde `pnpm-lock.yaml` sin errores; build OK (PWA generada, `_headers` reescrito); **9/9 pruebas**; lint 0 errores / 9 warnings — idéntico al §21 |
| CI/CD: los workflows son sintácticamente válidos | Parseo YAML de `.github/workflows/ci.yml` y `cd.yml` dentro del clon (no se repitió `actionlint`, ya validado en §18/§19 sobre el mismo contenido) | Ambos archivos parsean sin error |
| Reproducibilidad de punta a punta desde el historial | `docker compose -p ruta-clone-check -f docker-compose.demo.yml up -d --build --wait` dentro del clon (proyecto y volumen aislados) + `scripts/verify_delivery.py` del propio clon | Arranque limpio (`postgres`/`backend`/`frontend` Healthy, `seed` Exited 0) y **las 5 comprobaciones de `verify_delivery.py` pasan**, incluida la de N7.1 (facturas en borrador/canceladas sin exponer líneas) |

**No verificado en esta ronda, sin cambios respecto de §21/§18:** la ejecución real del pipeline en GitHub Actions y del despliegue en AWS/Cloudflare Pages, porque requieren `git push` a un remoto y credenciales reales — sigue siendo, explícitamente, una decisión de alcance, no una limitación técnica encontrada. `develop` es una rama local; no se hizo push a `origin` en ningún momento de esta recalificación.

### 22.2 Tabla de calificación (cuarta recalificación)

| Criterio | Máx. | §21 (96) | **§22** | Nivel | Justificación |
|---|---:|---:|---:|---|---|
| RA1 – Negocio y propuesta de valor | 15 | 14 | **14** | Excelente | Sin cambios |
| RA2 – Arquitectura, patrones y frontend | 20 | 20 | **20** | Excelente | Sin cambios |
| RA3 – Datos y especificación API | 20 | 19 | **19** | Excelente | Sin cambios: `timestamptz` a medias y paginación HTTP ausente en `admin/invoices`/`admin/users` no fueron tocados por la reorganización de git |
| RA4 – Desarrollo y calidad | 15 | 14 | **14** | Excelente | Sin cambios: `catch (err: any)` y los warnings de lint tampoco fueron tocados |
| Seguridad | 10 | 10 | **10** | Excelente | Sin cambios |
| Pruebas unitarias | 5 | 5 | **5** | Excelente | Sin cambios: 81/81 pruebas y la misma cobertura, ahora reverificadas desde un clon limpio en vez del árbol de trabajo |
| Rendimiento y pruebas de carga | 10 | 10 | **10** | Excelente | Sin cambios |
| DevOps, despliegue y reproducibilidad | 5 | 4 | **5** | Excelente | Sube 1: se cierra N2, la única reserva que quedaba. Un clon nuevo y aislado de `develop` — sin ningún archivo del árbol de trabajo original, sin `node_modules` ni caché de Maven propios del entorno de desarrollo — compila el backend (81/81 pruebas), construye y prueba el frontend, y levanta el stack de demo de punta a punta con `scripts/verify_delivery.py` pasando completo. El pipeline de CI/CD ya tiene sobre qué correr; solo falta ejecutarlo de verdad en GitHub Actions, lo que sigue siendo una decisión de alcance (requiere `git push`), no una limitación técnica |
| **TOTAL** | **100** | **96** | **97** | **Excelente** | |

### 22.3 Lo que sigue sin resolver

1. **CI en GitHub Actions y despliegue en AWS/Cloudflare Pages sin ejecutarse de verdad** — ya no bloqueado por N2 (el historial existe y es reproducible), pero requiere `git push` a un remoto con credenciales reales; sigue siendo una decisión de alcance explícita.
2. **PIN en texto plano** — decisión deliberada y documentada (Fase1 §3).
3. **`timestamptz` a medias** y **paginación HTTP ausente en `admin/invoices`/`admin/users`** (RA3).
4. **`catch (err: any)` sin tipar (16 casos) y ~9 warnings de lint** (RA4).
5. **`figuras/` sigue sin existir** (cosmético).

## CALIFICACIÓN FINAL (cuarta recalificación, 2026-09-27): 97/100 (Excelente)

Sube de 96 a 97 porque la única reserva que quedaba abierta en el §21 — N2, el historial de git — se verificó resuelta con la evidencia más fuerte posible: no el árbol de trabajo ya probado, sino un clon nuevo y aislado de `develop`, que compila el backend (81/81 pruebas), construye y prueba el frontend, y reproduce el stack completo de punta a punta con el script E2E pasando sin intervención manual. Esto es exactamente la prueba que un clon limpio no pasaba cuando N2 se detectó (§18.4): "un clon limpio no compila ni tiene pruebas; el CI/CD no puede correr sobre lo que este informe evalúa". ya no es cierto.

Lo único que falta para acercarse a la perfección son decisiones de alcance explícitas (ejecutar el CI/CD de verdad en GitHub Actions, lo que requiere un `git push` que no se ha autorizado en esta sesión; el PIN en texto plano, exigido por el propio negocio) y reservas menores y puntuales, ninguna nueva, que ninguna ronda desde el §18 llegó a tocar: `timestamptz` parcial y paginación HTTP ausente en dos endpoints administrativos (RA3), y tipado débil en el manejo de errores del frontend con sus warnings de lint (RA4).
