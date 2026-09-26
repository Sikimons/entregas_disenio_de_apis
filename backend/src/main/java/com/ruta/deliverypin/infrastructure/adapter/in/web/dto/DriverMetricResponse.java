package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import com.ruta.deliverypin.domain.model.DriverMetric;

public record DriverMetricResponse(String driverName, long confirmed, long rejected, long incident, long total) {

    public static DriverMetricResponse from(DriverMetric metric) {
        return new DriverMetricResponse(
                metric.driverName(),
                metric.confirmed(),
                metric.rejected(),
                metric.incident(),
                metric.total()
        );
    }
}
