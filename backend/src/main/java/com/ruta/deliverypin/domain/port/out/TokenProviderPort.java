package com.ruta.deliverypin.domain.port.out;

import com.ruta.deliverypin.domain.model.Driver;

import java.util.Optional;

/**
 * Puerto de salida para la emision/validacion de tokens de sesion (JWT en la infraestructura actual).
 */
public interface TokenProviderPort {

    String generateToken(Driver driver);

    Optional<String> extractUsername(String token);

    boolean isValid(String token, String username);
}
