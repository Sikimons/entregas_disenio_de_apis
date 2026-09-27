package com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository;

import com.ruta.deliverypin.domain.model.DeliveryAttemptOutcome;
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

    String SUMMARY_SELECT = "select a.id as id, a.invoiceId as invoiceId, a.invoiceNumber as invoiceNumber, " +
            "a.partnerName as partnerName, a.deliveryAddress as deliveryAddress, a.driver.fullName as driverName, " +
            "a.outcome as outcome, a.latitude as latitude, a.longitude as longitude, a.detail as detail, " +
            "(case when a.photo is null then false else true end) as hasPhoto, " +
            "a.distanceFromExpectedMeters as distanceFromExpectedMeters, a.createdAt as createdAt " +
            "from DeliveryAttemptJpaEntity a ";

    /**
     * Version paginada/de rango SIN la columna `photo`: antes se traia el entity completo
     * (incluida la foto, potencialmente cientos de KB por fila) solo para descartarla al
     * armar el DTO. Verificado con carga real (load-tests/dashboard-heavy.js): con 500
     * entregas confirmadas con foto de ~200KB, esta consulta es la diferencia entre un p95
     * de ~22s (con el entity completo) y unos pocos ms (con la proyeccion).
     */
    @Query(value = SUMMARY_SELECT + "order by a.createdAt desc",
            countQuery = "select count(a) from DeliveryAttemptJpaEntity a")
    Page<AttemptSummaryProjection> findSummaries(Pageable pageable);

    @Query(value = SUMMARY_SELECT + "where a.driver.id = :driverId order by a.createdAt desc",
            countQuery = "select count(a) from DeliveryAttemptJpaEntity a where a.driver.id = :driverId")
    Page<AttemptSummaryProjection> findSummariesByDriverId(@Param("driverId") Long driverId, Pageable pageable);

    @Query(SUMMARY_SELECT + "where a.createdAt between :from and :to order by a.createdAt desc")
    List<AttemptSummaryProjection> findSummariesByCreatedAtBetween(@Param("from") Instant from, @Param("to") Instant to);

    /** Metricas agregadas en SQL (GROUP BY conductor, resultado): no trae cada intento a Java. */
    @Query("select d.fullName as driverName, a.outcome as outcome, count(a) as total " +
            "from DeliveryAttemptJpaEntity a join a.driver d " +
            "where a.createdAt between :from and :to group by d.fullName, a.outcome")
    List<DriverOutcomeCountProjection> countByDriverAndOutcomeBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select a.photo as photo, a.photoContentType as photoContentType " +
            "from DeliveryAttemptJpaEntity a where a.id = :id and a.photo is not null")
    Optional<PhotoProjection> findPhotoById(@Param("id") Long id);

    @Query("select a.photo as photo, a.photoContentType as photoContentType " +
            "from DeliveryAttemptJpaEntity a where a.id = :id and a.driver.id = :driverId and a.photo is not null")
    Optional<PhotoProjection> findPhotoByIdAndDriverId(@Param("id") Long id, @Param("driverId") Long driverId);

    /**
     * Conteo y bytes de evidencia agregados en la base de datos, sin traer el contenido de
     * las fotos a memoria. Insumo del costo por entrega verificada (showback).
     */
    @Query(value = "SELECT count(*) AS confirmed, coalesce(sum(octet_length(photo)), 0) AS photoBytes " +
            "FROM delivery_log WHERE outcome = 'CONFIRMED' AND created_at >= :from AND created_at < :to",
            nativeQuery = true)
    VolumeProjection summarizeConfirmedBetween(@Param("from") Instant from, @Param("to") Instant to);

    interface PhotoProjection {
        byte[] getPhoto();
        String getPhotoContentType();
    }

    interface VolumeProjection {
        long getConfirmed();
        long getPhotoBytes();
    }

    interface AttemptSummaryProjection {
        Long getId();
        Long getInvoiceId();
        String getInvoiceNumber();
        String getPartnerName();
        String getDeliveryAddress();
        String getDriverName();
        DeliveryAttemptOutcome getOutcome();
        Double getLatitude();
        Double getLongitude();
        String getDetail();
        Boolean getHasPhoto();
        Double getDistanceFromExpectedMeters();
        Instant getCreatedAt();
    }

    interface DriverOutcomeCountProjection {
        String getDriverName();
        DeliveryAttemptOutcome getOutcome();
        long getTotal();
    }
}
