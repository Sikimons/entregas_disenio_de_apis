package com.ruta.deliverypin.infrastructure.adapter.in.web.security;

import com.ruta.deliverypin.domain.port.out.TokenProviderPort;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre la extraccion del JWT desde la cookie HttpOnly (auditoria tecnica, Tanda 1): antes
 * este filtro leia el header "Authorization: Bearer ..."; migrarlo a
 * request.getCookies() sin ningun test dedicado dejaba sin cubrir justo las ramas que mas
 * importan (sin cookies, cookie ausente entre varias, valor en blanco).
 */
class JwtAuthenticationFilterTest {

    private final TokenProviderPort tokenProvider = mock(TokenProviderPort.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    private void runFilter(MockHttpServletRequest request) throws Exception {
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    @Test
    void doFilter_noCookiesAtAll_leavesRequestUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/users");

        runFilter(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenProvider, never()).extractUsername(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doFilter_unrelatedCookiePresent_isIgnored() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/users");
        request.setCookies(new Cookie("otra_cookie", "algo"));

        runFilter(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_blankCookieValue_isTreatedAsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/users");
        request.setCookies(new Cookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, ""));

        runFilter(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenProvider, never()).extractUsername(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doFilter_validCookie_authenticatesTheUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/users");
        request.setCookies(new Cookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, "signed.jwt.token"));
        when(tokenProvider.extractUsername("signed.jwt.token")).thenReturn(Optional.of("admin"));
        when(tokenProvider.isValid("signed.jwt.token", "admin")).thenReturn(true);
        when(userDetailsService.loadUserByUsername("admin"))
                .thenReturn(new User("admin", "hash", java.util.List.of()));

        runFilter(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("admin");
    }

    @Test
    void doFilter_disabledUser_isNotAuthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/users");
        request.setCookies(new Cookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, "signed.jwt.token"));
        when(tokenProvider.extractUsername("signed.jwt.token")).thenReturn(Optional.of("admin"));
        when(tokenProvider.isValid("signed.jwt.token", "admin")).thenReturn(true);
        when(userDetailsService.loadUserByUsername("admin"))
                .thenReturn(new User("admin", "hash", false, true, true, true, java.util.List.of()));

        runFilter(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_multipleCookies_findsTheAccessTokenAmongThem() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/users");
        request.setCookies(
                new Cookie("XSRF-TOKEN", "csrf-value"),
                new Cookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, "signed.jwt.token")
        );
        when(tokenProvider.extractUsername("signed.jwt.token")).thenReturn(Optional.of("admin"));
        when(tokenProvider.isValid("signed.jwt.token", "admin")).thenReturn(true);
        when(userDetailsService.loadUserByUsername("admin"))
                .thenReturn(new User("admin", "hash", java.util.List.of()));

        runFilter(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }
}
