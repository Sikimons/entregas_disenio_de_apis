package com.ruta.deliverypin.infrastructure.adapter.out.persistence;

import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.port.out.DriverRepositoryPort;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.DriverJpaEntity;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository.SpringDataDriverJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida: implementa el puerto DriverRepositoryPort usando Spring Data JPA.
 */
@Component
public class DriverRepositoryAdapter implements DriverRepositoryPort {

    private final SpringDataDriverJpaRepository jpaRepository;

    public DriverRepositoryAdapter(SpringDataDriverJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Driver> findByUsername(String username) {
        return jpaRepository.findByUsernameIgnoreCase(username).map(DriverPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Driver> findById(Long id) {
        return jpaRepository.findById(id).map(DriverPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsernameIgnoreCase(username);
    }

    @Override
    public List<Driver> findAll() {
        return jpaRepository.findAll().stream().map(DriverPersistenceMapper::toDomain).toList();
    }

    @Override
    public Driver save(Driver driver) {
        DriverJpaEntity saved = jpaRepository.save(DriverPersistenceMapper.toEntity(driver));
        return DriverPersistenceMapper.toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
