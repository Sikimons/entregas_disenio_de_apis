package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateDriverRequest(
        @Schema(example = "Conductor Dos") @NotBlank String fullName,
        @Schema(example = "true") @NotNull Boolean active,
        @Schema(description = "Opcional: si se omite o esta en blanco, conserva la contrasena actual.")
        @Size(min = 6, message = "La contrasena debe tener al menos 6 caracteres") String password
) {
}
