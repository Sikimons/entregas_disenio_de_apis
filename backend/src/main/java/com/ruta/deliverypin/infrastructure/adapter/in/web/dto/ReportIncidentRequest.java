package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportIncidentRequest(
        @Schema(example = "42") @NotNull Long invoiceId,
        @Schema(example = "001-104-0000001234") @NotBlank String invoiceNumber,
        @Schema(example = "Cliente de prueba") String partnerName,
        @Schema(example = "Calle de prueba, Guayaquil") String deliveryAddress,
        @Schema(example = "Cliente ausente", description = "Ver frontend/src/pages/driver/driverHome/incidentReasons.ts para los motivos que ofrece la PWA.")
        @NotBlank String reason,
        @Schema(example = "Se llamo dos veces, nadie respondio.")
        @Size(max = 500, message = "Las notas no pueden superar los 500 caracteres") String notes,
        @Schema(example = "-2.170998") Double latitude,
        @Schema(example = "-79.922359") Double longitude
) {
}
