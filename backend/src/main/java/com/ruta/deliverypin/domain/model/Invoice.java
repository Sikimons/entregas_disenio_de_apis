package com.ruta.deliverypin.domain.model;

/** Datos y operaciones de entregas. */
public record Invoice(
        Long id,
        String number,
        String partnerName,
        String deliveryAddress,
        String invoiceDate,
        String state,
        Double expectedLatitude,
        Double expectedLongitude
) {
}
