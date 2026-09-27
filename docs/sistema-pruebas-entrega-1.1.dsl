workspace "Plataforma de verificación de entregas" "Modelo C4 de una plataforma interna para verificar entregas mediante PIN, fotografía y ubicación." {

    !identifiers hierarchical
    !impliedRelationships false

    model {
        // ---------------------------------------------------------------------
        // Personas
        // ---------------------------------------------------------------------
        cliente = person "Cliente" "Recibe el pedido y participa en la verificación mediante el PIN."
        conductor = person "Conductor" "Realiza la entrega y registra la evidencia en campo."
        operador = person "Operador administrativo" "Supervisa entregas, consulta evidencias y gestiona incidencias."

        // ---------------------------------------------------------------------
        // Sistema en estudio
        // ---------------------------------------------------------------------
        plataforma = softwareSystem "Plataforma de verificación de entregas" "Verifica entregas mediante PIN, fotografía y ubicación; conserva la evidencia y sincroniza el estado con sistemas externos." {

            appConductor = container "Aplicación del conductor" "Permite buscar una entrega, capturar evidencias y confirmar la entrega, incluso con conectividad intermitente." "Angular PWA" "Frontend" {
                entregaUi = component "Interfaz de entrega" "Orquesta la interacción del conductor para consultar y confirmar una entrega." "Angular Component" "UI"
                capturaEvidencia = component "Captura de evidencia" "Obtiene y prepara la fotografía y la ubicación del dispositivo." "Angular Service" "Application"
                validadorLocal = component "Validador local" "Realiza validaciones básicas de la evidencia antes de enviarla." "Angular Service" "Application"
                sincronizadorOffline = component "Sincronizador offline" "Persiste solicitudes pendientes y las reintenta cuando se recupera la conectividad." "Angular Service + IndexedDB" "Application"
                apiConductor = component "Cliente de API" "Encapsula la comunicación HTTPS con la API de verificación." "Angular HttpClient" "Adapter"
            }

            panelAdmin = container "Panel administrativo" "Permite seguimiento operativo, consulta histórica, gestión de incidencias y visualización de métricas." "Angular SPA" "Frontend" {
                seguimientoUi = component "Seguimiento de entregas" "Presenta el estado actualizado de las entregas." "Angular Component" "UI"
                historialUi = component "Historial y evidencias" "Permite consultar entregas finalizadas y su evidencia asociada." "Angular Component" "UI"
                incidenciasUi = component "Gestión de incidencias" "Permite registrar incidencias y solicitar el reenvío del PIN." "Angular Component" "UI"
                metricasUi = component "Métricas operativas" "Presenta indicadores de operación y costos." "Angular Component" "UI"
                apiAdmin = component "Cliente de API" "Encapsula las operaciones REST del panel administrativo." "Angular HttpClient" "Adapter"
                eventosAdmin = component "Cliente de eventos" "Mantiene la suscripción a actualizaciones de estado en tiempo real." "EventSource / SSE" "Adapter"
            }

            api = container "API de verificación" "Implementa los casos de uso de verificación de entregas, PIN, evidencias, incidencias e integración con sistemas externos." "Java + Spring Boot" "Backend" {
                restApi = component "API REST" "Expone las operaciones requeridas por la aplicación del conductor y el panel administrativo." "Spring MVC / REST Controller" "InboundAdapter"
                entregaService = component "Servicio de aplicación de entregas" "Coordina la verificación, persistencia y confirmación de una entrega." "Spring Service" "Application"
                pinService = component "Servicio de PIN" "Genera, reenvía y valida PIN de entrega sin exponer su valor persistido." "Spring Service" "Application"
                evidenciaService = component "Validador de evidencia" "Aplica las reglas de validación de fotografía y ubicación." "Spring Service" "DomainService"
                eventosSse = component "Publicador de eventos" "Publica cambios de estado para los clientes administrativos suscritos." "Spring SseEmitter" "OutboundAdapter"
                entregaRepository = component "Repositorio de entregas" "Abstrae la persistencia de entregas, PIN, evidencias, incidencias y auditoría." "Spring Data JPA" "OutboundAdapter"
                erpGateway = component "Adaptador ERP" "Encapsula el contrato con el ERP para consultar comprobantes y reportar entregas." "Spring Component" "OutboundAdapter"
                mensajeriaGateway = component "Adaptador de mensajería" "Encapsula el proveedor utilizado para enviar el PIN al cliente." "Spring Component" "OutboundAdapter"
                geocodingGateway = component "Adaptador de geocodificación" "Centraliza la geocodificación y la estrategia principal/respaldo." "Spring Component" "OutboundAdapter"
            }

            db = container "Base de datos operacional" "Almacena entregas, PIN hasheado, referencias de evidencias, incidencias y auditoría." "PostgreSQL" "Database"
        }

        // ---------------------------------------------------------------------
        // Sistemas externos
        // ---------------------------------------------------------------------
        erp = softwareSystem "ERP" "Sistema corporativo que origina los comprobantes y recibe la confirmación de la entrega." "External"
        mensajeria = softwareSystem "Proveedor de mensajería" "Entrega el PIN al cliente mediante los canales habilitados." "External"
        osm = softwareSystem "Servicio de geocodificación OSM" "Servicio principal utilizado para geocodificación." "External"
        googleMaps = softwareSystem "Google Maps" "Servicio alternativo de geocodificación utilizado como respaldo." "External"

        // =====================================================================
        // C4 Nivel 1 - Relaciones de contexto (sin protocolos/tecnologías)
        // =====================================================================
        plataforma -> cliente "Envía un PIN de verificación para autorizar la entrega"
        conductor -> plataforma "Registra la entrega y sus evidencias"
        operador -> plataforma "Supervisa entregas y gestiona incidencias"
        plataforma -> erp "Consulta comprobantes y reporta entregas confirmadas"
        plataforma -> mensajeria "Solicita el envío de PIN"
        plataforma -> osm "Solicita geocodificación principal"
        plataforma -> googleMaps "Solicita geocodificación de respaldo"
        mensajeria -> cliente "Entrega el PIN de verificación"
        cliente -> conductor "Comunica el PIN al momento de la entrega"

        // =====================================================================
        // C4 Nivel 2 - Relaciones entre personas, contenedores y sistemas
        // =====================================================================
        conductor -> plataforma.appConductor "Registra y confirma entregas usando"
        operador -> plataforma.panelAdmin "Supervisa la operación usando"

        plataforma.appConductor -> plataforma.api "Consulta y confirma entregas" "HTTPS/JSON"
        plataforma.panelAdmin -> plataforma.api "Consulta y administra entregas" "HTTPS/JSON"
        plataforma.panelAdmin -> plataforma.api "Se suscribe a cambios de estado" "SSE/HTTPS"

        plataforma.api -> plataforma.db "Lee y persiste información operacional" "JDBC"
        plataforma.api -> erp "Consulta comprobantes y reporta entregas" "API del ERP"
        plataforma.api -> mensajeria "Solicita el envío de PIN" "HTTPS/JSON"
        plataforma.api -> osm "Solicita geocodificación principal" "HTTPS"
        plataforma.api -> googleMaps "Solicita geocodificación de respaldo" "HTTPS"

        // =====================================================================
        // C4 Nivel 3 - Componentes de la aplicación del conductor
        // =====================================================================
        conductor -> plataforma.appConductor.entregaUi "Opera la entrega usando"
        plataforma.appConductor.entregaUi -> plataforma.appConductor.capturaEvidencia "Solicita capturar fotografía y ubicación"
        plataforma.appConductor.entregaUi -> plataforma.appConductor.validadorLocal "Solicita validación previa"
        plataforma.appConductor.entregaUi -> plataforma.appConductor.apiConductor "Consulta y confirma la entrega"
        plataforma.appConductor.entregaUi -> plataforma.appConductor.sincronizadorOffline "Encola la confirmación cuando no hay conexión"
        plataforma.appConductor.sincronizadorOffline -> plataforma.appConductor.apiConductor "Reintenta solicitudes pendientes"
        plataforma.appConductor.apiConductor -> plataforma.api "Consume operaciones de verificación" "HTTPS/JSON"

        // =====================================================================
        // C4 Nivel 3 - Componentes del panel administrativo
        // =====================================================================
        operador -> plataforma.panelAdmin.seguimientoUi "Monitorea entregas usando"
        operador -> plataforma.panelAdmin.historialUi "Consulta evidencias usando"
        operador -> plataforma.panelAdmin.incidenciasUi "Gestiona incidencias usando"
        operador -> plataforma.panelAdmin.metricasUi "Consulta indicadores usando"

        plataforma.panelAdmin.seguimientoUi -> plataforma.panelAdmin.eventosAdmin "Solicita actualizaciones en tiempo real"
        plataforma.panelAdmin.historialUi -> plataforma.panelAdmin.apiAdmin "Consulta entregas y evidencias"
        plataforma.panelAdmin.incidenciasUi -> plataforma.panelAdmin.apiAdmin "Registra incidencias y solicita reenvío de PIN"
        plataforma.panelAdmin.metricasUi -> plataforma.panelAdmin.apiAdmin "Consulta métricas"
        plataforma.panelAdmin.apiAdmin -> plataforma.api "Consume operaciones administrativas" "HTTPS/JSON"
        plataforma.panelAdmin.eventosAdmin -> plataforma.api "Se suscribe a actualizaciones" "SSE/HTTPS"

        // =====================================================================
        // C4 Nivel 3 - Componentes de la API
        // =====================================================================
        plataforma.api.restApi -> plataforma.api.entregaService "Ejecuta casos de uso de entrega"
        plataforma.api.restApi -> plataforma.api.pinService "Ejecuta casos de uso de PIN"

        plataforma.api.entregaService -> plataforma.api.evidenciaService "Valida la evidencia recibida"
        plataforma.api.entregaService -> plataforma.api.pinService "Valida el PIN de la entrega"
        plataforma.api.entregaService -> plataforma.api.entregaRepository "Consulta y persiste la entrega"
        plataforma.api.entregaService -> plataforma.api.erpGateway "Consulta el comprobante y reporta la entrega"
        plataforma.api.entregaService -> plataforma.api.geocodingGateway "Normaliza o valida la ubicación"
        plataforma.api.entregaService -> plataforma.api.eventosSse "Publica el cambio de estado"

        plataforma.api.pinService -> plataforma.api.entregaRepository "Consulta y persiste datos del PIN"
        plataforma.api.pinService -> plataforma.api.mensajeriaGateway "Solicita el envío del PIN"

        plataforma.api.entregaRepository -> plataforma.db "Lee y escribe" "JDBC"
        plataforma.api.erpGateway -> erp "Consume el contrato del ERP" "API del ERP"
        plataforma.api.mensajeriaGateway -> mensajeria "Solicita envío de mensajes" "HTTPS/JSON"
        plataforma.api.geocodingGateway -> osm "Geocodifica mediante el proveedor principal" "HTTPS"
        plataforma.api.geocodingGateway -> googleMaps "Geocodifica mediante el proveedor de respaldo" "HTTPS"
    }

    views {
        // ---------------------------------------------------------------------
        // C4 Nivel 1
        // ---------------------------------------------------------------------
        systemContext plataforma "C4-Contexto" {
            include *?
            autoLayout lr 300 220
            title "C4 nivel 1 - Contexto del sistema"
            description "Personas y sistemas externos que interactúan con la Plataforma de verificación de entregas."
        }

        // ---------------------------------------------------------------------
        // C4 Nivel 2
        // ---------------------------------------------------------------------
        container plataforma "C4-Contenedores" {
            include *?
            autoLayout lr 320 220
            title "C4 nivel 2 - Contenedores"
            description "Arquitectura de alto nivel: dos clientes Angular, una API Spring Boot y PostgreSQL, junto con dependencias externas."
        }

        // ---------------------------------------------------------------------
        // C4 Nivel 3
        // ---------------------------------------------------------------------
        component plataforma.appConductor "C4-Componentes-AppConductor" {
            include *
            autoLayout lr 260 180
            title "C4 nivel 3 - Componentes de la aplicación del conductor"
            description "Responsabilidades de presentación, captura, validación local, sincronización offline y comunicación con la API."
        }

        component plataforma.panelAdmin "C4-Componentes-PanelAdmin" {
            include *
            autoLayout lr 260 180
            title "C4 nivel 3 - Componentes del panel administrativo"
            description "Componentes de seguimiento, historial, incidencias, métricas y comunicación con la API."
        }

        component plataforma.api "C4-Componentes-API" {
            include *
            autoLayout lr 300 190
            title "C4 nivel 3 - Componentes de la API de verificación"
            description "Casos de uso desacoplados de infraestructura mediante repositorios y adaptadores externos."
        }

        // ---------------------------------------------------------------------
        // Vista dinámica de apoyo: flujo principal
        // ---------------------------------------------------------------------
        dynamic plataforma "Flujo-Entrega-Verificada" {
            title "Flujo principal - Entrega verificada"
            plataforma.api -> mensajeria "Solicita enviar el PIN" "HTTPS/JSON"
            mensajeria -> cliente "Entrega el PIN por el canal configurado"
            cliente -> conductor "Comunica el PIN al recibir el pedido"
            conductor -> plataforma.appConductor "Registra PIN, fotografía y ubicación"
            plataforma.appConductor -> plataforma.api "Solicita confirmar la entrega" "HTTPS/JSON"
            plataforma.api -> plataforma.db "Consulta datos y persiste evidencia" "JDBC"
            plataforma.api -> erp "Reporta la entrega confirmada" "API del ERP"
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

            element "External" {
                background #999999
                color #FFFFFF
                border dashed
            }

            element "Frontend" {
                shape WebBrowser
            }

            element "Database" {
                shape Cylinder
            }

            element "InboundAdapter" {
                background #78B7E8
            }

            element "OutboundAdapter" {
                background #A5CBEF
            }

            element "Application" {
                background #85BBF0
            }

            element "DomainService" {
                background #B8D8F5
            }

            relationship "Relationship" {
                color #707070
                style solid
                routing Orthogonal
                fontSize 22
            }
        }
    }

    configuration {
        scope softwaresystem
    }
}
