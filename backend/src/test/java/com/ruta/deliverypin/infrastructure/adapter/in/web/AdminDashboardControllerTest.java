package com.ruta.deliverypin.infrastructure.adapter.in.web;

import com.ruta.deliverypin.domain.port.in.GetDriverMetricsUseCase;
import com.ruta.deliverypin.domain.port.in.ListDeliveryAttemptsInRangeUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Valida el rango maximo de fechas del tablero administrativo (Fase8, RA3 §5.1): sin esto,
 * /map y /metrics podian pedir una agregacion arbitrariamente pesada sobre delivery_log.
 */
class AdminDashboardControllerTest {

    private final ListDeliveryAttemptsInRangeUseCase listUseCase = Mockito.mock(ListDeliveryAttemptsInRangeUseCase.class);
    private final GetDriverMetricsUseCase metricsUseCase = Mockito.mock(GetDriverMetricsUseCase.class);
    private final AdminDashboardController controller = new AdminDashboardController(listUseCase, metricsUseCase);

    @Test
    void map_rangeWithinLimit_isAccepted() {
        Instant to = Instant.now();
        Instant from = to.minus(30, ChronoUnit.DAYS);
        Mockito.when(listUseCase.list(from, to)).thenReturn(List.of());

        assertThatCode(() -> controller.map(from, to)).doesNotThrowAnyException();
    }

    @Test
    void map_rangeBeyond93Days_isRejected() {
        Instant to = Instant.now();
        Instant from = to.minus(200, ChronoUnit.DAYS);

        assertThatThrownBy(() -> controller.map(from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("93");
    }

    @Test
    void metrics_toBeforeFrom_isRejected() {
        Instant from = Instant.now();
        Instant to = from.minus(1, ChronoUnit.DAYS);

        assertThatThrownBy(() -> controller.metrics(from, to))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
