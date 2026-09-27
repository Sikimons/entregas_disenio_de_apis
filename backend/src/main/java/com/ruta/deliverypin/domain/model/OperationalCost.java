package com.ruta.deliverypin.domain.model;

import java.time.YearMonth;

/**
 * Reporte de costo por entrega verificada de un mes (showback, Fase1 §4.5). No genera cargos
 * contables: es informativo. costPerDeliveryUsd es null cuando no hubo entregas confirmadas
 * en el mes (division por cero evitada).
 */
public record OperationalCost(
        YearMonth month,
        long confirmedDeliveries,
        long evidencePhotoBytes,
        double evidenceStorageGb,
        double infrastructureCostUsd,
        double storageCostUsd,
        double supportHours,
        double supportCostUsd,
        double totalCostUsd,
        Double costPerDeliveryUsd
) {
}
