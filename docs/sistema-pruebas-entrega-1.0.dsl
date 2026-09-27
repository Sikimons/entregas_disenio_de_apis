workspace "Plataforma de verificación de entregas" "API interna para verificar entregas a domicilio con PIN, foto y ubicación." {

    !identifiers hierarchical

    model {
        # Personas
        cliente = person "Cliente" "Recibe el pedido y el PIN."
        conductor = person "Conductor" "Registra la entrega en campo."
        administracion = person "Administración" "Supervisa entregas y resuelve reclamos."

        # Sistema en estudio
        plataforma = softwareSystem "Plataforma de verificación de entregas" "Verifica entregas con PIN, foto y ubicación y custodia la evidencia." {

            pwa = container "Aplicación del conductor" "Registra entregas en campo." "Angular, PWA" "Web Browser" {
                moduloEntregas = component "Módulo de entregas" "Busca comprobantes, pide el PIN y registra el estado." "Angular Component"
                captura = component "Captura de evidencias" "Toma y comprime la foto; obtiene la ubicación." "Angular Service"
                validadorLocal = component "Validador local" "Valida foto y ubicación sin conexión." "Angular Service"
                colaOffline = component "Cola offline" "Guarda envíos sin conexión y los reintenta." "Angular Service, IndexedDB"
                clienteApi = component "Cliente API" "Llama a la API REST." "Angular HttpClient"
            }

            panel = container "Panel administrativo" "Seguimiento, historial, métricas e incidencias." "Angular" "Web Browser" {
                seguimiento = component "Seguimiento" "Muestra entregas en vivo." "Angular Component"
                historial = component "Historial" "Consulta entregas y evidencias." "Angular Component"
                incidencias = component "Incidencias" "Gestiona reclamos y reenvía el PIN." "Angular Component"
                metricas = component "Métricas" "Muestra indicadores y costo por entrega." "Angular Component"
                clienteApi = component "Cliente API" "Llama a la API REST." "Angular HttpClient"
                clienteEventos = component "Cliente de eventos" "Recibe eventos en vivo." "Angular Service, EventSource"
            }

            api = container "API de verificación" "Genera y valida el PIN, valida foto y ubicación y custodia la evidencia." "Java, Spring Boot" {
                controlador = component "Controlador REST" "Endpoints del conductor y del panel." "Spring REST Controller"
                publicador = component "Publicador de eventos" "Emite cambios de estado en vivo." "Spring SseEmitter"
                servicioEntregas = component "Servicio de entregas" "Valida foto y ubicación y registra la entrega." "Spring Service"
                servicioPin = component "Servicio de PIN" "Genera, envía y valida el PIN." "Spring Service"
                adaptadorErp = component "Adaptador ERP" "Detecta comprobantes confirmados e informa entregas." "Spring Component"
                repositorio = component "Repositorio" "Persiste entregas y evidencias." "Spring Data JPA"
            }

            db = container "Base de datos" "Entregas, PIN hasheado, evidencias, incidencias y auditoría." "PostgreSQL" "Database"
        }

        # Sistemas externos
        erp = softwareSystem "ERP" "Entorno simulado del contrato mínimo del ERP vigente." "Externo"
        notificaciones = softwareSystem "Proveedor de mensajería" "Envía el PIN por SMS, WhatsApp o email." "Externo"
        osm = softwareSystem "OpenStreetMap" "Geocodificación principal." "Externo"
        googleMaps = softwareSystem "Google Maps" "Geocodificación de respaldo." "Externo"

        # Relaciones a nivel de contexto
        conductor -> plataforma "Registra entregas"
        administracion -> plataforma "Supervisa entregas y resuelve reclamos"
        cliente -> conductor "Le dicta el PIN"
        plataforma -> erp "Lee comprobantes e informa entregas"
        plataforma -> notificaciones "Envía el PIN"
        notificaciones -> cliente "Envía el PIN"
        plataforma -> osm "Geocodifica direcciones (principal)"
        plataforma -> googleMaps "Geocodifica direcciones (respaldo)"

        # Relaciones a nivel de contenedores
        conductor -> plataforma.pwa "Registra entregas usando"
        administracion -> plataforma.panel "Supervisa entregas usando"

        plataforma.pwa -> plataforma.api "Hace llamadas a la API" "JSON / HTTPS"
        plataforma.panel -> plataforma.api "Hace llamadas a la API" "JSON / HTTPS"
        plataforma.panel -> plataforma.api "Recibe eventos en vivo" "SSE"

        plataforma.api -> plataforma.db "Lee y escribe" "JDBC"
        plataforma.api -> erp "Lee comprobantes e informa entregas" "API del ERP"
        plataforma.api -> notificaciones "Envía el PIN" "HTTPS"
        plataforma.api -> osm "Geocodifica direcciones (principal)" "HTTPS"
        plataforma.api -> googleMaps "Geocodifica direcciones (respaldo)" "HTTPS"

        # Relaciones a nivel de componentes: aplicación del conductor
        conductor -> plataforma.pwa.moduloEntregas "Registra entregas usando"

        plataforma.pwa.moduloEntregas -> plataforma.pwa.captura "Obtiene foto y ubicación"
        plataforma.pwa.moduloEntregas -> plataforma.pwa.validadorLocal "Valida sin conexión"
        plataforma.pwa.moduloEntregas -> plataforma.pwa.clienteApi "Busca y confirma entregas"
        plataforma.pwa.moduloEntregas -> plataforma.pwa.colaOffline "Guarda si no hay conexión"
        plataforma.pwa.colaOffline -> plataforma.pwa.clienteApi "Reintenta el envío"

        plataforma.pwa.clienteApi -> plataforma.api.controlador "Hace llamadas a la API" "JSON / HTTPS"

        # Relaciones a nivel de componentes: panel administrativo
        administracion -> plataforma.panel.seguimiento "Monitorea entregas usando"
        administracion -> plataforma.panel.historial "Consulta evidencias usando"
        administracion -> plataforma.panel.incidencias "Resuelve reclamos usando"
        administracion -> plataforma.panel.metricas "Revisa indicadores usando"

        plataforma.panel.seguimiento -> plataforma.panel.clienteEventos "Se suscribe a eventos"
        plataforma.panel.historial -> plataforma.panel.clienteApi "Consulta evidencias"
        plataforma.panel.incidencias -> plataforma.panel.clienteApi "Registra incidencias y reenvía el PIN"
        plataforma.panel.metricas -> plataforma.panel.clienteApi "Consulta métricas"

        plataforma.panel.clienteApi -> plataforma.api.controlador "Hace llamadas a la API" "JSON / HTTPS"
        plataforma.panel.clienteEventos -> plataforma.api.publicador "Recibe eventos en vivo" "SSE"

        # Relaciones a nivel de componentes: API
        plataforma.api.controlador -> plataforma.api.servicioEntregas "Registra y consulta entregas"
        plataforma.api.controlador -> plataforma.api.servicioPin "Reenvía el PIN"
        plataforma.api.servicioEntregas -> plataforma.api.servicioPin "Valida el PIN"
        plataforma.api.servicioEntregas -> plataforma.api.adaptadorErp "Informa entregas"
        plataforma.api.servicioEntregas -> plataforma.api.repositorio "Guarda evidencias"
        plataforma.api.servicioEntregas -> plataforma.api.publicador "Publica cambios de estado"
        plataforma.api.adaptadorErp -> plataforma.api.servicioPin "Solicita el PIN"
        plataforma.api.servicioPin -> plataforma.api.repositorio "Guarda el PIN"

        plataforma.api.repositorio -> plataforma.db "Lee y escribe" "JDBC"
        plataforma.api.adaptadorErp -> erp "Lee comprobantes e informa entregas" "API del ERP"
        plataforma.api.servicioPin -> notificaciones "Envía el PIN" "HTTPS"
        plataforma.api.servicioEntregas -> osm "Geocodifica (principal)" "HTTPS"
        plataforma.api.servicioEntregas -> googleMaps "Geocodifica (respaldo)" "HTTPS"
    }

    views {
        systemContext plataforma "Contexto" {
            include *
            include cliente
            autolayout lr
            title "Plataforma de verificación de entregas - Diagrama de contexto (C4 nivel 1)"
            description "La plataforma agrega evidencia verificable (PIN, foto y ubicación) a cada entrega sin modificar el ERP."
        }

        container plataforma "Contenedores" {
            include *
            autolayout lr
            title "Plataforma de verificación de entregas - Diagrama de contenedores (C4 nivel 2)"
            description "Aplicación del conductor y panel administrativo en Angular, API en Spring Boot y PostgreSQL."
        }

        component plataforma.pwa "Componentes-PWA" {
            include *
            autolayout lr
            title "Aplicación del conductor - Diagrama de componentes (C4 nivel 3)"
            description "Captura y validación local de la entrega, con cola offline."
        }

        component plataforma.panel "Componentes-Panel" {
            include *
            autolayout lr
            title "Panel administrativo - Diagrama de componentes (C4 nivel 3)"
            description "Seguimiento en vivo, historial, incidencias y métricas."
        }

        component plataforma.api "Componentes-API" {
            include *
            autolayout lr
            title "API de verificación - Diagrama de componentes (C4 nivel 3)"
            description "Versión simplificada para la prueba de concepto."
        }

        styles {
            element "Person" {
                shape Person
                background #08427b
                color #ffffff
            }
            element "Software System" {
                background #1168bd
                color #ffffff
            }
            element "Container" {
                background #438dd5
                color #ffffff
            }
            element "Component" {
                background #85bbf0
                color #000000
            }
            element "Web Browser" {
                shape WebBrowser
            }
            element "Database" {
                shape Cylinder
            }
            element "Externo" {
                background #999999
                color #ffffff
            }
        }
    }

    configuration {
        scope softwaresystem
    }
}
