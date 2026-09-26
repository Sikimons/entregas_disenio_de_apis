package com.ruta.deliverypin.domain.exception;

public class DriverAlreadyExistsException extends RuntimeException {
    public DriverAlreadyExistsException(String username) {
        super("Ya existe un usuario con el nombre de usuario '" + username + "'.");
    }
}
