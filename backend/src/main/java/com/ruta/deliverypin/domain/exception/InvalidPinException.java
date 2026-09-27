package com.ruta.deliverypin.domain.exception;

/** El PIN ingresado no coincide con el de la factura. Se mapea a 422 Unprocessable Entity. */
public class InvalidPinException extends DeliveryRejectedException {
    public InvalidPinException() {
        super("El PIN ingresado no es correcto.");
    }
}
