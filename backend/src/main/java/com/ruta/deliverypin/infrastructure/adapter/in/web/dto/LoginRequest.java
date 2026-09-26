package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequest(
        @NotBlank @Schema(example = "admin") String username,
        @NotBlank @Schema(example = "admin123", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY) String password
) {
}
