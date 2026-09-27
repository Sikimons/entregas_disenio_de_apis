package com.ruta.deliverypin.infrastructure.config;

import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.model.Role;
import com.ruta.deliverypin.domain.port.out.DriverRepositoryPort;
import com.ruta.deliverypin.domain.port.out.PasswordEncoderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Crea el usuario administrador inicial en el primer arranque, si todavia no existe.
 * Depende unicamente de puertos de salida del dominio (no de detalles de persistencia).
 *
 * @Order(0) explicito: sin el, este runner queda "empatado" (sin orden declarado, ambos
 * caen en Ordered.LOWEST_PRECEDENCE) con DemoSeedExitRunner, y el orden de desempate real
 * de Spring depende del orden de registro de los bean definitions durante el escaneo de
 * componentes -- NO es alfabetico de forma garantizada (ver el bug real que esto causaba,
 * corregido junto con DemoDriverBootstrapRunner/DemoInvoiceLoader/DemoSeedExitRunner).
 */
@Component
@Order(0)
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final DriverRepositoryPort driverRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final AppProperties appProperties;

    public AdminBootstrapRunner(DriverRepositoryPort driverRepository, PasswordEncoderPort passwordEncoder, AppProperties appProperties) {
        this.driverRepository = driverRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    @Override
    public void run(String... args) {
        String username = appProperties.getAdmin().getBootstrapUsername();
        if (driverRepository.existsByUsername(username)) {
            return;
        }
        Driver admin = Driver.createNew(
                username,
                passwordEncoder.encode(appProperties.getAdmin().getBootstrapPassword()),
                "Administrador",
                Role.ADMIN
        );
        driverRepository.save(admin);
        log.info("Usuario admin inicial creado: {}", username);
    }
}
