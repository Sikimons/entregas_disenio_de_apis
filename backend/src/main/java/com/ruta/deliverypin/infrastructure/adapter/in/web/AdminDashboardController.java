package com.ruta.deliverypin.infrastructure.adapter.in.web;

import com.ruta.deliverypin.domain.port.in.GetDriverMetricsUseCase;
import com.ruta.deliverypin.domain.port.in.ListDeliveryAttemptsInRangeUseCase;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.DeliveryAttemptResponse;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.DriverMetricResponse;
import org.springframework.format.annotation.DateTimeFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Datos agregados para el tablero del admin: mapa de entregas/incidencias
 * y metricas por conductor, ambos acotados a un rango de fechas.
 */
@RestController
@Tag(name = "Panorama", description = "ADMIN: mapa y metricas por rango de fechas ISO 8601")
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final ListDeliveryAttemptsInRangeUseCase listDeliveryAttemptsInRangeUseCase;
    private final GetDriverMetricsUseCase getDriverMetricsUseCase;

    public AdminDashboardController(
            ListDeliveryAttemptsInRangeUseCase listDeliveryAttemptsInRangeUseCase,
            GetDriverMetricsUseCase getDriverMetricsUseCase
    ) {
        this.listDeliveryAttemptsInRangeUseCase = listDeliveryAttemptsInRangeUseCase;
        this.getDriverMetricsUseCase = getDriverMetricsUseCase;
    }

    @GetMapping("/map")
    @Operation(summary = "Consultar entregas e incidencias con ubicacion")
    public List<DeliveryAttemptResponse> map(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return listDeliveryAttemptsInRangeUseCase.list(from, to).stream()
                .filter(attempt -> attempt.getLocation() != null)
                .map(DeliveryAttemptResponse::from)
                .toList();
    }

    @GetMapping("/metrics")
    @Operation(summary = "Consultar metricas por conductor")
    public List<DriverMetricResponse> metrics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return getDriverMetricsUseCase.metrics(from, to).stream().map(DriverMetricResponse::from).toList();
    }
}
