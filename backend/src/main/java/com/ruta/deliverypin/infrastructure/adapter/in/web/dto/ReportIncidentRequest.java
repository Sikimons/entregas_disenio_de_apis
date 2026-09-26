package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportIncidentRequest(
        @NotNull Long invoiceId,
        @NotBlank String invoiceNumber,
        String partnerName,
        String deliveryAddress,
        @NotBlank String reason,
        String notes,
        Double latitude,
        Double longitude
) {
}
