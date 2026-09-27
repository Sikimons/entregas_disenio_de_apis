package com.ruta.deliverypin.domain.model;

import java.util.Base64;

/** Datos y operaciones de entregas. */
public record DeliveryPhoto(String base64Content, String filename, String contentType) {

    private static final int MAX_DECODED_BYTES = 5 * 1024 * 1024; // 5 MB

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

    /**
     * Decodifica y valida la evidencia: tamano maximo y que los primeros bytes
     * correspondan a un formato de imagen real (JPEG o PNG), no a un Content-Type
     * declarado sin verificar por el cliente.
     */
    public byte[] decodedBytes() {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64WithoutPrefix());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("La foto de evidencia no es un base64 valido.");
        }
        if (bytes.length == 0) {
            throw new IllegalArgumentException("La foto de evidencia esta vacia.");
        }
        if (bytes.length > MAX_DECODED_BYTES) {
            throw new IllegalArgumentException("La foto de evidencia supera el tamano maximo permitido (5 MB).");
        }
        if (!isJpeg(bytes) && !isPng(bytes)) {
            throw new IllegalArgumentException("La foto de evidencia debe ser una imagen JPEG o PNG valida.");
        }
        return bytes;
    }

    private static boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] bytes) {
        return bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A;
    }
}
