package com.ruta.deliverypin.domain.exception;

/** La entrega de esta factura ya fue confirmada antes. Se mapea a 409 Conflict. */
public class DeliveryAlreadyConfirmedException extends DeliveryRejectedException {
    public DeliveryAlreadyConfirmedException() {
        super("La entrega de esta factura ya fue confirmada.");
    }
}
