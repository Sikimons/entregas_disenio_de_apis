package com.ruta.deliverypin.infrastructure.adapter.out.persistence;

import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.DriverJpaEntity;

final class DriverPersistenceMapper {

    private DriverPersistenceMapper() {
    }

    static Driver toDomain(DriverJpaEntity entity) {
        return new Driver(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getFullName(),
                entity.getRole(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }

    static DriverJpaEntity toEntity(Driver driver) {
        return new DriverJpaEntity(
                driver.getId(),
                driver.getUsername(),
                driver.getPasswordHash(),
                driver.getFullName(),
                driver.getRole(),
                driver.isActive(),
                driver.getCreatedAt()
        );
    }
}
