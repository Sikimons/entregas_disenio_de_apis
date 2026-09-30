workspace "Plataforma de verificación de entregas" "Modelo C4 v1.4: actualiza v1.3 tras la recalificación técnica en vivo de docs/EVALUACION_TECNICA.md §17/§18 -- el frontend migró a TypeScript, el adaptador de facturas dejó JdbcTemplate por Spring Data JPA, y se agregaron Retry (Resilience4j) y Cache Aside (Caffeine) junto al Circuit Breaker y al rate limiting del login. La vista de despliegue ahora incluye el Nginx de borde de la EC2 (antes ausente) y de dónde se descargan las imágenes versionadas (GHCR, publicadas por .github/workflows/cd.yml). No incluye elementos no implementados." {

    !identifiers hierarchical
    !impliedRelationships false

    model {
        // Personas
        cliente = person "Cliente" "Recibe el pedido y dicta el PIN al conductor."
        conductor = person "Conductor" "Confirma la entrega con foto, ubicación y PIN; reporta incidencias." "Role.CONDUCTOR"
        administracion = person "Administración" "Gestiona facturas y equipo; supervisa entregas e incidencias." "Role.ADMIN"

        plataforma = softwareSystem "Plataforma de verificación de entregas" "Verifica entregas con PIN, foto y ubicación. React + Spring Boot + PostgreSQL." {

            frontend = container "Aplicación web" "SPA/PWA; el rol autenticado determina la vista." "React 19 + TypeScript + Vite + vite-plugin-pwa" "Frontend" {
                authContext = component "Contexto de autenticación" "Guarda el token y el usuario." "React Context"
                apiClient = component "Cliente HTTP" "Axios con token Bearer; cierra sesión en 401." "Axios"
                loginPage = component "Inicio de sesión" "Formulario de acceso; redirige por rol." "React Component"
                driverHome = component "Pantalla del conductor" "Busca la factura, captura foto/GPS y confirma con PIN o reporta incidencia." "React Component"
                driverHistory = component "Historial del conductor" "Historial propio paginado y evidencia." "React Component"
                adminInvoices = component "Gestión de facturas" "Crea y publica facturas (genera el PIN); exporta a CSV." "React Component"
                adminDrivers = component "Gestión de equipo" "CRUD de cuentas de conductores y administradores." "React Component"
                adminDeliveries = component "Registro de entregas" "Lista paginada de entregas, ubicación y evidencia." "React Component"
                adminDashboard = component "Tablero administrativo" "Mapa y métricas por conductor; costo por entrega y estado del breaker." "React Component"
            }

            api = container "API de verificación" "API REST hexagonal bajo /api/v1 (driver/admin); OpenAPI en los 8 controladores de negocio." "Java 17 + Spring Boot 3.3" "Backend" {
                jwtFilter = component "Filtro JWT" "Valida el JWT y el rol de cada ruta; deja públicas auth, docs y health." "Spring Security Filter" "Seguridad"
                exceptionHandler = component "Manejador global de excepciones" "Manejo centralizado: 409/422/429/503/400 con formato de error único." "Spring RestControllerAdvice"

                authController = component "Controlador de autenticación" "POST /api/v1/auth/login (público)." "Spring REST Controller"
                driverController = component "Controlador del conductor" "/api/v1/driver: búsqueda, confirmación, incidencias e historial." "Spring REST Controller"
                adminInvoiceController = component "Controlador de facturas" "/api/v1/admin/invoices: crea, publica y consulta facturas y PIN." "Spring REST Controller"
                adminDriverController = component "Controlador de equipo" "/api/v1/admin/users: CRUD de cuentas." "Spring REST Controller"
                adminHistoryController = component "Controlador de historial" "/api/v1/admin/deliveries: historial paginado (page, size) y evidencia." "Spring REST Controller"
                adminDashboardController = component "Controlador del tablero" "/api/v1/admin/dashboard: mapa y métricas filtrados por from/to (maximo 93 dias)." "Spring REST Controller"
                costController = component "Controlador de costos" "Costo por entrega verificada (showback); GET /cost, PUT /support-hours." "Spring REST Controller"
                resilienceController = component "Controlador de resiliencia" "Estado del Circuit Breaker y simulador de fallas del ERP." "Spring REST Controller"

                authService = component "Servicio de autenticación" "Valida credenciales y emite el JWT." "Spring Service" "Nucleo"
                deliveryService = component "Servicio de entregas" "Confirma entregas con bloqueo pesimista en una transacción." "Spring Service" "Nucleo"
                historyService = component "Servicio de historial y métricas" "Paginación y filtrado del historial; métricas por conductor." "Spring Service" "Nucleo"
                driverMgmtService = component "Servicio de gestión de equipo" "Crea, actualiza y elimina cuentas." "Spring Service" "Nucleo"
                invoiceMgmtService = component "Servicio de gestión de facturas" "Lista, crea y publica facturas via InvoiceAdminPort." "Spring Service" "Nucleo"
                costService = component "Servicio de costo por entrega" "Combina entregas confirmadas con tarifas y horas de soporte." "Spring Service" "Nucleo"

                invoiceAdapter = component "Adaptador de facturas" "Persiste facturas, simula el ERP y bloquea el PIN tras 5 intentos; protegido por Retry, Circuit Breaker y Cache Aside." "Spring Data JPA" "Adaptador"
                deliveryLogRepository = component "Repositorio de entregas" "Log de entregas y evidencia; consultas paginadas y resumen de costo." "Spring Data JPA" "Adaptador"
                driverRepository = component "Repositorio de usuarios" "Conductores y administradores." "Spring Data JPA" "Adaptador"
                jwtAdapter = component "Adaptador de tokens" "Firma y valida JWT (HMAC-SHA256)." "io.jsonwebtoken" "Adaptador"
                passwordAdapter = component "Adaptador de contraseñas" "Delega en BCrypt." "Spring Security" "Adaptador"
                costRatesAdapter = component "Adaptador de tarifas" "Tarifas de infraestructura, almacenamiento y soporte." "Spring Config" "Adaptador"
                costInputAdapter = component "Adaptador de horas de soporte" "Guarda y edita las horas de soporte por mes." "Spring Data JPA" "Adaptador"
                circuitBreaker = component "Circuit Breaker del ERP" "Breaker 'erpGateway' sobre el adaptador de facturas; excluye rechazos de negocio; circuito abierto responde 503." "Resilience4j" "Resiliencia"
                simulatedFailureToggle = component "Simulador de fallas del ERP" "Fuerza fallas simuladas para demostrar el ciclo del breaker." "Java" "Resiliencia"
                retry = component "Retry del ERP" "3 intentos/200ms solo en lecturas de facturas; envuelve al Circuit Breaker." "Resilience4j" "Resiliencia"
                cache = component "Cache Aside de facturas" "Caffeine sobre lineas y ubicación esperada, TTL configurable." "Caffeine" "Rendimiento"
                loginRateLimiter = component "Limitador de intentos de login" "5 intentos por IP+usuario cada 60s; resetea tras login exitoso." "bucket4j + Caffeine" "Seguridad"
            }

            db = container "Base de datos" "Facturas (PIN en texto plano), usuarios, historial y costos." "PostgreSQL 16" "Database"
        }

        // Contexto
        cliente -> conductor "Dicta el PIN al recibir el pedido"
        administracion -> cliente "Comunica el PIN vigente por un canal operativo (exportado a CSV)"
        conductor -> plataforma "Busca la factura, confirma la entrega o reporta una incidencia"
        administracion -> plataforma "Gestiona facturas y equipo; supervisa entregas e incidencias"

        // Contenedores
        conductor -> plataforma.frontend "Usa, autenticado con su cuenta de conductor"
        administracion -> plataforma.frontend "Usa, autenticado con su cuenta de administrador"
        plataforma.frontend -> plataforma.api "Consume la API REST (Bearer JWT)" "JSON/HTTPS"
        plataforma.api -> plataforma.db "Lee y escribe" "JDBC"

        // Componentes del frontend
        conductor -> plataforma.frontend.loginPage "Inicia sesión usando"
        administracion -> plataforma.frontend.loginPage "Inicia sesión usando"
        plataforma.frontend.loginPage -> plataforma.frontend.apiClient "Autentica"
        plataforma.frontend.apiClient -> plataforma.frontend.authContext "Guarda el token y el usuario"
        conductor -> plataforma.frontend.driverHome "Busca, confirma y reporta usando"
        conductor -> plataforma.frontend.driverHistory "Consulta su historial usando"
        plataforma.frontend.driverHome -> plataforma.frontend.apiClient "Busca facturas, confirma entregas y reporta incidencias"
        plataforma.frontend.driverHistory -> plataforma.frontend.apiClient "Consulta el historial propio y la evidencia"
        administracion -> plataforma.frontend.adminInvoices "Gestiona facturas usando"
        administracion -> plataforma.frontend.adminDrivers "Gestiona el equipo usando"
        administracion -> plataforma.frontend.adminDeliveries "Revisa el registro de entregas usando"
        administracion -> plataforma.frontend.adminDashboard "Supervisa el mapa y las métricas usando"
        plataforma.frontend.adminInvoices -> plataforma.frontend.apiClient "Crea, publica y exporta facturas"
        plataforma.frontend.adminDrivers -> plataforma.frontend.apiClient "Administra cuentas"
        plataforma.frontend.adminDeliveries -> plataforma.frontend.apiClient "Consulta el historial paginado"
        plataforma.frontend.adminDashboard -> plataforma.frontend.apiClient "Consulta mapa, métricas, costo por entrega (y horas de soporte) y estado del breaker; simula fallas"
        plataforma.frontend.apiClient -> plataforma.api "Invoca /api/v1/*" "JSON/HTTPS"

        // Componentes de la API: entrada y seguridad
        plataforma.frontend -> plataforma.api.jwtFilter "Invoca /api/v1/* con Bearer JWT" "JSON/HTTPS"
        plataforma.api.jwtFilter -> plataforma.api.jwtAdapter "Valida el token de cada petición"
        plataforma.api.jwtFilter -> plataforma.api.authController "Permite /api/v1/auth/**"
        plataforma.api.jwtFilter -> plataforma.api.driverController "Autoriza /api/v1/driver/** (ADMIN, CONDUCTOR)"
        plataforma.api.jwtFilter -> plataforma.api.adminInvoiceController "Autoriza /api/v1/admin/** (ADMIN)"
        plataforma.api.jwtFilter -> plataforma.api.adminDriverController "Autoriza /api/v1/admin/** (ADMIN)"
        plataforma.api.jwtFilter -> plataforma.api.adminHistoryController "Autoriza /api/v1/admin/** (ADMIN)"
        plataforma.api.jwtFilter -> plataforma.api.adminDashboardController "Autoriza /api/v1/admin/** (ADMIN)"
        plataforma.api.exceptionHandler -> plataforma.api.driverController "Traduce las excepciones de dominio a respuestas HTTP"
        plataforma.api.exceptionHandler -> plataforma.api.adminInvoiceController "Traduce las excepciones de dominio a respuestas HTTP"

        // Controladores -> núcleo
        plataforma.api.authController -> plataforma.api.loginRateLimiter "Verifica el limite de intentos antes de autenticar"
        plataforma.api.authController -> plataforma.api.authService "Autentica"
        plataforma.api.driverController -> plataforma.api.deliveryService "Ejecuta los casos de uso de entrega"
        plataforma.api.adminInvoiceController -> plataforma.api.invoiceMgmtService "Crea, publica y consulta facturas"
        plataforma.api.adminDriverController -> plataforma.api.driverMgmtService "Ejecuta la gestión de equipo"
        plataforma.api.adminHistoryController -> plataforma.api.historyService "Consulta el historial paginado y la evidencia"
        plataforma.api.adminDashboardController -> plataforma.api.historyService "Consulta el mapa y las métricas por rango de fechas"
        plataforma.api.costController -> plataforma.api.costService "Consulta el costo del mes y edita las horas de soporte"

        // Núcleo -> puertos de salida (adaptadores)
        plataforma.api.authService -> plataforma.api.driverRepository "Busca al usuario"
        plataforma.api.authService -> plataforma.api.passwordAdapter "Verifica la contraseña"
        plataforma.api.authService -> plataforma.api.jwtAdapter "Emite el token"
        plataforma.api.deliveryService -> plataforma.api.circuitBreaker "Busca, valida el PIN y confirma a través del breaker"
        plataforma.api.deliveryService -> plataforma.api.retry "Busca facturas pendientes y sus lineas a traves del retry"
        plataforma.api.invoiceMgmtService -> plataforma.api.circuitBreaker "Lista, crea y publica facturas a través del breaker"
        plataforma.api.invoiceMgmtService -> plataforma.api.retry "Lista facturas a traves del retry"
        plataforma.api.retry -> plataforma.api.circuitBreaker "Reintenta hasta 3 veces (backoff 200ms) si la lectura falla"
        plataforma.api.circuitBreaker -> plataforma.api.invoiceAdapter "Delega la llamada protegida (bloqueo pesimista de fila en escrituras)"
        plataforma.api.invoiceAdapter -> plataforma.api.cache "Sirve lineas de factura y ubicacion esperada desde cache antes de tocar la base"
        plataforma.api.resilienceController -> plataforma.api.circuitBreaker "Consulta el estado"
        plataforma.api.resilienceController -> plataforma.api.simulatedFailureToggle "Programa fallas simuladas"
        plataforma.api.simulatedFailureToggle -> plataforma.api.circuitBreaker "Provoca fallas dentro de las llamadas protegidas"
        plataforma.api.costService -> plataforma.api.deliveryLogRepository "Resume entregas confirmadas y bytes de evidencia (SQL)"
        plataforma.api.costService -> plataforma.api.costRatesAdapter "Obtiene las tarifas"
        plataforma.api.costService -> plataforma.api.costInputAdapter "Lee y edita las horas de soporte"
        plataforma.api.jwtFilter -> plataforma.api.costController "Autoriza /api/v1/admin/** (ADMIN)"
        plataforma.api.jwtFilter -> plataforma.api.resilienceController "Autoriza /api/v1/admin/** (ADMIN)"
        plataforma.api.deliveryService -> plataforma.api.deliveryLogRepository "Registra el intento (confirmado, rechazado o incidencia)"
        plataforma.api.historyService -> plataforma.api.deliveryLogRepository "Consulta paginada del historial y métricas"
        plataforma.api.driverMgmtService -> plataforma.api.driverRepository "Crea, actualiza y elimina cuentas"
        plataforma.api.driverMgmtService -> plataforma.api.passwordAdapter "Cifra la contraseña"

        // Adaptadores -> infraestructura y sistemas externos
        plataforma.api.driverRepository -> plataforma.db "Lee y escribe" "JDBC"
        plataforma.api.deliveryLogRepository -> plataforma.db "Lee y escribe" "JDBC"
        plataforma.api.invoiceAdapter -> plataforma.db "Lee y escribe" "JDBC"
        plataforma.api.costInputAdapter -> plataforma.db "Lee y escribe las horas de soporte" "JDBC"

        // Despliegue de producción
        produccion = deploymentEnvironment "Producción" {
            navegador = deploymentNode "Navegador del usuario" "Móvil del conductor o equipo de la administración." "Chrome, Safari, Edge" {
                pwaInstancia = containerInstance plataforma.frontend
            }
            cloudflare = deploymentNode "Cloudflare" "Red perimetral de Cloudflare." "Cloudflare" {
                pages = deploymentNode "Cloudflare Pages" "Aloja el build estático de la PWA; despliegue automático por rama vía integración nativa de Git." "Cloudflare Pages" {
                    sitio = infrastructureNode "Sitio estático de la PWA" "HTML, JS, service worker y assets de OCR; CSP en public/_headers." "CDN de Cloudflare" "Infraestructura"
                }
                proxy = infrastructureNode "DNS y proxy de Cloudflare" "Resuelve el dominio, termina TLS y oculta la IP del servidor." "Cloudflare DNS + Proxy" "Infraestructura"
            }
            aws = deploymentNode "Amazon Web Services" "" "AWS" {
                ec2 = deploymentNode "Instancia EC2" "Elastic IP, solo accesible desde Cloudflare; imágenes versionadas desde GHCR, con pull/backup/rollback automático (deploy.sh)." "Amazon EC2 + Docker Compose" {
                    nginx = infrastructureNode "Nginx de borde" "Termina TLS de origen; expone /healthz y reenvía /api/ al backend." "Docker, nginx 1.30-alpine" "Infraestructura"
                    contBackend = deploymentNode "Contenedor backend" "" "Docker, eclipse-temurin 17" {
                        apiInstancia = containerInstance plataforma.api
                    }
                    contDb = deploymentNode "Contenedor PostgreSQL" "Volumen persistente; puerto no publicado fuera de la máquina." "Docker, postgres:16-alpine" {
                        dbInstancia = containerInstance plataforma.db
                    }
                }
            }
            produccion.navegador.pwaInstancia -> produccion.cloudflare.pages.sitio "Descarga la PWA" "HTTPS"
            produccion.navegador.pwaInstancia -> produccion.cloudflare.proxy "Llama a la API por su dominio" "HTTPS"
            produccion.cloudflare.proxy -> produccion.aws.ec2.nginx "Reenvía a la Elastic IP" "HTTPS"
            produccion.aws.ec2.nginx -> produccion.aws.ec2.contBackend.apiInstancia "proxy_pass /api/" "HTTP"
        }
    }

    views {
        systemContext plataforma "C4-Contexto" {
            include *
            autoLayout lr 300 220
            title "C4 nivel 1 - Contexto"
            description "No hay sistemas externos integrados en el piloto: el ERP se simula con datos propios y el PIN se comunica por un canal operativo."
        }

        container plataforma "C4-Contenedores" {
            include *
            autoLayout lr 300 200
            title "C4 nivel 2 - Contenedores"
            description "Aplicación web en React + TypeScript, API Spring Boot hexagonal y PostgreSQL."
        }


        component plataforma.frontend "C4-Componentes-Frontend" {
            include *
            autoLayout lr 260 180
            title "C4 nivel 3 - Componentes del frontend"
        }

        component plataforma.api "C4-Componentes-API" {
            include *
            autoLayout lr 300 190
            title "C4 nivel 3 - Componentes de la API"
        }


        deployment plataforma produccion "C4-Despliegue" {
            include *
            exclude "produccion.navegador.pwaInstancia -> produccion.aws.ec2.nginx"
            autoLayout lr 250 150
            title "Despliegue de producción"
            description "PWA en Cloudflare Pages; API y PostgreSQL en una instancia EC2 con Elastic IP, detrás de un Nginx de borde y del DNS/proxy de Cloudflare."
        }

        dynamic plataforma "Flujo-Confirmar-Entrega" {
            title "Flujo principal - Confirmar una entrega"
            conductor -> plataforma.frontend "Busca la factura, captura foto y ubicación, e ingresa el PIN dictado por el cliente"
            plataforma.frontend -> plataforma.api "POST /api/v1/driver/deliveries/confirm" "JSON/HTTPS"
            plataforma.api -> plataforma.db "Bloquea la factura (@Lock PESSIMISTIC_WRITE, mismo SELECT ... FOR UPDATE), valida el PIN y guarda la evidencia en la misma transacción" "JDBC"
            autoLayout lr 260 180
        }

        styles {
            element "Person" {
                shape Person
                background #08427B
                color #FFFFFF
            }
            element "Software System" {
                background #1168BD
                color #FFFFFF
            }
            element "Container" {
                background #438DD5
                color #FFFFFF
            }
            element "Component" {
                background #85BBF0
                color #000000
            }
            element "Frontend" {
                shape WebBrowser
            }
            element "Infraestructura" {
                background #F6821F
                color #FFFFFF
            }
            element "Database" {
                shape Cylinder
            }
            element "Resiliencia" {
                background #F4B183
            }
            element "Rendimiento" {
                background #A9D18E
            }
            element "Seguridad" {
                background #F2D7A6
            }
        }
    }

    configuration {
        scope softwaresystem
    }
}
