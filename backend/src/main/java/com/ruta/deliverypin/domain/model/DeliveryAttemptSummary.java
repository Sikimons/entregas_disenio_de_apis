package com.ruta.deliverypin.domain.model;

import java.time.Instant;

/**
 * Resumen de un intento de entrega para listados (mapa, historial paginado): igual que
 * DeliveryAttempt pero sin el contenido de la foto ni el Driver completo, calculado con
 * una proyeccion SQL que no trae la columna `photo` (bytea) a memoria. Usar DeliveryAttempt
 * (con save()) para escribir; usar este resumen solo para leer/listar.
 */
public record DeliveryAttemptSummary(
        Long id,
        Long invoiceId,
        String invoiceNumber,
        String partnerName,
        String deliveryAddress,
        String driverName,
        DeliveryAttemptOutcome outcome,
        Double latitude,
        Double longitude,
        String detail,
        boolean hasPhoto,
        Double distanceFromExpectedMeters,
        Instant createdAt
) {
}
