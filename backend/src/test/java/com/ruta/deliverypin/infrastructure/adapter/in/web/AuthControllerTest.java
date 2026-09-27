package com.ruta.deliverypin.infrastructure.adapter.in.web;

import com.ruta.deliverypin.domain.model.Role;
import com.ruta.deliverypin.domain.port.in.LoginUseCase;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.LoginRequest;
import com.ruta.deliverypin.infrastructure.adapter.in.web.security.JwtAuthenticationFilter;
import com.ruta.deliverypin.infrastructure.adapter.in.web.security.LoginRateLimiter;
import com.ruta.deliverypin.infrastructure.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Cubre la migracion de JWT en el cuerpo de la respuesta a cookie HttpOnly (auditoria
 * tecnica, Tanda 1): sin este test, AuthController.setAuthCookie()/clearAuthCookie() -el
 * cambio de seguridad mas sensible de toda la auditoria- no tenia ninguna cobertura propia,
 * solo la indirecta de los tests de integracion end-to-end que no llegaron a escribirse.
 *
 * Los tests "insegura" (Tanda 4, verificacion end-to-end real) cubren el hallazgo que la
 * ejecucion real contra el stack local expuso: Secure=true fijo rompia
 * scripts/verify_delivery.py (Python http.cookiejar rechaza reenviar una cookie Secure
 * sobre http:// puro, a diferencia de un navegador real, que si trata http://localhost
 * como contexto seguro). Ahora depende de request.isSecure().
 */
class AuthControllerTest {

    private final LoginUseCase loginUseCase = mock(LoginUseCase.class);
    private final LoginRateLimiter rateLimiter = mock(LoginRateLimiter.class);
    private AuthController controller;
    private static final String COOKIE = JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret");
        jwtProperties.setExpirationMinutes(480);
        controller = new AuthController(loginUseCase, rateLimiter, jwtProperties);

        when(loginUseCase.login("admin", "admin123")).thenReturn(
                new LoginUseCase.AuthResult("signed.jwt.token", "admin", "Administrador", Role.ADMIN));
    }

    private String sameSiteOf(MockHttpServletResponse response) {
        return ((MockCookie) response.getCookie(COOKIE)).getSameSite();
    }

    private MockHttpServletRequest secureRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        request.setScheme("https");
        return request;
    }

    @Test
    void login_overHttps_setsSecureCookie_withSameSiteNone() {
        // Produccion "Escenario A" (Fase2 §3.6): PWA y API en dominios distintos, cookie
        // genuinamente cross-site -- SameSite=None exige Secure, y ahi si hay HTTPS real.
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.login(new LoginRequest("admin", "admin123", true), secureRequest(), response);

        var cookie = response.getCookie(COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo("signed.jwt.token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(sameSiteOf(response)).isEqualTo("None");
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    @Test
    void login_overPlainHttp_setsNonSecureCookie_withSameSiteLax() {
        // Desarrollo local (deploy/docker-compose.yml, NGINX_SITE=local, sin TLS): todo es
        // mismo origen, y un cliente HTTP estricto (no un navegador) nunca reenviaria una
        // cookie Secure sobre http:// -- verificado ejecutando scripts/verify_delivery.py
        // contra el stack local antes de esta correccion (fallaba con 401 en cada llamada
        // autenticada tras el login).
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.login(new LoginRequest("admin", "admin123", true), new MockHttpServletRequest(), response);

        var cookie = response.getCookie(COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getSecure()).isFalse();
        assertThat(sameSiteOf(response)).isEqualTo("Lax");
    }

    @Test
    void login_withRememberTrue_setsCookieMaxAge_matchingJwtExpiration() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.login(new LoginRequest("admin", "admin123", true), secureRequest(), response);

        // 480 minutos (JwtProperties de este test) == 28800 segundos.
        assertThat(response.getCookie(COOKIE).getMaxAge()).isEqualTo(480 * 60);
    }

    @Test
    void login_withRememberFalse_setsSessionCookie_withoutMaxAge() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.login(new LoginRequest("admin", "admin123", false), secureRequest(), response);

        // -1 es el valor de MockHttpServletResponse.Cookie cuando nunca se llamo setMaxAge:
        // el navegador la trata como cookie de sesion, se borra sola al cerrarse.
        assertThat(response.getCookie(COOKIE).getMaxAge()).isEqualTo(-1);
    }

    @Test
    void login_doesNotExposeTheTokenInTheResponseBody() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        var body = controller.login(new LoginRequest("admin", "admin123", true), secureRequest(), response);

        // LoginResponse ya no tiene campo "token" (record de 3 componentes): si alguien lo
        // reintrodujera, este test dejaria de compilar, no solo de fallar en runtime.
        assertThat(body.username()).isEqualTo("admin");
        assertThat(body.fullName()).isEqualTo("Administrador");
        assertThat(body.role()).isEqualTo("ADMIN");
    }

    @Test
    void logout_overHttps_clearsTheCookie_withMaxAgeZero() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.logout(secureRequest(), response);

        var cookie = response.getCookie(COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.getSecure()).isTrue();
    }

    @Test
    void logout_overPlainHttp_clearsTheCookie_withoutSecure() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.logout(new MockHttpServletRequest(), response);

        var cookie = response.getCookie(COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.getSecure()).isFalse();
    }
}
