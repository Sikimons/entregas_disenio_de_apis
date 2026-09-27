package com.ruta.deliverypin.infrastructure.adapter.in.web;

import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.domain.model.PageResult;
import com.ruta.deliverypin.domain.model.Role;
import com.ruta.deliverypin.domain.port.in.CreateDriverUseCase;
import com.ruta.deliverypin.domain.port.in.DeleteDriverUseCase;
import com.ruta.deliverypin.domain.port.in.ListDriversUseCase;
import com.ruta.deliverypin.domain.port.in.UpdateDriverUseCase;
import com.ruta.deliverypin.domain.port.out.TokenProviderPort;
import com.ruta.deliverypin.infrastructure.adapter.in.web.security.CurrentDriverResolver;
import com.ruta.deliverypin.infrastructure.adapter.in.web.security.JwtAuthenticationFilter;
import com.ruta.deliverypin.infrastructure.adapter.in.web.security.SpringSecurityUserDetailsAdapter;
import com.ruta.deliverypin.infrastructure.config.AppProperties;
import com.ruta.deliverypin.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de la cadena de seguridad real (SecurityConfig) sobre un endpoint /api/v1/admin/**:
 * sin autenticacion -> 401; rol equivocado -> 403; rol correcto -> 200.
 */
@WebMvcTest(AdminDriverController.class)
@Import({SecurityConfig.class, AdminDriverControllerWebMvcTest.SecurityTestConfig.class})
class AdminDriverControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateDriverUseCase createDriverUseCase;
    @MockBean
    private UpdateDriverUseCase updateDriverUseCase;
    @MockBean
    private DeleteDriverUseCase deleteDriverUseCase;
    @MockBean
    private ListDriversUseCase listDriversUseCase;

    @MockBean
    private SpringSecurityUserDetailsAdapter userDetailsService;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @MockBean
    private CurrentDriverResolver currentDriverResolver;

    @Test
    void list_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CONDUCTOR")
    void list_withWrongRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_withAdminRole_returns200() throws Exception {
        Driver driver = new Driver(1L, "conductor1", "hash", "Conductor Uno", Role.CONDUCTOR, true, Instant.now());
        when(listDriversUseCase.listAll(any(PageRequest.class)))
                .thenReturn(new PageResult<>(List.of(driver), 0, 100, 1, 1));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    static class SecurityTestConfig {

        /** AppProperties real (no mock) para que corsConfigurationSource() no reciba null. */
        @Bean
        AppProperties appProperties() {
            AppProperties properties = new AppProperties();
            AppProperties.Cors cors = new AppProperties.Cors();
            cors.setAllowedOrigins("http://localhost:5173");
            properties.setCors(cors);
            return properties;
        }

        /**
         * Instancia real (no @MockBean): mockear la clase del filtro sobreescribe tambien
         * su doFilter heredado de OncePerRequestFilter, cortando la cadena antes de llegar
         * a la autorizacion real. Estas dependencias nunca se invocan en las pruebas porque
         * ninguna peticion trae cabecera Authorization (doFilterInternal delega directo).
         */
        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            return new JwtAuthenticationFilter(mock(TokenProviderPort.class), mock(UserDetailsService.class));
        }
    }
}
