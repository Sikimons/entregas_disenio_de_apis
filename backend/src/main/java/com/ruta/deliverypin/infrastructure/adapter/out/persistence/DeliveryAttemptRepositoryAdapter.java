package com.ruta.deliverypin.infrastructure.adapter.out.persistence;

import com.ruta.deliverypin.domain.model.DeliveryAttempt;
import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.domain.model.PageResult;
import com.ruta.deliverypin.domain.model.StoredPhoto;
import com.ruta.deliverypin.domain.port.out.DeliveryAttemptRepositoryPort;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.DeliveryAttemptJpaEntity;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository.SpringDataDeliveryAttemptJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
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

    @Override
    @Transactional(readOnly = true)
    public PageResult<DeliveryAttempt> findAllOrderByCreatedAtDesc(PageRequest pageRequest) {
        Page<DeliveryAttemptJpaEntity> page = jpaRepository.findAllByOrderByCreatedAtDesc(
                org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size())
        );

        return new PageResult<>(
                page.getContent().stream().map(DeliveryAttemptPersistenceMapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DeliveryAttempt> findAllByDriverOrderByCreatedAtDesc(Long driverId, PageRequest pageRequest) {
        Page<DeliveryAttemptJpaEntity> page = jpaRepository.findAllByDriver_IdOrderByCreatedAtDesc(
                driverId,
                org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size())
        );

        return new PageResult<>(
                page.getContent().stream().map(DeliveryAttemptPersistenceMapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
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
    public List<DeliveryAttempt> findAllBetween(Instant from, Instant to) {
        return jpaRepository.findAllByCreatedAtBetweenOrderByCreatedAtDesc(from, to).stream()
                .map(DeliveryAttemptPersistenceMapper::toDomain)
                .toList();
    }
}
