package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import com.ruta.deliverypin.domain.port.in.LoginUseCase;

public record LoginResponse(String token, String username, String fullName, String role) {

    public static LoginResponse from(LoginUseCase.AuthResult result) {
        return new LoginResponse(result.token(), result.username(), result.fullName(), result.role().name());
    }
}
