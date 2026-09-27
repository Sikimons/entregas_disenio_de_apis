package com.ruta.deliverypin.infrastructure.adapter.in.web;

import com.ruta.deliverypin.domain.port.in.LoginUseCase;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.LoginRequest;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.LoginResponse;
import com.ruta.deliverypin.infrastructure.adapter.in.web.security.LoginRateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticación", description = "Login por usuario y contraseña, con rate limiting.")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final LoginRateLimiter rateLimiter;

    public AuthController(LoginUseCase loginUseCase, LoginRateLimiter rateLimiter) {
        this.loginUseCase = loginUseCase;
        this.rateLimiter = rateLimiter;
    }

    @Operation(summary = "Iniciar sesion", description = "Autentica por usuario y contrasena y devuelve un JWT con el rol (ADMIN o CONDUCTOR). "
            + "Limitado por IP y usuario para mitigar fuerza bruta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales validas; se devuelve el token"),
            @ApiResponse(responseCode = "401", description = "Usuario o contrasena incorrectos, o cuenta desactivada"),
            @ApiResponse(responseCode = "429", description = "Demasiados intentos de inicio de sesion; reintentar mas tarde")
    })
    @SecurityRequirements
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = clientIp(httpRequest);
        rateLimiter.checkAllowed(clientIp, request.username());
        LoginUseCase.AuthResult result = loginUseCase.login(request.username(), request.password());
        rateLimiter.recordSuccess(clientIp, request.username());
        return LoginResponse.from(result);
    }

    private String clientIp(HttpServletRequest request) {
        // Detras de Cloudflare/Nginx, la IP real del cliente llega en X-Forwarded-For
        // (primer valor de la lista); sin proxy, se usa la IP de la conexion directa.
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
