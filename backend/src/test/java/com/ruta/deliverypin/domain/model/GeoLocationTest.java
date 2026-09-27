package com.ruta.deliverypin.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class GeoLocationTest {

    @Test
    void distanceMetersTo_samePoint_isZero() {
        GeoLocation point = new GeoLocation(-2.170998, -79.922359);
        assertThat(point.distanceMetersTo(point)).isCloseTo(0.0, within(0.01));
    }

    @Test
    void distanceMetersTo_guayaquilToQuito_isAroundKnownDistance() {
        GeoLocation guayaquil = new GeoLocation(-2.170998, -79.922359);
        GeoLocation quito = new GeoLocation(-0.180653, -78.467834);

        double distanceKm = guayaquil.distanceMetersTo(quito) / 1000.0;

        // Distancia real en linea recta Guayaquil-Quito: ~ 264 km.
        assertThat(distanceKm).isBetween(250.0, 280.0);
    }

    @Test
    void constructor_latitudeOutOfRange_throws() {
        assertThatThrownBy(() -> new GeoLocation(90.1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeoLocation(-90.1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_longitudeOutOfRange_throws() {
        assertThatThrownBy(() -> new GeoLocation(0, 180.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeoLocation(0, -180.1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
