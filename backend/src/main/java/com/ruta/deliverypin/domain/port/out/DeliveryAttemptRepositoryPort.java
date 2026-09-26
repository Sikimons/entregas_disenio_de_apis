package com.ruta.deliverypin.domain.port.out;

import com.ruta.deliverypin.domain.model.DeliveryAttempt;
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

    PageResult<DeliveryAttempt> findAllOrderByCreatedAtDesc(PageRequest pageRequest);

    PageResult<DeliveryAttempt> findAllByDriverOrderByCreatedAtDesc(Long driverId, PageRequest pageRequest);

    Optional<StoredPhoto> findPhotoByAttemptId(Long attemptId);

    Optional<StoredPhoto> findPhotoByAttemptIdAndDriverId(Long attemptId, Long driverId);

    /** Sin paginar: para el mapa y las metricas del admin, acotado por rango de fechas. */
    List<DeliveryAttempt> findAllBetween(Instant from, Instant to);
}
