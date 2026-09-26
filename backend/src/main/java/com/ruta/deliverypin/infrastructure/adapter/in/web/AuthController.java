package com.ruta.deliverypin.infrastructure.adapter.in.web;

import com.ruta.deliverypin.domain.port.in.LoginUseCase;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.LoginRequest;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.LoginResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Autenticacion", description = "Inicio de sesion y obtencion del JWT")
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;

    public AuthController(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion y obtener un token JWT")
    @SecurityRequirements
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        LoginUseCase.AuthResult result = loginUseCase.login(request.username(), request.password());
        return LoginResponse.from(result);
    }
}
