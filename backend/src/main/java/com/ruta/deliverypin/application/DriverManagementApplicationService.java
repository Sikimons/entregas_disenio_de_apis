package com.ruta.deliverypin.application;

import com.ruta.deliverypin.domain.exception.DriverAlreadyExistsException;
import com.ruta.deliverypin.domain.exception.DriverNotFoundException;
import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.port.in.CreateDriverUseCase;
import com.ruta.deliverypin.domain.port.in.DeleteDriverUseCase;
import com.ruta.deliverypin.domain.port.in.FindDriverByUsernameUseCase;
import com.ruta.deliverypin.domain.port.in.ListDriversUseCase;
import com.ruta.deliverypin.domain.port.in.UpdateDriverUseCase;
import com.ruta.deliverypin.domain.port.out.DriverRepositoryPort;
import com.ruta.deliverypin.domain.port.out.PasswordEncoderPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DriverManagementApplicationService implements
        CreateDriverUseCase, UpdateDriverUseCase, DeleteDriverUseCase, ListDriversUseCase, FindDriverByUsernameUseCase {

    private final DriverRepositoryPort driverRepository;
    private final PasswordEncoderPort passwordEncoder;

    public DriverManagementApplicationService(DriverRepositoryPort driverRepository, PasswordEncoderPort passwordEncoder) {
        this.driverRepository = driverRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Driver create(CreateDriverCommand command) {
        if (driverRepository.existsByUsername(command.username())) {
            throw new DriverAlreadyExistsException(command.username());
        }
        Driver driver = Driver.createNew(
                command.username(),
                passwordEncoder.encode(command.rawPassword()),
                command.fullName(),
                command.role()
        );
        return driverRepository.save(driver);
    }

    @Override
    public Driver update(Long id, UpdateDriverCommand command) {
        Driver existing = driverRepository.findById(id)
                .orElseThrow(() -> new DriverNotFoundException(id));

        String passwordHash = command.rawPassword() != null && !command.rawPassword().isBlank()
                ? passwordEncoder.encode(command.rawPassword())
                : existing.getPasswordHash();

        Driver updated = existing.withUpdatedProfile(command.fullName(), command.active(), passwordHash);
        return driverRepository.save(updated);
    }

    @Override
    public void delete(Long id) {
        if (driverRepository.findById(id).isEmpty()) {
            throw new DriverNotFoundException(id);
        }
        driverRepository.deleteById(id);
    }

    @Override
    public List<Driver> listAll() {
        return driverRepository.findAll();
    }

    @Override
    public Optional<Driver> findByUsername(String username) {
        return driverRepository.findByUsername(username);
    }
}
