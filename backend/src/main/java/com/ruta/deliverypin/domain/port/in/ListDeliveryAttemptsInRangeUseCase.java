package com.ruta.deliverypin.domain.port.in;

import com.ruta.deliverypin.domain.model.DeliveryAttemptSummary;

import java.time.Instant;
import java.util.List;

/**
 * Usado por el mapa del admin: todos los intentos (entregas e incidencias) con
 * coordenadas GPS dentro de un rango de fechas, sin paginar.
 */
public interface ListDeliveryAttemptsInRangeUseCase {

    List<DeliveryAttemptSummary> list(Instant from, Instant to);
}
