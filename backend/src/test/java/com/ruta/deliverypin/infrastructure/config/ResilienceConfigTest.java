package com.ruta.deliverypin.infrastructure.config;

import com.ruta.deliverypin.infrastructure.adapter.out.invoice.SimulatedErpFailureToggle.SimulatedErpFailureException;
import io.github.resilience4j.retry.Retry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * N7 (docs/EVALUACION_TECNICA.md §18): antes "erpGatewayRetry" reintentaba cualquier
 * RuntimeException, no solo el fallo transitorio simulado que su propio Javadoc describia.
 * Estas pruebas construyen el bean tal como lo arma ResilienceConfig (sin necesitar un
 * contexto de Spring completo) y verifican el comportamiento real, no solo el texto del
 * comentario.
 */
class ResilienceConfigTest {

    private Retry erpGatewayRetry(int maxAttempts) {
        ResilienceProperties properties = new ResilienceProperties();
        properties.setRetryMaxAttempts(maxAttempts);
        properties.setRetryWaitDurationMillis(1);
        return new ResilienceConfig().erpGatewayRetry(properties);
    }

    @Test
    void retriesSimulatedFailure_untilItSucceeds() {
        Retry retry = erpGatewayRetry(3);
        AtomicInteger calls = new AtomicInteger();
        Supplier<String> action = () -> {
            if (calls.incrementAndGet() < 2) {
                throw new SimulatedErpFailureException();
            }
            return "ok";
        };

        String result = Retry.decorateSupplier(retry, action).get();

        assertThat(result).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void doesNotRetryAnUnrelatedRuntimeException_failsOnFirstAttempt() {
        // Antes del fix, esto se reintentaba 3 veces (retryExceptions(RuntimeException.class))
        // aunque un IllegalStateException real jamas tiene chance de exito al reintentar.
        Retry retry = erpGatewayRetry(3);
        AtomicInteger calls = new AtomicInteger();
        Supplier<String> action = () -> {
            calls.incrementAndGet();
            throw new IllegalStateException("bug real, no deberia reintentarse");
        };

        assertThatThrownBy(() -> Retry.decorateSupplier(retry, action).get())
                .isInstanceOf(IllegalStateException.class);
        assertThat(calls.get()).isEqualTo(1);
    }
}
