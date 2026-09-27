package com.ruta.deliverypin.infrastructure.adapter.in.web.security;

import com.ruta.deliverypin.domain.exception.DriverNotFoundException;
import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.model.Role;
import com.ruta.deliverypin.domain.port.in.FindDriverByUsernameUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Resuelve la identidad real del usuario autenticado: lo usan casi todos los controladores
 * (confirmar entrega, reportar incidencia, crear/publicar factura). Auditoria tecnica,
 * Tanda 2: quedaba en 25% de cobertura sin ningun test propio.
 */
class CurrentDriverResolverTest {

    private final FindDriverByUsernameUseCase findDriverByUsernameUseCase = mock(FindDriverByUsernameUseCase.class);
    private final CurrentDriverResolver resolver = new CurrentDriverResolver(findDriverByUsernameUseCase);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String username) {
        var auth = new UsernamePasswordAuthenticationToken(username, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void resolve_authenticatedUsernameExists_returnsTheDomainDriver() {
        authenticateAs("conductor1");
        Driver driver = new Driver(1L, "conductor1", "hash", "Conductor Uno", Role.CONDUCTOR, true, Instant.now());
        when(findDriverByUsernameUseCase.findByUsername("conductor1")).thenReturn(Optional.of(driver));

        Driver resolved = resolver.resolve();

        assertThat(resolved.getId()).isEqualTo(1L);
        assertThat(resolved.getUsername()).isEqualTo("conductor1");
    }

    @Test
    void resolve_usernameNotFound_throwsDriverNotFound() {
        // Borde real: el token JWT era valido cuando se emitio, pero la cuenta se elimino
        // despues (o el username cambio) antes de que el token expirara.
        authenticateAs("fantasma");
        when(findDriverByUsernameUseCase.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(resolver::resolve).isInstanceOf(DriverNotFoundException.class);
    }
}
