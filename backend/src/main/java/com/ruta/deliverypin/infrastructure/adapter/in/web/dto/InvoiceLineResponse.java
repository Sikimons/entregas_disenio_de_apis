package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import com.ruta.deliverypin.domain.model.InvoiceLine;

public record InvoiceLineResponse(Long id, String description, Double quantity) {

    public static InvoiceLineResponse from(InvoiceLine line) {
        return new InvoiceLineResponse(line.id(), line.description(), line.quantity());
    }
}
