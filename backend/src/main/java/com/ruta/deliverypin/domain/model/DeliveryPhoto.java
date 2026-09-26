package com.ruta.deliverypin.domain.model;

import java.util.Base64;

/** Datos y operaciones de entregas. */
public record DeliveryPhoto(String base64Content, String filename, String contentType) {

    public DeliveryPhoto {
        if (base64Content == null || base64Content.isBlank()) {
            throw new IllegalArgumentException("La foto de evidencia esta vacia.");
        }
    }

    /** El contenido base64 sin el prefijo "data:image/...;base64," si viene de un <input> del navegador. */
    public String base64WithoutPrefix() {
        int commaIndex = base64Content.indexOf(',');
        return base64Content.startsWith("data:") && commaIndex >= 0
                ? base64Content.substring(commaIndex + 1)
                : base64Content;
    }

    public byte[] decodedBytes() {
        return Base64.getDecoder().decode(base64WithoutPrefix());
    }
}
