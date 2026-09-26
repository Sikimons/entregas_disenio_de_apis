package com.ruta.deliverypin.infrastructure.adapter.out.persistence;

import com.ruta.deliverypin.domain.model.DeliveryAttempt;
import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.model.GeoLocation;
import com.ruta.deliverypin.domain.model.StoredPhoto;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.DeliveryAttemptJpaEntity;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.DriverJpaEntity;

final class DeliveryAttemptPersistenceMapper {

    private DeliveryAttemptPersistenceMapper() {
    }

    static DeliveryAttempt toDomain(DeliveryAttemptJpaEntity entity) {
        DriverJpaEntity driverEntity = entity.getDriver();
        Driver driver = DriverPersistenceMapper.toDomain(driverEntity);
        GeoLocation location = entity.getLatitude() != null && entity.getLongitude() != null
                ? new GeoLocation(entity.getLatitude(), entity.getLongitude())
                : null;
        StoredPhoto photo = entity.getPhoto() != null
                ? new StoredPhoto(entity.getPhoto(), entity.getPhotoContentType())
                : null;

        return DeliveryAttempt.reconstitute(
                entity.getId(),
                entity.getInvoiceId(),
                entity.getInvoiceNumber(),
                entity.getPartnerName(),
                entity.getDeliveryAddress(),
                driver,
                entity.getOutcome(),
                location,
                entity.getDetail(),
                photo,
                entity.getDistanceFromExpectedMeters(),
                entity.getCreatedAt()
        );
    }

    static DeliveryAttemptJpaEntity toEntity(DeliveryAttempt attempt) {
        DriverJpaEntity driverReference = DriverPersistenceMapper.toEntity(attempt.getDriver());
        GeoLocation location = attempt.getLocation();
        StoredPhoto photo = attempt.getPhoto();

        return new DeliveryAttemptJpaEntity(
                attempt.getId(),
                attempt.getInvoiceId(),
                attempt.getInvoiceNumber(),
                attempt.getPartnerName(),
                attempt.getDeliveryAddress(),
                driverReference,
                attempt.getOutcome(),
                location != null ? location.latitude() : null,
                location != null ? location.longitude() : null,
                attempt.getDetail(),
                photo != null ? photo.data() : null,
                photo != null ? photo.contentType() : null,
                attempt.getDistanceFromExpectedMeters(),
                attempt.getCreatedAt()
        );
    }
}
