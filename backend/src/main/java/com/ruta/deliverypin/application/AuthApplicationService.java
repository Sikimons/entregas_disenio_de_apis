package com.ruta.deliverypin.application;

import com.ruta.deliverypin.domain.exception.InvalidCredentialsException;
import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.port.in.LoginUseCase;
import com.ruta.deliverypin.domain.port.out.DriverRepositoryPort;
import com.ruta.deliverypin.domain.port.out.PasswordEncoderPort;
import com.ruta.deliverypin.domain.port.out.TokenProviderPort;
import org.springframework.stereotype.Service;

@Service
public class AuthApplicationService implements LoginUseCase {

    private final DriverRepositoryPort driverRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;

    public AuthApplicationService(DriverRepositoryPort driverRepository, PasswordEncoderPort passwordEncoder, TokenProviderPort tokenProvider) {
        this.driverRepository = driverRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public AuthResult login(String username, String rawPassword) {
        Driver driver = driverRepository.findByUsername(username)
                .filter(Driver::isActive)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(rawPassword, driver.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = tokenProvider.generateToken(driver);
        return new AuthResult(token, driver.getUsername(), driver.getFullName(), driver.getRole());
    }
}
