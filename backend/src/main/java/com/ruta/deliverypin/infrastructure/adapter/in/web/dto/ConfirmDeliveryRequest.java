package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ConfirmDeliveryRequest(
        @NotNull Long invoiceId,
        @NotNull @Pattern(regexp = "\\d{6}", message = "El PIN debe tener 6 digitos") String pin,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotBlank(message = "La foto de evidencia es obligatoria") String photoBase64,
        String photoFilename,
        String photoContentType
) {
}
