package com.ruta.deliverypin.domain.exception;

public class TooManyConfirmAttemptsException extends RuntimeException {
    public TooManyConfirmAttemptsException() {
        super("Demasiados intentos de confirmacion de entrega. Espera un momento e intenta nuevamente.");
    }
}
