package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import com.ruta.deliverypin.domain.model.OperationalCost;

/** Reporte de costo por entrega verificada de un mes (showback, Fase1 §4.5). Es informativo. */
public record OperationalCostResponse(
        String month,
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
    public static OperationalCostResponse from(OperationalCost cost) {
        return new OperationalCostResponse(
                cost.month().toString(),
                cost.confirmedDeliveries(),
                cost.evidencePhotoBytes(),
                cost.evidenceStorageGb(),
                cost.infrastructureCostUsd(),
                cost.storageCostUsd(),
                cost.supportHours(),
                cost.supportCostUsd(),
                cost.totalCostUsd(),
                cost.costPerDeliveryUsd()
        );
    }
}
