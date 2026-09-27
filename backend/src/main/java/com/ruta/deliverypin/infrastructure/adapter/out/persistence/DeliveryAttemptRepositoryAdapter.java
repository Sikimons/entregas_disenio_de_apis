package com.ruta.deliverypin.infrastructure.adapter.out.persistence;

import com.ruta.deliverypin.domain.model.DeliveryAttempt;
import com.ruta.deliverypin.domain.model.DeliveryAttemptSummary;
import com.ruta.deliverypin.domain.model.DeliveryVolumeSummary;
import com.ruta.deliverypin.domain.model.DriverMetric;
import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.domain.model.PageResult;
import com.ruta.deliverypin.domain.model.StoredPhoto;
import com.ruta.deliverypin.domain.port.out.DeliveryAttemptRepositoryPort;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.DeliveryAttemptJpaEntity;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository.SpringDataDeliveryAttemptJpaRepository;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository.SpringDataDeliveryAttemptJpaRepository.AttemptSummaryProjection;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository.SpringDataDeliveryAttemptJpaRepository.DriverOutcomeCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DeliveryAttemptRepositoryAdapter implements DeliveryAttemptRepositoryPort {

    private final SpringDataDeliveryAttemptJpaRepository jpaRepository;

    public DeliveryAttemptRepositoryAdapter(SpringDataDeliveryAttemptJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public DeliveryAttempt save(DeliveryAttempt attempt) {
        DeliveryAttemptJpaEntity saved = jpaRepository.save(DeliveryAttemptPersistenceMapper.toEntity(attempt));
        return DeliveryAttemptPersistenceMapper.toDomain(saved);
    }

    private static DeliveryAttemptSummary toSummary(AttemptSummaryProjection p) {
        return new DeliveryAttemptSummary(
                p.getId(), p.getInvoiceId(), p.getInvoiceNumber(), p.getPartnerName(), p.getDeliveryAddress(),
                p.getDriverName(), p.getOutcome(), p.getLatitude(), p.getLongitude(), p.getDetail(),
                Boolean.TRUE.equals(p.getHasPhoto()), p.getDistanceFromExpectedMeters(), p.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DeliveryAttemptSummary> findAllOrderByCreatedAtDesc(PageRequest pageRequest) {
        Page<AttemptSummaryProjection> page = jpaRepository.findSummaries(
                org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size())
        );
        return new PageResult<>(
                page.getContent().stream().map(DeliveryAttemptRepositoryAdapter::toSummary).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DeliveryAttemptSummary> findAllByDriverOrderByCreatedAtDesc(Long driverId, PageRequest pageRequest) {
        Page<AttemptSummaryProjection> page = jpaRepository.findSummariesByDriverId(
                driverId, org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size())
        );
        return new PageResult<>(
                page.getContent().stream().map(DeliveryAttemptRepositoryAdapter::toSummary).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()
        );
    }

    @Override
    public Optional<StoredPhoto> findPhotoByAttemptId(Long attemptId) {
        return jpaRepository.findPhotoById(attemptId)
                .map(projection -> new StoredPhoto(projection.getPhoto(), projection.getPhotoContentType()));
    }

    @Override
    public Optional<StoredPhoto> findPhotoByAttemptIdAndDriverId(Long attemptId, Long driverId) {
        return jpaRepository.findPhotoByIdAndDriverId(attemptId, driverId)
                .map(projection -> new StoredPhoto(projection.getPhoto(), projection.getPhotoContentType()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAttemptSummary> findAllBetween(Instant from, Instant to) {
        return jpaRepository.findSummariesByCreatedAtBetween(from, to).stream()
                .map(DeliveryAttemptRepositoryAdapter::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverMetric> metricsBetween(Instant from, Instant to) {
        List<DriverOutcomeCountProjection> rows = jpaRepository.countByDriverAndOutcomeBetween(from, to);

        Map<String, long[]> countsByDriver = new LinkedHashMap<>(); // [confirmed, rejected, incident]
        for (DriverOutcomeCountProjection row : rows) {
            long[] counts = countsByDriver.computeIfAbsent(row.getDriverName(), k -> new long[3]);
            switch (row.getOutcome()) {
                case CONFIRMED -> counts[0] += row.getTotal();
                case REJECTED -> counts[1] += row.getTotal();
                case INCIDENT -> counts[2] += row.getTotal();
            }
        }

        return countsByDriver.entrySet().stream()
                .map(e -> new DriverMetric(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .sorted((a, b) -> Long.compare(b.total(), a.total()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryVolumeSummary summarizeConfirmedBetween(Instant from, Instant to) {
        var projection = jpaRepository.summarizeConfirmedBetween(from, to);
        return new DeliveryVolumeSummary(projection.getConfirmed(), projection.getPhotoBytes());
    }
}
