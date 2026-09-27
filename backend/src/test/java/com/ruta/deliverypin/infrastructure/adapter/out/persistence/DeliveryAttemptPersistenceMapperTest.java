package com.ruta.deliverypin.infrastructure.adapter.out.persistence;

import com.ruta.deliverypin.domain.model.DeliveryAttempt;
import com.ruta.deliverypin.domain.model.DeliveryAttemptOutcome;
import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.model.Role;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.DeliveryAttemptJpaEntity;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.DriverJpaEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryAttemptPersistenceMapperTest {

    private DriverJpaEntity driverEntity() {
        return new DriverJpaEntity(1L, "conductor1", "hash", "Conductor Uno", Role.CONDUCTOR, true, Instant.now());
    }

    @Test
    void toDomain_withLocationAndPhoto_mapsEveryField() {
        Instant createdAt = Instant.parse("2026-09-01T10:00:00Z");
        DeliveryAttemptJpaEntity entity = new DeliveryAttemptJpaEntity(
                10L, 5L, "046-101-000005884", "Cliente Uno", "Av. Siempre Viva 123",
                driverEntity(), DeliveryAttemptOutcome.CONFIRMED, -2.17, -79.92, null,
                new byte[]{1, 2, 3}, "image/jpeg", 42.5, createdAt
        );

        DeliveryAttempt attempt = DeliveryAttemptPersistenceMapper.toDomain(entity);

        assertThat(attempt.getId()).isEqualTo(10L);
        assertThat(attempt.getInvoiceId()).isEqualTo(5L);
        assertThat(attempt.getInvoiceNumber()).isEqualTo("046-101-000005884");
        assertThat(attempt.getPartnerName()).isEqualTo("Cliente Uno");
        assertThat(attempt.getDeliveryAddress()).isEqualTo("Av. Siempre Viva 123");
        assertThat(attempt.getDriver().getUsername()).isEqualTo("conductor1");
        assertThat(attempt.getOutcome()).isEqualTo(DeliveryAttemptOutcome.CONFIRMED);
        assertThat(attempt.getLocation().latitude()).isEqualTo(-2.17);
        assertThat(attempt.getLocation().longitude()).isEqualTo(-79.92);
        assertThat(attempt.getPhoto().data()).containsExactly(1, 2, 3);
        assertThat(attempt.getPhoto().contentType()).isEqualTo("image/jpeg");
        assertThat(attempt.getDistanceFromExpectedMeters()).isEqualTo(42.5);
        assertThat(attempt.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void toDomain_withoutLocationOrPhoto_mapsNullsInsteadOfEmptyObjects() {
        DeliveryAttemptJpaEntity entity = new DeliveryAttemptJpaEntity(
                11L, 6L, "046-101-000005885", "Cliente Dos", null,
                driverEntity(), DeliveryAttemptOutcome.INCIDENT, null, null, "Cliente ausente",
                null, null, null, Instant.now()
        );

        DeliveryAttempt attempt = DeliveryAttemptPersistenceMapper.toDomain(entity);

        assertThat(attempt.getLocation()).isNull();
        assertThat(attempt.getPhoto()).isNull();
        assertThat(attempt.getDetail()).isEqualTo("Cliente ausente");
    }

    @Test
    void toEntity_roundTripsThroughToDomain() {
        Driver driver = new Driver(2L, "conductor2", "hash", "Conductor Dos", Role.CONDUCTOR, true, Instant.now());
        DeliveryAttempt original = DeliveryAttempt.succeeded(
                7L, "046-101-000005886", "Cliente Tres", "Direccion Tres",
                driver, new com.ruta.deliverypin.domain.model.GeoLocation(-2.0, -79.0),
                new com.ruta.deliverypin.domain.model.StoredPhoto(new byte[]{9, 8, 7}, "image/png"), 12.0
        );

        DeliveryAttemptJpaEntity entity = DeliveryAttemptPersistenceMapper.toEntity(original);

        assertThat(entity.getInvoiceId()).isEqualTo(7L);
        assertThat(entity.getInvoiceNumber()).isEqualTo("046-101-000005886");
        assertThat(entity.getOutcome()).isEqualTo(DeliveryAttemptOutcome.CONFIRMED);
        assertThat(entity.getLatitude()).isEqualTo(-2.0);
        assertThat(entity.getLongitude()).isEqualTo(-79.0);
        assertThat(entity.getPhoto()).containsExactly(9, 8, 7);
        assertThat(entity.getPhotoContentType()).isEqualTo("image/png");
        assertThat(entity.getDistanceFromExpectedMeters()).isEqualTo(12.0);
    }
}
