package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "admin") @NotBlank String username,
        @Schema(example = "admin123") @NotBlank String password,
        // "Mantener mi sesion" (LoginPage.tsx): decide si la cookie de sesion (AuthController)
        // se emite con expiracion (sobrevive a cerrar el navegador) o como cookie de sesion
        // pura (se borra sola al cerrarlo). Por defecto false si el cliente no lo manda.
        @Schema(description = "\"Mantener mi sesion\": si es true, la cookie de sesion sobrevive a cerrar el navegador "
                + "(dura app.jwt.expiration-minutes); si es false, se borra sola al cerrarlo.", defaultValue = "false")
        boolean remember
) {
}
