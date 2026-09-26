package com.ruta.deliverypin.application;

import com.ruta.deliverypin.domain.model.DeliveryAttempt;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public PageResult<DeliveryAttempt> list(PageRequest pageRequest) {
        return deliveryAttemptRepository.findAllOrderByCreatedAtDesc(pageRequest);
    }

    @Override
    public Optional<StoredPhoto> getPhoto(Long attemptId) {
        return deliveryAttemptRepository.findPhotoByAttemptId(attemptId);
    }

    @Override
    public PageResult<DeliveryAttempt> list(Long driverId, PageRequest pageRequest) {
        return deliveryAttemptRepository.findAllByDriverOrderByCreatedAtDesc(driverId, pageRequest);
    }

    @Override
    public Optional<StoredPhoto> getPhoto(Long attemptId, Long driverId) {
        return deliveryAttemptRepository.findPhotoByAttemptIdAndDriverId(attemptId, driverId);
    }

    @Override
    public List<DeliveryAttempt> list(Instant from, Instant to) {
        return deliveryAttemptRepository.findAllBetween(from, to);
    }

    @Override
    public List<DriverMetric> metrics(Instant from, Instant to) {
        List<DeliveryAttempt> attempts = deliveryAttemptRepository.findAllBetween(from, to);

        Map<String, long[]> countsByDriver = new LinkedHashMap<>(); // [confirmed, rejected, incident]
        for (DeliveryAttempt attempt : attempts) {
            String driverName = attempt.getDriver().getFullName();
            long[] counts = countsByDriver.computeIfAbsent(driverName, k -> new long[3]);
            switch (attempt.getOutcome()) {
                case CONFIRMED -> counts[0]++;
                case REJECTED -> counts[1]++;
                case INCIDENT -> counts[2]++;
            }
        }

        List<DriverMetric> metrics = new ArrayList<>();
        countsByDriver.forEach((driverName, counts) ->
                metrics.add(new DriverMetric(driverName, counts[0], counts[1], counts[2])));
        metrics.sort((a, b) -> Long.compare(b.total(), a.total()));
        return metrics;
    }
}
