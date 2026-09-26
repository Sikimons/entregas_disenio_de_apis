package com.ruta.deliverypin.domain.port.in;

import com.ruta.deliverypin.domain.model.DriverMetric;

import java.time.Instant;
import java.util.List;

public interface GetDriverMetricsUseCase {

    List<DriverMetric> metrics(Instant from, Instant to);
}
