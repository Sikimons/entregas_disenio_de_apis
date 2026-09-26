package com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository;

import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.DeliveryAttemptJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SpringDataDeliveryAttemptJpaRepository extends JpaRepository<DeliveryAttemptJpaEntity, Long> {
    Page<DeliveryAttemptJpaEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<DeliveryAttemptJpaEntity> findAllByDriver_IdOrderByCreatedAtDesc(Long driverId, Pageable pageable);

    List<DeliveryAttemptJpaEntity> findAllByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);

    @Query("select a.photo as photo, a.photoContentType as photoContentType " +
            "from DeliveryAttemptJpaEntity a where a.id = :id and a.photo is not null")
    Optional<PhotoProjection> findPhotoById(@Param("id") Long id);

    @Query("select a.photo as photo, a.photoContentType as photoContentType " +
            "from DeliveryAttemptJpaEntity a where a.id = :id and a.driver.id = :driverId and a.photo is not null")
    Optional<PhotoProjection> findPhotoByIdAndDriverId(@Param("id") Long id, @Param("driverId") Long driverId);

    interface PhotoProjection {
        byte[] getPhoto();
        String getPhotoContentType();
    }
}
