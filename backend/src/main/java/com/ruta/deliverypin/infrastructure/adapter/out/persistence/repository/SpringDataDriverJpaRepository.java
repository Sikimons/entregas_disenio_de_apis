package com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository;

import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.DriverJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataDriverJpaRepository extends JpaRepository<DriverJpaEntity, Long> {
    Optional<DriverJpaEntity> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
}
