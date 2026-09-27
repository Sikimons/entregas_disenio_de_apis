package com.ruta.deliverypin.application;

import com.ruta.deliverypin.domain.model.DeliveryAttemptSummary;
import com.ruta.deliverypin.domain.model.DriverMetric;
import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.domain.model.PageResult;
import com.ruta.deliverypin.domain.model.StoredPhoto;
import com.ruta.deliverypin.domain.port.in.GetDeliveryPhotoUseCase;
import com.ruta.deliverypin.domain.port.in.GetDriverDeliveryPhotoUseCase;
import com.ruta.deliverypin.domain.port.in.GetDriverMetricsUseCase;
import com.ruta.deliverypin.domain.port.in.ListDeliveryAttemptsInRangeUseCase;
import com.ruta.deliverypin.domain.port.in.ListDeliveryHistoryUseCase;
import com.ruta.deliverypin.domain.port.in.ListDriverDeliveryHistoryUseCase;
import com.ruta.deliverypin.domain.port.out.DeliveryAttemptRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class DeliveryHistoryApplicationService implements
        ListDeliveryHistoryUseCase, GetDeliveryPhotoUseCase, ListDeliveryAttemptsInRangeUseCase, GetDriverMetricsUseCase,
        ListDriverDeliveryHistoryUseCase, GetDriverDeliveryPhotoUseCase {

    private final DeliveryAttemptRepositoryPort deliveryAttemptRepository;

    public DeliveryHistoryApplicationService(DeliveryAttemptRepositoryPort deliveryAttemptRepository) {
        this.deliveryAttemptRepository = deliveryAttemptRepository;
    }

    @Override
    public PageResult<DeliveryAttemptSummary> list(PageRequest pageRequest) {
        return deliveryAttemptRepository.findAllOrderByCreatedAtDesc(pageRequest);
    }

    @Override
    public Optional<StoredPhoto> getPhoto(Long attemptId) {
        return deliveryAttemptRepository.findPhotoByAttemptId(attemptId);
    }

    @Override
    public PageResult<DeliveryAttemptSummary> list(Long driverId, PageRequest pageRequest) {
        return deliveryAttemptRepository.findAllByDriverOrderByCreatedAtDesc(driverId, pageRequest);
    }

    @Override
    public Optional<StoredPhoto> getPhoto(Long attemptId, Long driverId) {
        return deliveryAttemptRepository.findPhotoByAttemptIdAndDriverId(attemptId, driverId);
    }

    @Override
    public List<DeliveryAttemptSummary> list(Instant from, Instant to) {
        return deliveryAttemptRepository.findAllBetween(from, to);
    }

    @Override
    public List<DriverMetric> metrics(Instant from, Instant to) {
        // Agregado en SQL (GROUP BY conductor, resultado) por el adaptador: ya no trae
        // cada intento (ni su foto) a memoria Java solo para contarlo.
        return deliveryAttemptRepository.metricsBetween(from, to);
    }
}
