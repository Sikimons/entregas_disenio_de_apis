package com.ruta.deliverypin.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regresion real (Tanda 4, auditoria tecnica): reproduce exactamente el bug encontrado al
 * probar el stack completo con docker compose (no algo que un test unitario aislado
 * hubiera detectado -- la reproduccion con MockHttpServletRequest/Response NO lo mostraba,
 * solo aparecia con el contenedor Tomcat real).
 *
 * Causa raiz (confirmada leyendo el bytecode real de CsrfConfigurer/SessionManagementFilter
 * de Spring Security, no adivinando): sin fijar CsrfConfigurer.sessionAuthenticationStrategy()
 * explicitamente, cada peticion autenticada por cookie (JwtAuthenticationFilter reconstruye
 * la identidad en CADA request, no solo en el login) le parecia "un login nuevo" a
 * SessionManagementFilter -- porque el repositorio de contexto STATELESS nunca reporta que
 * ya habia una autenticacion persistida -- y disparaba CsrfAuthenticationStrategy, que
 * invalidaba el token CSRF vigente en la primera peticion autenticada despues de cada login.
 * Esto rompia scripts/verify_delivery.py (y cualquier cliente real que respete CSRF) con 403
 * en la primera escritura tras el login.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CsrfCookieSurvivesAcrossRequestsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "test_secret_for_testcontainers_integration_min_32_chars_long");
        registry.add("app.admin.bootstrap-password", () -> "testcontainers_admin_password");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static String extractCookieValue(List<String> setCookieHeaders, String name) {
        Pattern pattern = Pattern.compile(name + "=([^;]*)");
        for (String header : setCookieHeaders) {
            Matcher matcher = pattern.matcher(header);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    @Test
    void xsrfCookie_survives_acrossMultipleAuthenticatedRequests_afterLogin() {
        // 1) Login real (no un mock): AuthController.setAuthCookie() emite "access_token" y
        // el propio filtro CSRF, forzado por CsrfCookieFilter, emite "XSRF-TOKEN" en esta
        // misma respuesta.
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/auth/login",
                new org.springframework.http.HttpEntity<>(
                        "{\"username\":\"admin\",\"password\":\"testcontainers_admin_password\",\"remember\":true}",
                        headersWithContentType()),
                String.class);
        assertThat(loginResponse.getStatusCode().value()).isEqualTo(200);

        List<String> loginCookies = loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(loginCookies).isNotNull();
        String accessToken = extractCookieValue(loginCookies, "access_token");
        String xsrfAfterLogin = extractCookieValue(loginCookies, "XSRF-TOKEN");
        assertThat(accessToken).isNotBlank();
        assertThat(xsrfAfterLogin).isNotBlank();

        // 2) Primera peticion autenticada (GET, no exige CSRF, pero es la que antes borraba
        // la cookie -- CsrfCookieFilter la fuerza a "renderizarse" en toda respuesta).
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HttpHeaders.COOKIE, "access_token=" + accessToken + "; XSRF-TOKEN=" + xsrfAfterLogin);
        ResponseEntity<String> firstGet = restTemplate.exchange(
                RequestEntity.get(URI.create("http://localhost:" + port + "/api/v1/admin/users"))
                        .headers(requestHeaders).build(),
                String.class);
        assertThat(firstGet.getStatusCode().value()).isEqualTo(200);

        List<String> firstGetCookies = firstGet.getHeaders().get(HttpHeaders.SET_COOKIE);
        String xsrfAfterFirstGet = (firstGetCookies != null) ? extractCookieValue(firstGetCookies, "XSRF-TOKEN") : null;
        // El bug real: aqui llegaba "" con Max-Age=0 (cookie borrada). Debe seguir siendo el
        // mismo token (o no reescribirse), nunca vaciarse.
        assertThat(xsrfAfterFirstGet == null || xsrfAfterFirstGet.equals(xsrfAfterLogin))
                .as("XSRF-TOKEN no debe invalidarse en una peticion autenticada normal (visto: '%s')", xsrfAfterFirstGet)
                .isTrue();

        // 3) Una escritura real con esa misma clave CSRF debe funcionar (esto es lo que
        // scripts/verify_delivery.py hacia y fallaba con 403 antes de esta correccion).
        HttpHeaders writeHeaders = new HttpHeaders();
        writeHeaders.setContentType(MediaType.APPLICATION_JSON);
        writeHeaders.add(HttpHeaders.COOKIE, "access_token=" + accessToken + "; XSRF-TOKEN=" + xsrfAfterLogin);
        writeHeaders.add("X-XSRF-TOKEN", xsrfAfterLogin);
        ResponseEntity<String> create = restTemplate.exchange(
                RequestEntity.method(HttpMethod.POST, URI.create("http://localhost:" + port + "/api/v1/admin/invoices"))
                        .headers(writeHeaders)
                        .body("{\"number\":\"CSRF-REGRESSION-TEST\",\"partnerName\":\"x\",\"deliveryAddress\":\"y\","
                                + "\"latitude\":-2.1,\"longitude\":-79.9,\"requiresPin\":true,"
                                + "\"products\":[{\"description\":\"p\",\"quantity\":1}]}"),
                String.class);
        assertThat(create.getStatusCode().value()).isEqualTo(201);
    }

    private HttpHeaders headersWithContentType() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
