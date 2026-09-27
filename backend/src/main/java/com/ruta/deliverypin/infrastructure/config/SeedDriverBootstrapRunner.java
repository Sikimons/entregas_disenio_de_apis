package com.ruta.deliverypin.infrastructure.config;

import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.model.Role;
import com.ruta.deliverypin.domain.port.out.DriverRepositoryPort;
import com.ruta.deliverypin.domain.port.out.PasswordEncoderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Crea un conductor de muestra (conductor/conductor123) en el primer arranque, solo
 * en entornos de siembra (Fase7): sin este runner, scripts/verify_delivery.py necesitaba que
 * alguien lo creara a mano vía /api/v1/admin/users antes de poder correr en una instalacion
 * nueva. Mismo patron que AdminBootstrapRunner, gated igual que SeedInvoiceLoader.
 *
 * @Order(1) explicito: debe correr antes que SeedExitRunner (ver su Javadoc para el
 * detalle del bug de ordenamiento que esto corrige).
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Order(1)
public class SeedDriverBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDriverBootstrapRunner.class);
    private static final String USERNAME = "conductor";
    private static final String PASSWORD = "conductor123";

    private final DriverRepositoryPort driverRepository;
    private final PasswordEncoderPort passwordEncoder;

    public SeedDriverBootstrapRunner(DriverRepositoryPort driverRepository, PasswordEncoderPort passwordEncoder) {
        this.driverRepository = driverRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (driverRepository.existsByUsername(USERNAME)) {
            return;
        }
        Driver seedDriver = Driver.createNew(USERNAME, passwordEncoder.encode(PASSWORD), "Conductor Demo", Role.CONDUCTOR);
        driverRepository.save(seedDriver);
        log.info("Conductor de muestra creado: {}", USERNAME);
    }
}
