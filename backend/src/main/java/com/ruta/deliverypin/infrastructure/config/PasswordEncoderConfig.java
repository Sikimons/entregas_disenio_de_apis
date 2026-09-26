package com.ruta.deliverypin.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Definido en su propia clase (sin otras dependencias) para evitar un ciclo:
 * SecurityConfig depende de SpringSecurityUserDetailsAdapter, que a traves de
 * DriverManagementApplicationService y SpringPasswordEncoderAdapter necesita
 * este mismo bean PasswordEncoder.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
