package com.ruta.deliverypin.infrastructure.config;

import com.ruta.deliverypin.domain.exception.DeliveryRejectedException;
import com.ruta.deliverypin.domain.exception.ErpUnavailableException;
import com.ruta.deliverypin.infrastructure.adapter.out.invoice.SimulatedErpFailureToggle;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker "erpGateway": protege el adaptador que simula el ERP vigente
 * (LocalInvoiceAdapter), en linea con el riesgo de integracion ya anticipado en
 * Fase1 §5.1. slidingWindowSize/minimumNumberOfCalls quedan fijos porque describen
 * la sensibilidad del patron, no una tarifa de negocio; failureRateThreshold y el
 * tiempo en estado abierto si son configurables (ResilienceProperties).
 */
@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreaker erpGatewayCircuitBreaker(ResilienceProperties properties) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(properties.getFailureRateThreshold())
                .waitDurationInOpenState(Duration.ofSeconds(properties.getWaitDurationOpenStateSeconds()))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                // Un PIN incorrecto o una entrega ya confirmada son reglas de negocio, no una
                // falla del "ERP": no deben contar para abrir el breaker.
                .ignoreExceptions(DeliveryRejectedException.class)
                .build();
        return CircuitBreaker.of("erpGateway", config);
    }

    /**
     * Retry "erpGatewayRetry": reintenta solo fallos transitorios simulados
     * (SimulatedErpFailureToggle.SimulatedErpFailureException) en las lecturas de
     * LocalInvoiceAdapter (ver protectedRead()).
     *
     * "retryExceptions" ya nombra ese unico tipo (N7, docs/EVALUACION_TECNICA.md §18: antes
     * decia "RuntimeException.class", una excepcion generica que en la practica tambien
     * reintentaba cualquier otro error inesperado de la lectura -p. ej. un bug real- en vez
     * de fallar rapido, lo que solo agregaba latencia sin ninguna chance real de exito).
     * "ignoreExceptions" sigue explicito por claridad y por si alguna de esas excepciones
     * alguna vez extendiera SimulatedErpFailureException: nunca se reintenta una excepcion
     * de negocio (PIN invalido, entrega ya confirmada) ni una llamada ya rechazada porque el
     * breaker esta abierto, para no ocultar esas dos senales bajo una capa de reintentos.
     */
    @Bean
    public Retry erpGatewayRetry(ResilienceProperties properties) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(properties.getRetryMaxAttempts())
                .waitDuration(Duration.ofMillis(properties.getRetryWaitDurationMillis()))
                .retryExceptions(SimulatedErpFailureToggle.SimulatedErpFailureException.class)
                .ignoreExceptions(DeliveryRejectedException.class, CallNotPermittedException.class, ErpUnavailableException.class)
                .build();
        return Retry.of("erpGatewayRetry", config);
    }
}
