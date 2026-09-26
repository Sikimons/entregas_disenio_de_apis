package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import com.ruta.deliverypin.domain.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDriverRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 6, message = "La contrasena debe tener al menos 6 caracteres") String password,
        @NotBlank String fullName,
        @NotNull Role role
) {
}
