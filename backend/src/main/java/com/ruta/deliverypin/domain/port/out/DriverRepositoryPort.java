package com.ruta.deliverypin.domain.port.out;

import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.domain.model.PageResult;

import java.util.Optional;

/**
 * Puerto de salida (driven port) para la persistencia de usuarios/conductores.
 * Lo implementa un adaptador de infraestructura (JPA, en este caso).
 */
public interface DriverRepositoryPort {

    Optional<Driver> findByUsername(String username);

    Optional<Driver> findById(Long id);

    boolean existsByUsername(String username);

    PageResult<Driver> findAll(PageRequest pageRequest);

    Driver save(Driver driver);

    void deleteById(Long id);
}
