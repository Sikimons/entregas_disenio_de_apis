package com.ruta.deliverypin.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSRF-TOKEN debe llevar Domain=<app.cookie.domain> cuando frontend y API viven en
 * subdominios distintos (app.x / api.x): sin eso, document.cookie del frontend no la ve
 * y axios nunca manda X-XSRF-TOKEN (todo POST/PUT/DELETE responde 403).
 */
class SecurityConfigCsrfCookieTest {

    private static String xsrfSetCookie(String domain) {
        AppProperties properties = new AppProperties();
        properties.getCookie().setDomain(domain);
        CookieCsrfTokenRepository repository = new SecurityConfig(null, properties).csrfTokenRepository();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CsrfToken token = repository.generateToken(request);
        repository.saveToken(token, request, response);
        return response.getHeader("Set-Cookie");
    }

    @Test
    void withDomain_cookieIsScopedToThatDomainAndReadableByJs() {
        String header = xsrfSetCookie("midominio.com");

        assertThat(header).startsWith("XSRF-TOKEN=");
        assertThat(header).contains("Domain=midominio.com");
        assertThat(header).doesNotContain("HttpOnly");
    }

    @Test
    void withoutDomain_cookieStaysHostOnly() {
        assertThat(xsrfSetCookie(null)).doesNotContain("Domain=");
        assertThat(xsrfSetCookie("  ")).doesNotContain("Domain=");
    }
}
