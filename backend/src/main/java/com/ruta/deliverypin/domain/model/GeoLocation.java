package com.ruta.deliverypin.domain.model;

/**
 * Coordenadas GPS capturadas por la PWA en el momento de confirmar una entrega.
 */
public record GeoLocation(double latitude, double longitude) {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    public GeoLocation {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitud fuera de rango: " + latitude);
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitud fuera de rango: " + longitude);
        }
    }

    /** Distancia aproximada en metros a otro punto (formula de Haversine). */
    public double distanceMetersTo(GeoLocation other) {
        double lat1 = Math.toRadians(latitude);
        double lat2 = Math.toRadians(other.latitude);
        double deltaLat = Math.toRadians(other.latitude - latitude);
        double deltaLon = Math.toRadians(other.longitude - longitude);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
