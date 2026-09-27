package com.ruta.deliverypin.domain.port.out;

import com.ruta.deliverypin.domain.model.DeliveryAttempt;
import com.ruta.deliverypin.domain.model.DeliveryAttemptSummary;
import com.ruta.deliverypin.domain.model.DeliveryVolumeSummary;
import com.ruta.deliverypin.domain.model.DriverMetric;
import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.domain.model.PageResult;
import com.ruta.deliverypin.domain.model.StoredPhoto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para la auditoria (log) de intentos de entrega.
 */
public interface DeliveryAttemptRepositoryPort {

    DeliveryAttempt save(DeliveryAttempt attempt);

    /** Resumen (sin foto) para listados: ver DeliveryAttemptSummary. */
    PageResult<DeliveryAttemptSummary> findAllOrderByCreatedAtDesc(PageRequest pageRequest);

    PageResult<DeliveryAttemptSummary> findAllByDriverOrderByCreatedAtDesc(Long driverId, PageRequest pageRequest);

    Optional<StoredPhoto> findPhotoByAttemptId(Long attemptId);

    Optional<StoredPhoto> findPhotoByAttemptIdAndDriverId(Long attemptId, Long driverId);

    /** Sin paginar: para el mapa del admin, acotado por rango de fechas. */
    List<DeliveryAttemptSummary> findAllBetween(Instant from, Instant to);

    /**
     * Metricas por conductor agregadas en SQL (GROUP BY driver, outcome), no en memoria
     * Java: evita traer cada intento (y su foto) solo para contar.
     */
    List<DriverMetric> metricsBetween(Instant from, Instant to);

    /**
     * Conteo de entregas confirmadas y bytes de evidencia, calculado en la base de datos
     * (sin traer el contenido de las fotos). Insumo del costo por entrega verificada (showback).
     */
    DeliveryVolumeSummary summarizeConfirmedBetween(Instant from, Instant to);
}
