package com.ruta.deliverypin.domain.exception;

public class DriverNotFoundException extends RuntimeException {
    public DriverNotFoundException(Long id) {
        super("No se encontro el usuario con id " + id + ".");
    }

    public DriverNotFoundException(String username) {
        super("No se encontro el usuario '" + username + "'.");
    }
}
