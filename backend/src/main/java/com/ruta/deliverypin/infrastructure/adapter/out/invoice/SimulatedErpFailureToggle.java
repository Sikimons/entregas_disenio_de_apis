package com.ruta.deliverypin.infrastructure.adapter.out.invoice;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Interruptor para probar el Circuit Breaker sin depender de que algo externo falle
 * de verdad: el admin activa N fallos simulados, y las siguientes N llamadas al
 * adaptador que simula el ERP (LocalInvoiceAdapter) fallan, permitiendo observar
 * como el breaker se abre y luego se recupera.
 */
@Component
public class SimulatedErpFailureToggle {

    private final AtomicInteger pendingFailures = new AtomicInteger(0);

    public void simulateFailures(int count) {
        pendingFailures.set(Math.max(0, count));
    }

    public void maybeFail() {
        int previous = pendingFailures.getAndUpdate(v -> v > 0 ? v - 1 : v);
        if (previous > 0) {
            throw new SimulatedErpFailureException();
        }
    }

    public int pendingFailures() {
        return pendingFailures.get();
    }

    public static class SimulatedErpFailureException extends RuntimeException {
        public SimulatedErpFailureException() {
            super("Fallo simulado del ERP (prueba del Circuit Breaker).");
        }
    }
}
