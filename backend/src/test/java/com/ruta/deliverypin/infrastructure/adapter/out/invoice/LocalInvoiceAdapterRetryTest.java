package com.ruta.deliverypin.infrastructure.adapter.out.invoice;

import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.InvoiceLineJpaEntity;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository.SpringDataInvoiceJpaRepository;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository.SpringDataInvoiceLineJpaRepository;
import com.ruta.deliverypin.infrastructure.config.ResilienceProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba unitaria del Retry "erpGatewayRetry" (Fase8, ver LocalInvoiceAdapter.protectedRead()):
 * un fallo transitorio simulado dentro del limite de reintentos se absorbe sin que el
 * llamador lo note; una racha mas larga que el limite si se propaga.
 */
class LocalInvoiceAdapterRetryTest {

    private SpringDataInvoiceJpaRepository invoices;
    private SpringDataInvoiceLineJpaRepository lines;
    private SimulatedErpFailureToggle failureToggle;
    private LocalInvoiceAdapter adapter;

    @BeforeEach
    void setUp() {
        invoices = Mockito.mock(SpringDataInvoiceJpaRepository.class);
        lines = Mockito.mock(SpringDataInvoiceLineJpaRepository.class);
        failureToggle = new SimulatedErpFailureToggle();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                // minimumNumberOfCalls alto a proposito: este test aisla el Retry, no el breaker.
                .minimumNumberOfCalls(100)
                .slidingWindowSize(100)
                .build());
        Retry retry = Retry.of("test", RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(5))
                .build());

        ResilienceProperties properties = new ResilienceProperties();
        properties.setCircuitBreakerEnabled(true);
        properties.setRetryEnabled(true);

        adapter = new LocalInvoiceAdapter(invoices, lines, circuitBreaker, retry, properties, failureToggle);

        Mockito.when(lines.findByInvoice_IdOrderById(1L)).thenReturn(List.of(new InvoiceLineJpaEntity(10L, null, "item", 2.0)));
    }

    @Test
    void transientFailure_withinRetryLimit_isAbsorbedTransparently() {
        // 2 fallos simulados, 3 intentos permitidos: el 3er intento tiene exito.
        failureToggle.simulateFailures(2);

        List<?> result = adapter.findInvoiceLines(1L);

        assertThat(result).hasSize(1);
        assertThat(failureToggle.pendingFailures()).isZero();
    }

    @Test
    void failuresBeyondRetryLimit_stillPropagate() {
        // 10 fallos simulados, solo 3 intentos permitidos: los 3 fallan.
        failureToggle.simulateFailures(10);

        assertThatThrownBy(() -> adapter.findInvoiceLines(1L))
                .isInstanceOf(SimulatedErpFailureToggle.SimulatedErpFailureException.class);

        assertThat(failureToggle.pendingFailures()).isEqualTo(7);
    }

    @Test
    void whenRetryDisabled_firstFailureAlreadyPropagates() {
        ResilienceProperties disabled = new ResilienceProperties();
        disabled.setCircuitBreakerEnabled(true);
        disabled.setRetryEnabled(false);
        LocalInvoiceAdapter noRetryAdapter = new LocalInvoiceAdapter(invoices, lines,
                CircuitBreaker.of("test-no-retry", CircuitBreakerConfig.custom().minimumNumberOfCalls(100).slidingWindowSize(100).build()),
                Retry.of("unused", RetryConfig.ofDefaults()), disabled, failureToggle);

        failureToggle.simulateFailures(1);

        assertThatThrownBy(() -> noRetryAdapter.findInvoiceLines(1L))
                .isInstanceOf(SimulatedErpFailureToggle.SimulatedErpFailureException.class);
        assertThat(failureToggle.pendingFailures()).isZero();
    }
}
