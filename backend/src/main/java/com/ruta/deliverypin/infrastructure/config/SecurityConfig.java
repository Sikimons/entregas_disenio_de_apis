package com.ruta.deliverypin.infrastructure.config;

import com.ruta.deliverypin.infrastructure.adapter.in.web.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AppProperties appProperties;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AppProperties appProperties
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.appProperties = appProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // El JWT ahora viaja en cookie HttpOnly (JwtAuthenticationFilter/AuthController),
                // no en un header que solo el propio JS del frontend adjunta: eso reabre la
                // superficie CSRF clasica que "stateless + Authorization header" evitaba sin
                // esfuerzo (auditoria tecnica, hallazgo P1). Patron de "doble cookie" (double
                // submit) recomendado por la propia guia de Spring Security para SPA:
                // CookieCsrfTokenRepository escribe el token en una cookie NO HttpOnly
                // ("XSRF-TOKEN") que el JS si puede leer; axios (frontend/src/api/client.ts,
                // withXSRFToken) la reenvia sola como header "X-XSRF-TOKEN" en cada peticion de
                // escritura. Un sitio de terceros que induzca al navegador a mandar la cookie de
                // sesion NO puede leer esa segunda cookie (misma politica de origen), asi que
                // nunca puede reproducir el header -> la peticion falsificada se rechaza.
                // "/auth/login" se excluye porque, sin sesion previa que robar, no hay nada que
                // un CSRF pueda falsificar ahi.
                //
                // sessionAuthenticationStrategy(NullAuthenticatedSessionStrategy) -- no el
                // default (Tanda 4, verificacion end-to-end real; encontrado reproduciendo el
                // bug con logging DEBUG de Spring Security, no en un test unitario, y
                // confirmado leyendo el bytecode real de CsrfConfigurer/SessionManagementFilter,
                // no adivinando): sin esto, CsrfConfigurer crea SU PROPIA CsrfAuthenticationStrategy
                // (getSessionAuthenticationStrategy() interno) y la AGREGA -no reemplaza- a la
                // lista de SessionManagementConfigurer, sin importar lo que se configure ahi.
                // SessionManagementFilter corre en cada peticion y, al ser STATELESS, el
                // repositorio de contexto (NullSecurityContextRepository) siempre reporta que el
                // SecurityContext no venia persistido de antes -- asi que cada autenticacion por
                // JWT (JwtAuthenticationFilter la reconstruye en CADA request, no solo en el
                // login) le parece un login nuevo, y dispara esa estrategia. CsrfAuthenticationStrategy
                // invalida y rota el token CSRF vigente al detectar "un login" (proteccion real
                // contra fijacion de sesion via CSRF), pero aqui se disparaba en cada peticion
                // autenticada, no solo en el login (confirmado: el log "Replaced CSRF Token" salia
                // en el GET posterior, nunca en el POST /auth/login). Nuestro login real ni pasa
                // por la maquinaria de autenticacion de Spring Security (lo maneja
                // AuthController.setAuthCookie() directamente) y no existe ninguna sesion de
                // servidor que proteger de fijacion, asi que esa defensa no aplica aqui. Debe
                // fijarse aqui, en CsrfConfigurer -- fijarlo solo en .sessionManagement(...) no
                // alcanza, porque CsrfConfigurer nunca lee ese valor para decidir si crear la suya.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                        .ignoringRequestMatchers("/api/v1/auth/login")
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"message\":\"No tienes permiso para realizar esta accion.\"}");
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/driver/**").hasAnyRole("ADMIN", "CONDUCTOR")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // El token CSRF se genera de forma diferida (Spring Security 6): sin forzar su
                // lectura aqui, la cookie "XSRF-TOKEN" nunca llega a escribirse en la respuesta
                // si nada del propio request la pide antes. Este filtro solo la "toca" para que
                // se renderice en cada respuesta, sin cambiar el flujo de la peticion.
                .addFilterAfter(new CsrfCookieFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Con app.cookie.domain (p. ej. "midominio.com"), XSRF-TOKEN se emite para todo el
    // dominio: el frontend en app.midominio.com puede leerla con document.cookie aunque la
    // API viva en api.midominio.com (mismo sitio, SameSite no la bloquea). Sin el valor
    // (local, mismo origen) queda host-only, como antes.
    CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        String domain = appProperties.getCookie().getDomain();
        if (domain != null && !domain.isBlank()) {
            repository.setCookieCustomizer(cookie -> cookie.domain(domain.trim()));
        }
        return repository;
    }

    private static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"No autenticado. Inicia sesion nuevamente.\"}");
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        String origins = appProperties.getCors().getAllowedOrigins();
        // Patrones (no solo origenes exactos) para admitir las URLs de preview por PR de
        // Cloudflare Pages en staging, p. ej. "https://*.ruta-staging.pages.dev".
        configuration.setAllowedOriginPatterns(Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
