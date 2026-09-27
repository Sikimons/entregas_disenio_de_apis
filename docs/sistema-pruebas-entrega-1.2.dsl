workspace "Plataforma de verificación de entregas" "Modelo C4 alineado con el código real del repositorio (rama evalution) y con el documento de visión aprobado (Fase1_Vision_Producto_Modelo_Negocio_API_Final.md, v1.1). Sustituye a sistema-pruebas-entrega-1.0.dsl y -1.1.dsl, que documentaban una arquitectura aspiracional (Angular, SSE, mensajería, geocodificación externa, cola offline) no implementada." {

    !identifiers hierarchical
    !impliedRelationships false

    model {
        // Personas (terminología alineada con Fase1 §1-§3)
        cliente = person "Cliente" "Recibe el pedido y dicta el PIN al conductor."
        conductor = person "Conductor" "Confirma la entrega con foto, ubicación y PIN; reporta incidencias." "Role.CONDUCTOR"
        administracion = person "Administración" "Gestiona facturas y equipo; supervisa entregas e incidencias." "Role.ADMIN"

        plataforma = softwareSystem "Plataforma de verificación de entregas" "Verifica entregas con PIN, foto y ubicación. React + Spring Boot + PostgreSQL." {

            frontend = container "Aplicación web" "SPA/PWA para conductor y administración, según el rol autenticado." "React 19 + Vite + vite-plugin-pwa" "Frontend" {
                authContext = component "Contexto de autenticación" "Guarda el token y el usuario (localStorage/sessionStorage)." "React Context" "frontend/src/context/AuthContext.jsx"
                apiClient = component "Cliente HTTP" "Axios con token Bearer; cierra sesión en 401." "Axios" "frontend/src/api/client.js"
                loginPage = component "Inicio de sesión" "Formulario de acceso; redirige por rol." "React Component" "frontend/src/pages/LoginPage.jsx"
                driverHome = component "Pantalla del conductor" "Busca la factura, captura foto/GPS y confirma con PIN o reporta incidencia." "React Component" "frontend/src/pages/driver/DriverHomePage.jsx"
                driverHistory = component "Historial del conductor" "Historial propio y evidencia." "React Component" "frontend/src/pages/driver/DriverHistoryPage.jsx"
                adminInvoices = component "Gestión de facturas" "Crea y publica facturas (genera el PIN); exporta a CSV." "React Component" "frontend/src/pages/admin/AdminInvoicesPage.jsx"
                adminDrivers = component "Gestión de equipo" "CRUD de cuentas de conductores y administradores." "React Component" "frontend/src/pages/admin/AdminDriversPage.jsx"
                adminDeliveries = component "Registro de entregas" "Lista paginada de entregas, ubicación y evidencia." "React Component" "frontend/src/pages/admin/AdminDeliveriesPage.jsx"
                adminDashboard = component "Tablero administrativo" "Mapa y métricas de entregas por conductor." "React Component" "frontend/src/pages/admin/AdminDashboardPage.jsx"
            }

            api = container "API de verificación" "API REST con arquitectura hexagonal: autenticación, facturas, entregas e historial." "Java 17 + Spring Boot 3.3" "Backend" {
                authController = component "Controlador de autenticación" "POST /api/auth/login." "Spring REST Controller" "AuthController"
                driverController = component "Controlador del conductor" "Búsqueda, confirmación, incidencias e historial del conductor." "Spring REST Controller" "DriverDeliveryController"
                adminInvoiceController = component "Controlador de facturas" "Crea, publica y consulta facturas y sus PIN." "Spring REST Controller" "AdminInvoiceController"
                adminDriverController = component "Controlador de equipo" "CRUD de cuentas de conductores/administradores." "Spring REST Controller" "AdminDriverController"
                adminHistoryController = component "Controlador de historial" "Historial paginado y evidencia." "Spring REST Controller" "AdminDeliveryHistoryController"
                adminDashboardController = component "Controlador del tablero" "Datos agregados para el mapa y las métricas." "Spring REST Controller" "AdminDashboardController"
                exceptionHandler = component "Manejador global de excepciones" "Traduce excepciones de dominio a códigos HTTP." "Spring RestControllerAdvice" "GlobalExceptionHandler"
                jwtFilter = component "Filtro JWT" "Valida el JWT en cada petición." "Spring Security Filter" "JwtAuthenticationFilter"

                authService = component "Servicio de autenticación" "Valida credenciales y emite el token JWT." "Spring Service" "AuthApplicationService"
                deliveryService = component "Servicio de entregas" "Confirma entregas y registra incidencias, en una transacción." "Spring Service" "DeliveryApplicationService"
                historyService = component "Servicio de historial y métricas" "Pagina el historial y agrega métricas por conductor." "Spring Service" "DeliveryHistoryApplicationService"
                driverMgmtService = component "Servicio de gestión de equipo" "Crea, actualiza y elimina cuentas." "Spring Service" "DriverManagementApplicationService"

                driverRepository = component "Repositorio de usuarios" "Persiste conductores y administradores." "Spring Data JPA" "DriverRepositoryAdapter"
                deliveryLogRepository = component "Repositorio de entregas" "Persiste el log de entregas y evidencia." "Spring Data JPA" "DeliveryAttemptRepositoryAdapter"
                invoiceAdapter = component "Adaptador de facturas" "Persiste facturas; simula el ERP (Fase1 §3)." "Spring JdbcTemplate" "LocalInvoiceAdapter"
                jwtAdapter = component "Adaptador de tokens" "Firma y valida JWT (HMAC-SHA256)." "io.jsonwebtoken" "JwtTokenProviderAdapter"
                passwordAdapter = component "Adaptador de contraseñas" "Delega en BCrypt." "Spring Security" "SpringPasswordEncoderAdapter"
            }

            db = container "Base de datos" "Facturas, usuarios e historial de entregas. PIN en texto plano, sin límite de intentos." "PostgreSQL 16" "Database"
        }

        // Nota de alcance (Fase1 §3): en este piloto la integración con el ERP vigente se simula con datos
        // propios (LocalInvoiceAdapter); la distribución del PIN al cliente ocurre por un canal operativo
        // existente fuera de esta plataforma (la administración exporta el PIN vigente a CSV y lo comunica
        // al cliente). No hay envío automático de PIN, eventos en tiempo real (SSE), geocodificación externa
        // ni sincronización offline en el código actual: esas piezas no forman parte de este modelo.

        cliente -> conductor "Dicta el PIN al recibir el pedido"
        conductor -> plataforma "Busca la factura, confirma la entrega o reporta una incidencia"
        administracion -> plataforma "Gestiona facturas y equipo; supervisa entregas e incidencias"
        administracion -> cliente "Comunica el PIN vigente por el canal operativo disponible (fuera del alcance automatizado de esta plataforma)"

        conductor -> plataforma.frontend "Usa, autenticado con su cuenta de conductor"
        administracion -> plataforma.frontend "Usa, autenticado con su cuenta de administrador"
        plataforma.frontend -> plataforma.api "Consume la API REST" "JSON/HTTPS"
        plataforma.api -> plataforma.db "Lee y escribe" "JDBC"

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
        plataforma.frontend.adminDashboard -> plataforma.frontend.apiClient "Consulta el mapa y las métricas"

        plataforma.frontend.apiClient -> plataforma.api "Invoca" "JSON/HTTPS"

        plataforma.api.authController -> plataforma.api.authService "Autentica"
        plataforma.api.driverController -> plataforma.api.deliveryService "Ejecuta los casos de uso de entrega"
        plataforma.api.adminInvoiceController -> plataforma.api.invoiceAdapter "Crea, publica y consulta facturas"
        plataforma.api.adminDriverController -> plataforma.api.driverMgmtService "Ejecuta la gestión de equipo"
        plataforma.api.adminHistoryController -> plataforma.api.historyService "Consulta el historial y la evidencia"
        plataforma.api.adminDashboardController -> plataforma.api.historyService "Consulta el mapa y las métricas"

        plataforma.api.authService -> plataforma.api.driverRepository "Busca al usuario"
        plataforma.api.authService -> plataforma.api.passwordAdapter "Verifica la contraseña"
        plataforma.api.authService -> plataforma.api.jwtAdapter "Emite el token"

        plataforma.api.deliveryService -> plataforma.api.invoiceAdapter "Busca la factura, valida el PIN y confirma la entrega"
        plataforma.api.deliveryService -> plataforma.api.deliveryLogRepository "Registra el intento de entrega (confirmado, rechazado o incidencia)"

        plataforma.api.historyService -> plataforma.api.deliveryLogRepository "Consulta el historial, la evidencia y las métricas"
        plataforma.api.driverMgmtService -> plataforma.api.driverRepository "Crea, actualiza y elimina cuentas"
        plataforma.api.driverMgmtService -> plataforma.api.passwordAdapter "Cifra la contraseña"

        plataforma.api.jwtFilter -> plataforma.api.jwtAdapter "Valida el token de cada petición"
        plataforma.api.exceptionHandler -> plataforma.api.driverController "Traduce las excepciones de dominio a respuestas HTTP"

        plataforma.api.driverRepository -> plataforma.db "Lee y escribe" "JDBC"
        plataforma.api.deliveryLogRepository -> plataforma.db "Lee y escribe" "JDBC"
        plataforma.api.invoiceAdapter -> plataforma.db "Lee y escribe" "JDBC"
    }

    views {
        systemContext plataforma "C4-Contexto" {
            include *
            autoLayout lr 300 220
            title "C4 nivel 1 - Contexto (según el código real del repositorio)"
            description "Personas que interactúan con la plataforma. No hay sistemas externos integrados en esta iteración: el ERP se simula con datos propios (Fase1 §3) y la distribución del PIN es manual."
        }

        container plataforma "C4-Contenedores" {
            include *
            autoLayout lr 320 220
            title "C4 nivel 2 - Contenedores"
            description "Una SPA/PWA en React sirve ambos roles; una API Spring Boot con arquitectura hexagonal; una base PostgreSQL."
        }

        component plataforma.frontend "C4-Componentes-Frontend" {
            include *
            autoLayout lr 260 180
            title "C4 nivel 3 - Componentes del frontend"
            description "Páginas y servicios reales de frontend/src, organizados por rol."
        }

        component plataforma.api "C4-Componentes-API" {
            include *
            autoLayout lr 300 190
            title "C4 nivel 3 - Componentes de la API"
            description "Controladores, servicios de aplicación y adaptadores reales del backend."
        }

        dynamic plataforma "Flujo-Confirmar-Entrega" {
            title "Flujo principal - Confirmar una entrega (código real)"
            conductor -> plataforma.frontend "Busca la factura, captura foto y ubicación, e ingresa el PIN dictado por el cliente"
            plataforma.frontend -> plataforma.api "GET /api/driver/invoices y POST /api/driver/deliveries/confirm" "JSON/HTTPS"
            plataforma.api -> plataforma.db "Bloquea la factura (SELECT ... FOR UPDATE), valida el PIN y guarda la evidencia y el resultado, en la misma transacción" "JDBC"
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
            element "Database" {
                shape Cylinder
            }
        }
    }

    configuration {
        scope softwaresystem
    }
}
