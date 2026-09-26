package com.ruta.deliverypin.domain.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Usuario o contrasena incorrectos.");
    }
}
