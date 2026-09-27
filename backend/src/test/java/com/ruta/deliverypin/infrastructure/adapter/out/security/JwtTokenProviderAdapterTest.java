package com.ruta.deliverypin.infrastructure.adapter.out.security;

import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.model.Role;
import com.ruta.deliverypin.infrastructure.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderAdapterTest {

    private static final String SECRET = "test_secret_key_at_least_32_characters_long_ok";

    private JwtTokenProviderAdapter adapter;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpirationMinutes(60);
        adapter = new JwtTokenProviderAdapter(properties);
    }

    private Driver driver() {
        return new Driver(1L, "conductor1", "hash", "Conductor Uno", Role.CONDUCTOR, true, Instant.now());
    }

    @Test
    void generateToken_thenExtractUsername_matchesOriginalUser() {
        String token = adapter.generateToken(driver());

        assertThat(adapter.extractUsername(token)).contains("conductor1");
        assertThat(adapter.isValid(token, "conductor1")).isTrue();
    }

    @Test
    void isValid_differentUsername_isFalse() {
        String token = adapter.generateToken(driver());

        assertThat(adapter.isValid(token, "otro-usuario")).isFalse();
    }

    @Test
    void extractUsername_malformedToken_isEmpty() {
        assertThat(adapter.extractUsername("esto-no-es-un-jwt")).isEmpty();
    }

    @Test
    void isValid_tokenSignedWithDifferentSecret_isFalse() {
        String token = adapter.generateToken(driver());

        JwtProperties otherProperties = new JwtProperties();
        otherProperties.setSecret("otro_secreto_distinto_de_al_menos_32_caracteres");
        otherProperties.setExpirationMinutes(60);
        JwtTokenProviderAdapter otherAdapter = new JwtTokenProviderAdapter(otherProperties);

        assertThat(otherAdapter.isValid(token, "conductor1")).isFalse();
        assertThat(otherAdapter.extractUsername(token)).isEmpty();
    }
}
