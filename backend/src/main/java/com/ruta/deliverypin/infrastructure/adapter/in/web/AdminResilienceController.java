package com.ruta.deliverypin.infrastructure.adapter.in.web;

import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.CircuitBreakerStatusResponse;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.SimulateFailuresRequest;
import com.ruta.deliverypin.infrastructure.adapter.out.invoice.SimulatedErpFailureToggle;
import com.ruta.deliverypin.infrastructure.config.ResilienceProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Permite observar el Circuit Breaker "erpGateway" y el Retry "erpGatewayRetry" (que
 * protegen LocalInvoiceAdapter, la simulacion del ERP de Fase1 §3) y probarlos de forma
 * segura, simulando fallos sin depender de que algo externo falle de verdad.
 */
@Tag(name = "Resiliencia", description = "Circuit Breaker y Retry que protegen la simulacion del ERP; permite simular fallos para probarlos en vivo.")
@RestController
@RequestMapping("/api/v1/admin/resilience")
public class AdminResilienceController {

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final ResilienceProperties resilienceProperties;
    private final SimulatedErpFailureToggle failureToggle;

    public AdminResilienceController(
            CircuitBreaker erpGatewayCircuitBreaker,
            Retry erpGatewayRetry,
            ResilienceProperties resilienceProperties,
            SimulatedErpFailureToggle failureToggle
    ) {
        this.circuitBreaker = erpGatewayCircuitBreaker;
        this.retry = erpGatewayRetry;
        this.resilienceProperties = resilienceProperties;
        this.failureToggle = failureToggle;
    }

    @Operation(summary = "Estado del Circuit Breaker y del Retry", description = "Estado actual del breaker erpGateway (cerrado/abierto/semiabierto) y del retry "
            + "erpGatewayRetry (llamadas que se recuperaron solas tras un fallo transitorio), habilitados o no, y tasas de exito/fallo.")
    @ApiResponse(responseCode = "200", description = "Estado del breaker y del retry")
    @GetMapping("/status")
    public CircuitBreakerStatusResponse status() {
        return CircuitBreakerStatusResponse.from(
                circuitBreaker, resilienceProperties.isCircuitBreakerEnabled(), failureToggle.pendingFailures(),
                retry, resilienceProperties.isRetryEnabled());
    }

    @Operation(summary = "Simular fallas del ERP", description = "Fuerza N fallas dentro de las llamadas protegidas por el breaker y el retry, para demostrar "
            + "tanto la recuperacion transparente de fallos aislados (Retry) como el ciclo cerrado -> abierto -> semiabierto -> cerrado (Circuit Breaker) "
            + "ante una racha mas larga, sin depender de un ERP real.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fallas programadas; se devuelve el estado del breaker"),
            @ApiResponse(responseCode = "400", description = "count fuera del rango permitido (1 a 50)")
    })
    @PostMapping("/simulate-failures")
    public CircuitBreakerStatusResponse simulateFailures(@Valid @RequestBody SimulateFailuresRequest request) {
        failureToggle.simulateFailures(request.count());
        return status();
    }
}
