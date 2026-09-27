package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

/**
 * Estado observable del Circuit Breaker "erpGateway" y del Retry "erpGatewayRetry"
 * (Fase8), para el panel de administracion. Un mismo simulate-failures demuestra ambos
 * patrones: fallos aislados los absorbe el Retry sin que el llamador los note; una racha
 * mas larga termina abriendo el Circuit Breaker.
 */
public record CircuitBreakerStatusResponse(
        String state,
        boolean enabled,
        int pendingSimulatedFailures,
        float failureRate,
        long numberOfSuccessfulCalls,
        long numberOfFailedCalls,
        long numberOfNotPermittedCalls,
        boolean retryEnabled,
        long retrySuccessfulCallsWithoutRetry,
        long retrySuccessfulCallsWithRetry,
        long retryFailedCallsWithRetry
) {
    public static CircuitBreakerStatusResponse from(CircuitBreaker circuitBreaker, boolean enabled, int pendingSimulatedFailures,
                                                     Retry retry, boolean retryEnabled) {
        var metrics = circuitBreaker.getMetrics();
        var retryMetrics = retry.getMetrics();
        return new CircuitBreakerStatusResponse(
                circuitBreaker.getState().name(),
                enabled,
                pendingSimulatedFailures,
                metrics.getFailureRate(),
                metrics.getNumberOfSuccessfulCalls(),
                metrics.getNumberOfFailedCalls(),
                metrics.getNumberOfNotPermittedCalls(),
                retryEnabled,
                retryMetrics.getNumberOfSuccessfulCallsWithoutRetryAttempt(),
                retryMetrics.getNumberOfSuccessfulCallsWithRetryAttempt(),
                retryMetrics.getNumberOfFailedCallsWithRetryAttempt()
        );
    }
}
