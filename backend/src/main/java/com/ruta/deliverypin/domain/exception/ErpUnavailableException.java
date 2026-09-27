package com.ruta.deliverypin.domain.exception;

/**
 * El Circuit Breaker que protege la simulacion del ERP (LocalInvoiceAdapter) esta
 * abierto: se rechaza la llamada sin tocar la base de datos, en vez de esperar a que
 * falle. Se mapea a 503 en GlobalExceptionHandler.
 */
public class ErpUnavailableException extends RuntimeException {

    public ErpUnavailableException() {
        super("Servicio de facturacion temporalmente no disponible. Intenta nuevamente en unos segundos.");
    }
}
