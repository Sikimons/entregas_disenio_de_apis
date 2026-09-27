package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportIncidentRequest(
        @NotNull Long invoiceId,
        @NotBlank String invoiceNumber,
        String partnerName,
        String deliveryAddress,
        @NotBlank String reason,
        @Size(max = 500, message = "Las notas no pueden superar los 500 caracteres") String notes,
        Double latitude,
        Double longitude
) {
}
