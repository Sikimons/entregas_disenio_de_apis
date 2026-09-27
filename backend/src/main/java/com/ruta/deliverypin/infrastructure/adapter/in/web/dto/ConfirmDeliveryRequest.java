package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ConfirmDeliveryRequest(
        @Schema(example = "42") @NotNull Long invoiceId,
        @Schema(example = "123456", description = "PIN de 6 digitos comunicado al cliente al publicar la factura.")
        @NotNull @Pattern(regexp = "\\d{6}", message = "El PIN debe tener 6 digitos") String pin,
        @Schema(example = "-2.170998") @NotNull Double latitude,
        @Schema(example = "-79.922359") @NotNull Double longitude,
        @Schema(description = "Foto en base64, con o sin el prefijo \"data:image/jpeg;base64,\". "
                + "Debe decodificar a un JPEG o PNG real (verificado por los primeros bytes) de hasta 5 MB.")
        @NotBlank(message = "La foto de evidencia es obligatoria") String photoBase64,
        @Schema(example = "evidencia.jpg") String photoFilename,
        @Schema(example = "image/jpeg") String photoContentType
) {
}
