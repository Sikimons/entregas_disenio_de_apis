workspace "Plataforma de verificación de entregas" "Modelo C4 v1.3 alineado con el código real y con la Fase 1 (v1.1). Parte de la v1.2 y hace visibles los patrones de la Fase 2: arquitectura hexagonal, Circuit Breaker (Resilience4j) sobre el adaptador que simula el ERP, paginación y filtrado, seguridad JWT por rol, transacción con bloqueo pesimista y panel de costo por entrega (showback). Incluye la vista de despliegue de producción (Cloudflare Pages + Cloudflare DNS/proxy + una instancia EC2 con Elastic IP). No incluye elementos no implementados." {

    !identifiers hierarchical
    !impliedRelationships false

    model {
        // Personas
        cliente = person "Cliente" "Recibe el pedido; dicta el PIN al conductor en el momento de la entrega (Fase1 §3)."
        conductor = person "Conductor" "Confirma la entrega con foto, ubicación y PIN; reporta incidencias." "Role.CONDUCTOR"
        administracion = person "Administración" "Gestiona facturas y equipo; supervisa entregas e incidencias." "Role.ADMIN"

        plataforma = softwareSystem "Plataforma de verificación de entregas" "Verifica entregas con PIN, foto y ubicación. React + Spring Boot + PostgreSQL." {

            frontend = container "Aplicación web" "SPA/PWA que se ejecuta en el navegador; el rol autenticado determina las vistas del conductor o de la administración." "React 19 + Vite + vite-plugin-pwa" "Frontend" {
                authContext = component "Contexto de autenticación" "Guarda el token y el usuario (localStorage/sessionStorage)." "React Context"
                apiClient = component "Cliente HTTP" "Axios con token Bearer; cierra sesión en 401; baseURL fija /api/v1 (pendiente pasar a variable de entorno de Vite)." "Axios"
                loginPage = component "Inicio de sesión" "Formulario de acceso; redirige por rol." "React Component"
                driverHome = component "Pantalla del conductor" "Busca la factura, captura foto/GPS y confirma con PIN o reporta incidencia." "React Component"
                driverHistory = component "Historial del conductor" "Historial propio paginado y evidencia." "React Component"
                adminInvoices = component "Gestión de facturas" "Crea y publica facturas (genera el PIN); exporta a CSV." "React Component"
                adminDrivers = component "Gestión de equipo" "CRUD de cuentas de conductores y administradores." "React Component"
                adminDeliveries = component "Registro de entregas" "Lista paginada de entregas, ubicación y evidencia." "React Component"
                adminDashboard = component "Tablero administrativo" "Mapa y métricas por conductor filtrados por fechas; costo por entrega y estado del Circuit Breaker." "React Component"
            }

            api = container "API de verificación" "API REST con arquitectura hexagonal: autenticación, facturas, entregas, historial, costo por entrega y resiliencia. Rutas agrupadas por rol bajo /api/v1 (/api/v1/driver y /api/v1/admin); contrato documentado con OpenAPI (springdoc)." "Java 17 + Spring Boot 3.3" "Backend" {
                jwtFilter = component "Filtro JWT" "Seguridad sin estado: valida el JWT y aplica el rol de cada ruta. Deja publicos /api/v1/auth, /v3/api-docs y /swagger-ui." "Spring Security Filter" "Seguridad"
                exceptionHandler = component "Manejador global de excepciones" "Manejo centralizado de errores: formato unico de mensaje; 409 (entrega ya confirmada), 422 (PIN invalido), 503 (circuito abierto) y 400 para DeliveryRejectedException generica." "Spring RestControllerAdvice"

                authController = component "Controlador de autenticación" "POST /api/v1/auth/login (público)." "Spring REST Controller"
                driverController = component "Controlador del conductor" "/api/v1/driver: búsqueda limitada, confirmación, incidencias e historial paginado. Acepta CONDUCTOR y ADMIN." "Spring REST Controller"
                adminInvoiceController = component "Controlador de facturas" "/api/v1/admin/invoices: crea, publica y consulta facturas y PIN." "Spring REST Controller"
                adminDriverController = component "Controlador de equipo" "/api/v1/admin/users: CRUD de cuentas." "Spring REST Controller"
                adminHistoryController = component "Controlador de historial" "/api/v1/admin/deliveries: historial paginado (page, size) y evidencia." "Spring REST Controller"
                adminDashboardController = component "Controlador del tablero" "/api/v1/admin/dashboard: mapa y métricas filtrados por from/to." "Spring REST Controller"
                costController = component "Controlador de costos" "GET /api/v1/admin/cost?month=yyyy-MM y PUT /support-hours: costo por entrega verificada (showback, Fase1 §4.5) con entregas y bytes de evidencia calculados en SQL, mas tarifas y horas de soporte." "Spring REST Controller"
                resilienceController = component "Controlador de resiliencia" "GET /api/v1/admin/resilience/status y POST /simulate-failures {count 1-50}, sin ruta para desactivar la simulacion." "Spring REST Controller"

                authService = component "Servicio de autenticación" "Valida credenciales y emite el JWT." "Spring Service" "Nucleo"
                deliveryService = component "Servicio de entregas" "Confirma entregas en una única transacción con bloqueo pesimista (SELECT ... FOR UPDATE) y registra el intento." "Spring Service" "Nucleo"
                historyService = component "Servicio de historial y métricas" "Paginación y filtrado del historial (PageRequest/PageResult); métricas por conductor." "Spring Service" "Nucleo"
                driverMgmtService = component "Servicio de gestión de equipo" "Crea, actualiza y elimina cuentas." "Spring Service" "Nucleo"
                invoiceMgmtService = component "Servicio de gestión de facturas" "InvoiceManagementApplicationService: lista, crea y publica facturas a traves del puerto InvoiceAdminPort." "Spring Service" "Nucleo"
                costService = component "Servicio de costo por entrega" "OperationalCostApplicationService: combina el resumen de entregas confirmadas con tarifas y horas de soporte del mes." "Spring Service" "Nucleo"

                invoiceAdapter = component "Adaptador de facturas" "Implementa InvoiceQueryPort, DeliveryConfirmationGatewayPort e InvoiceAdminPort: persiste facturas, simula el ERP (Fase1 §3) y controla los intentos del PIN (5 intentos, bloqueo 5 min). Ejecuta sus llamadas a traves del Circuit Breaker." "Spring JdbcTemplate" "Adaptador"
                deliveryLogRepository = component "Repositorio de entregas" "Puerto DeliveryAttemptRepositoryPort: log de entregas y evidencia, con consultas paginadas y resumen SQL de costo (sin cargar fotos)." "Spring Data JPA" "Adaptador"
                driverRepository = component "Repositorio de usuarios" "Puerto DriverRepositoryPort: conductores y administradores." "Spring Data JPA" "Adaptador"
                jwtAdapter = component "Adaptador de tokens" "Puerto TokenProviderPort: firma y valida JWT (HMAC-SHA256)." "io.jsonwebtoken" "Adaptador"
                passwordAdapter = component "Adaptador de contraseñas" "Puerto PasswordEncoderPort: delega en BCrypt." "Spring Security" "Adaptador"
                costRatesAdapter = component "Adaptador de tarifas" "Puerto CostRatesPort: tarifas de infraestructura, almacenamiento y horas de soporte (APP_COST_*), sin tarifa de envio de PIN." "Spring Config" "Adaptador"
                costInputAdapter = component "Adaptador de horas de soporte" "Puerto OperationalCostInputPort: guarda y edita las horas de soporte por mes." "Spring JdbcTemplate" "Adaptador"
                circuitBreaker = component "Circuit Breaker del ERP" "Breaker 'erpGateway', construido a mano en ResilienceConfig; aplicado dentro del adaptador de facturas (protectedCall), cubre consulta, confirmacion y gestion de facturas. Excluye DeliveryRejectedException (incluye PIN incorrecto y entrega ya confirmada). Umbrales: 50% de fallos, ventana de 10, minimo 5 llamadas, 15s abierto, 3 llamadas en semiabierto, transicion automatica. Circuito abierto -> 503." "Resilience4j" "Resiliencia"
                simulatedFailureToggle = component "Simulador de fallas del ERP" "SimulatedErpFailureToggle: contador atomico que fuerza fallas dentro del breaker para demostrar el ciclo cerrado/abierto/semiabierto." "Java" "Resiliencia"
            }

            db = container "Base de datos" "Facturas (PIN en texto plano, sin expiracion), usuarios, historial de entregas con evidencia y horas de soporte por mes." "PostgreSQL 16" "Database"
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
        plataforma.api.invoiceMgmtService -> plataforma.api.circuitBreaker "Lista, crea y publica facturas a través del breaker"
        plataforma.api.circuitBreaker -> plataforma.api.invoiceAdapter "Delega la llamada protegida (bloqueo SELECT ... FOR UPDATE)"
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
                pages = deploymentNode "Cloudflare Pages" "Aloja y distribuye los archivos estáticos de la PWA (build de Vite)." "Cloudflare Pages" {
                    sitio = infrastructureNode "Sitio estático de la PWA" "HTML, JS, service worker y assets de OCR." "CDN de Cloudflare" "Infraestructura"
                }
                proxy = infrastructureNode "DNS y proxy de Cloudflare" "Resuelve el dominio de la API, termina TLS, oculta la IP del servidor y admite reglas de limitación de tasa." "Cloudflare DNS + Proxy" "Infraestructura"
            }
            aws = deploymentNode "Amazon Web Services" "" "AWS" {
                ec2 = deploymentNode "Instancia EC2" "Una sola máquina con Elastic IP; el grupo de seguridad solo admite tráfico desde los rangos de Cloudflare." "Amazon EC2 + Docker Compose" {
                    contBackend = deploymentNode "Contenedor backend" "" "Docker, eclipse-temurin 17" {
                        apiInstancia = containerInstance plataforma.api
                    }
                    contDb = deploymentNode "Contenedor PostgreSQL" "Volumen persistente; puerto no publicado fuera de la máquina." "Docker, postgres:16-alpine" {
                        dbInstancia = containerInstance plataforma.db
                    }
                }
            }
            produccion.navegador -> produccion.cloudflare.pages.sitio "Descarga la PWA" "HTTPS"
            produccion.navegador.pwaInstancia -> produccion.cloudflare.proxy "Llama a la API por su dominio" "HTTPS"
            produccion.cloudflare.proxy -> produccion.aws.ec2.contBackend.apiInstancia "Reenvía a la Elastic IP" "HTTPS"
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
            description "Aplicación web en React, API Spring Boot hexagonal y PostgreSQL."
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
            exclude "produccion.navegador.pwaInstancia -> produccion.aws.ec2.contBackend.apiInstancia"
            autoLayout lr 250 150
            title "Despliegue de producción"
            description "PWA en Cloudflare Pages; API y PostgreSQL en una instancia EC2 con Elastic IP, detrás del DNS y proxy de Cloudflare."
        }

        dynamic plataforma "Flujo-Confirmar-Entrega" {
            title "Flujo principal - Confirmar una entrega"
            conductor -> plataforma.frontend "Busca la factura, captura foto y ubicación, e ingresa el PIN dictado por el cliente"
            plataforma.frontend -> plataforma.api "POST /api/v1/driver/deliveries/confirm" "JSON/HTTPS"
            plataforma.api -> plataforma.db "Bloquea la factura (SELECT ... FOR UPDATE), valida el PIN y guarda la evidencia en la misma transacción" "JDBC"
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
            element "Seguridad" {
                background #F2D7A6
            }
        }
    }

    configuration {
        scope softwaresystem
    }
}
