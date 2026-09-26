package com.ruta.deliverypin.domain.exception;

/** Datos y operaciones de entregas. */
public class DeliveryRejectedException extends RuntimeException {
    public DeliveryRejectedException(String reason) {
        super(reason);
    }
}
