package com.ruta.deliverypin.domain.port.in;

import com.ruta.deliverypin.domain.model.Driver;

import java.util.Optional;

/**
 * Usado por los adaptadores de seguridad (UserDetailsService, filtro JWT,
 * resolucion del usuario autenticado) para cargar el conductor actual.
 */
public interface FindDriverByUsernameUseCase {

    Optional<Driver> findByUsername(String username);
}
