package com.ruta.deliverypin.domain.model;

/**
 * Vista de una factura para el panel administrativo (incluye PIN y estado de
 * confirmacion, que el conductor no puede ver). Sustituye al Map&lt;String,Object&gt;
 * crudo que devolvia LocalInvoiceAdapter directamente al controlador.
 */
public record AdminInvoiceView(
        Long id,
        String number,
        String partnerName,
        String deliveryAddress,
        String invoiceDate,
        String state,
        boolean requiresPin,
        String pin,
        boolean confirmed,
        Double expectedLatitude,
        Double expectedLongitude
) {
}
