package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateDriverRequest(
        @NotBlank String fullName,
        @NotNull Boolean active,
        @Size(min = 6, message = "La contrasena debe tener al menos 6 caracteres") String password
) {
}
