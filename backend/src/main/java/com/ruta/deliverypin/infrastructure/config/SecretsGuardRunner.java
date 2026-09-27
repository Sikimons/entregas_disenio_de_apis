package com.ruta.deliverypin.infrastructure.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Falla el arranque si JWT_SECRET o ADMIN_PASSWORD siguen siendo el valor por defecto y
 * no se esta en modo de siembra explicito (APP_SEED_ENABLED=false). Solo el servicio
 * "seed" del compose corre con APP_SEED_ENABLED=true (una sola pasada, ver
 * infrastructure.seed.Initializer); el "backend" que queda sirviendo siempre arranca en
 * false, asi que este guard sigue exigiendole un JWT_SECRET y un ADMIN_PASSWORD propios.
 */
@Component
public class SecretsGuardRunner implements ApplicationRunner {

    private static final String PLACEHOLDER_SECRET = "CHANGE_ME_super_secret_key_min_32_chars_long_please";
    private static final String PLACEHOLDER_ADMIN_PASSWORD = "admin123";

    private final JwtProperties jwtProperties;
    private final AppProperties appProperties;

    public SecretsGuardRunner(JwtProperties jwtProperties, AppProperties appProperties) {
        this.jwtProperties = jwtProperties;
        this.appProperties = appProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (appProperties.getSeed().isEnabled()) {
            return;
        }
        if (PLACEHOLDER_SECRET.equals(jwtProperties.getSecret())) {
            throw new IllegalStateException(
                    "JWT_SECRET no fue configurado (sigue siendo el valor de ejemplo). "
                            + "Define un secreto propio de al menos 32 caracteres, o habilita APP_SEED_ENABLED=true si es un entorno de siembra de datos de muestra.");
        }
        if (PLACEHOLDER_ADMIN_PASSWORD.equals(appProperties.getAdmin().getBootstrapPassword())) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD no fue configurado (sigue siendo el valor de ejemplo 'admin123'). "
                            + "Define una contrasena propia, o habilita APP_SEED_ENABLED=true si es un entorno de siembra de datos de muestra.");
        }
    }
}
