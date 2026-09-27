package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import com.ruta.deliverypin.domain.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDriverRequest(
        @Schema(example = "conductor2") @NotBlank String username,
        @Schema(example = "conductor123") @NotBlank @Size(min = 6, message = "La contrasena debe tener al menos 6 caracteres") String password,
        @Schema(example = "Conductor Dos") @NotBlank String fullName,
        @Schema(example = "CONDUCTOR") @NotNull Role role
) {
}
