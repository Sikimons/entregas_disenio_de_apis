package com.ruta.deliverypin.domain.model;

/**
 * Un producto vendido en la factura, tal como lo necesita el conductor para
 * marcarlo como verificado/cargado en su checklist de entrega. Sin precios.
 */
public record InvoiceLine(
        Long id,
        String description,
        Double quantity
) {
}
