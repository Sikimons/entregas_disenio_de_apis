package com.ruta.deliverypin.domain.port.out;

/**
 * Puerto de salida para el hashing/verificacion de contrasenas, desacoplado de la
 * libreria concreta (BCrypt, Argon2, etc) usada por la infraestructura.
 */
public interface PasswordEncoderPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
