package com.ruta.deliverypin.domain.model;

/**
 * Resumen de intentos de entrega de un conductor en un rango de fechas,
 * para el tablero de metricas del admin.
 */
public record DriverMetric(
        String driverName,
        long confirmed,
        long rejected,
        long incident
) {
    public long total() {
        return confirmed + rejected + incident;
    }
}
