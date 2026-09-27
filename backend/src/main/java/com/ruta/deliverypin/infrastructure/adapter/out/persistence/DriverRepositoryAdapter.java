package com.ruta.deliverypin.infrastructure.adapter.out.persistence;

import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.domain.model.PageResult;
import com.ruta.deliverypin.domain.port.out.DriverRepositoryPort;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.DriverJpaEntity;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository.SpringDataDriverJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

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
    public PageResult<Driver> findAll(PageRequest pageRequest) {
        Page<DriverJpaEntity> page = jpaRepository.findAll(
                org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size(), Sort.by("id"))
        );
        return new PageResult<>(
                page.getContent().stream().map(DriverPersistenceMapper::toDomain).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()
        );
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
