package com.ruta.deliverypin.application;

import com.ruta.deliverypin.domain.exception.InvalidCredentialsException;
import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.model.Role;
import com.ruta.deliverypin.domain.port.out.DriverRepositoryPort;
import com.ruta.deliverypin.domain.port.out.PasswordEncoderPort;
import com.ruta.deliverypin.domain.port.out.TokenProviderPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

    @Mock
    private DriverRepositoryPort driverRepository;
    @Mock
    private PasswordEncoderPort passwordEncoder;
    @Mock
    private TokenProviderPort tokenProvider;

    private AuthApplicationService service;

    private Driver activeAdmin() {
        return new Driver(1L, "admin", "hashed", "Administrador", Role.ADMIN, true, Instant.now());
    }

    @Test
    void login_validCredentials_returnsTokenAndProfile() {
        service = new AuthApplicationService(driverRepository, passwordEncoder, tokenProvider);
        Driver admin = activeAdmin();
        when(driverRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("admin123", "hashed")).thenReturn(true);
        when(tokenProvider.generateToken(admin)).thenReturn("signed-token");

        var result = service.login("admin", "admin123");

        assertThat(result.token()).isEqualTo("signed-token");
        assertThat(result.username()).isEqualTo("admin");
        assertThat(result.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        service = new AuthApplicationService(driverRepository, passwordEncoder, tokenProvider);
        Driver admin = activeAdmin();
        when(driverRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.login("admin", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_unknownUser_throwsInvalidCredentials() {
        service = new AuthApplicationService(driverRepository, passwordEncoder, tokenProvider);
        when(driverRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("ghost", "whatever"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_inactiveUser_throwsInvalidCredentials() {
        service = new AuthApplicationService(driverRepository, passwordEncoder, tokenProvider);
        Driver inactive = new Driver(2L, "conductor1", "hashed", "Conductor Uno", Role.CONDUCTOR, false, Instant.now());
        when(driverRepository.findByUsername("conductor1")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.login("conductor1", "any"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
