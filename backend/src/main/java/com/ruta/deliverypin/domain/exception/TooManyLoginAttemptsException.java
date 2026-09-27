package com.ruta.deliverypin.domain.exception;

public class TooManyLoginAttemptsException extends RuntimeException {
    public TooManyLoginAttemptsException() {
        super("Demasiados intentos de inicio de sesion. Espera un momento e intenta nuevamente.");
    }
}
