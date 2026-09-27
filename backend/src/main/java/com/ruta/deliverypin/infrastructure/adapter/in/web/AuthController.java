package com.ruta.deliverypin.infrastructure.adapter.in.web;

import com.ruta.deliverypin.domain.port.in.LoginUseCase;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.LoginRequest;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.LoginResponse;
import com.ruta.deliverypin.infrastructure.adapter.in.web.security.JwtAuthenticationFilter;
import com.ruta.deliverypin.infrastructure.adapter.in.web.security.LoginRateLimiter;
import com.ruta.deliverypin.infrastructure.config.JwtProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Tag(name = "Autenticación", description = "Login por usuario y contraseña, con rate limiting. El JWT viaja en cookie HttpOnly.")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final LoginRateLimiter rateLimiter;
    private final JwtProperties jwtProperties;

    public AuthController(LoginUseCase loginUseCase, LoginRateLimiter rateLimiter, JwtProperties jwtProperties) {
        this.loginUseCase = loginUseCase;
        this.rateLimiter = rateLimiter;
        this.jwtProperties = jwtProperties;
    }

    @Operation(summary = "Iniciar sesion", description = "Autentica por usuario y contrasena y devuelve el rol (ADMIN o CONDUCTOR). "
            + "El JWT viaja en una cookie HttpOnly (no en el cuerpo de la respuesta), para que el JavaScript del "
            + "frontend no pueda leerlo. Limitado por IP y usuario para mitigar fuerza bruta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales validas; la cookie de sesion queda establecida"),
            @ApiResponse(responseCode = "401", description = "Usuario o contrasena incorrectos, o cuenta desactivada"),
            @ApiResponse(responseCode = "429", description = "Demasiados intentos de inicio de sesion; reintentar mas tarde")
    })
    @SecurityRequirements
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // Con server.forward-headers-strategy=native (application.yml), request.getRemoteAddr()
        // ya resuelve la IP real solo cuando el salto entrante es de un proxy interno de
        // confianza (Tomcat RemoteIpValve); si no, es la IP real de quien conecto, nunca una
        // que el propio cliente pueda falsificar via X-Forwarded-For.
        String clientIp = httpRequest.getRemoteAddr();
        rateLimiter.checkAllowed(clientIp, request.username());
        LoginUseCase.AuthResult result = loginUseCase.login(request.username(), request.password());
        rateLimiter.recordSuccess(clientIp, request.username());
        setAuthCookie(httpRequest, httpResponse, result.token(), request.remember());
        return LoginResponse.from(result);
    }

    @Operation(summary = "Cerrar sesion", description = "Invalida la cookie de sesion (JWT) del lado del navegador. "
            + "El backend no mantiene estado de sesion: esto solo borra la cookie, no revoca el token en el servidor.")
    @ApiResponse(responseCode = "204", description = "Cookie de sesion eliminada")
    @SecurityRequirements
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        clearAuthCookie(httpRequest, httpResponse);
        return ResponseEntity.noContent().build();
    }

    /**
     * Migracion de JWT en localStorage/sessionStorage a cookie HttpOnly (auditoria
     * tecnica, hallazgo P1/brecha de Fase2 §6: robo de token por XSS).
     *
     * "Secure"/"SameSite" dependen de httpRequest.isSecure() -- NO son fijos (verificado
     * por ejecucion real, no solo en tests: un cliente HTTP estricto como Python
     * http.cookiejar, a diferencia de un navegador, respeta "Secure" al pie de la letra y
     * nunca reenvia esa cookie sobre http:// simple, ni siquiera contra localhost. Fijar
     * Secure=true rompia scripts/verify_delivery.py y cualquier otro cliente no-navegador
     * contra el stack local, que corre sobre http:// puro):
     * - Peticion segura (https, o http detras de un proxy que ya termino TLS y lo declara
     *   con X-Forwarded-Proto -- ver server.forward-headers-strategy=native en
     *   application.yml): Secure + SameSite=None. Necesario en produccion "Escenario A"
     *   (Fase2 §3.6), donde la PWA en Cloudflare Pages y la API en la EC2 son dominios
     *   distintos -- SameSite=None es indispensable ahi, y los navegadores exigen Secure
     *   junto con el (lo rechazan sin eso).
     * - Peticion insegura (http liso, como el compose local sin TLS): sin Secure,
     *   SameSite=Lax -- alcanza porque ahi todo es mismo origen (nginx local proxya /api/),
     *   y evita depender de que el cliente trate http://localhost como contexto seguro.
     */
    private void setAuthCookie(HttpServletRequest request, HttpServletResponse response, String token, boolean remember) {
        boolean secure = request.isSecure();
        ResponseCookie.ResponseCookieBuilder cookie = ResponseCookie.from(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path("/");
        // "Recuerdame" desmarcado -> cookie de sesion (sin maxAge): el navegador la borra
        // sola al cerrarse, aunque el JWT en si siga vigente por su propia expiracion.
        if (remember) {
            cookie.maxAge(Duration.ofMinutes(jwtProperties.getExpirationMinutes()));
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.build().toString());
    }

    private void clearAuthCookie(HttpServletRequest request, HttpServletResponse response) {
        boolean secure = request.isSecure();
        ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
