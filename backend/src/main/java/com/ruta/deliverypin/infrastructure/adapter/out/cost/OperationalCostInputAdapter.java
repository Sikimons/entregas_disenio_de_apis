package com.ruta.deliverypin.infrastructure.adapter.out.cost;

import com.ruta.deliverypin.domain.port.out.OperationalCostInputPort;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.OperationalCostInputJpaEntity;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository.SpringDataOperationalCostInputJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

/**
 * Horas de soporte por mes, editables desde el panel administrativo (no es una tarifa fija).
 * Migrado en la Fase8 de JdbcTemplate a JPA (SpringDataOperationalCostInputJpaRepository),
 * cerrando junto con LocalInvoiceAdapter la doble estrategia de persistencia senalada en RA4.
 */
@Repository
public class OperationalCostInputAdapter implements OperationalCostInputPort {

    private final SpringDataOperationalCostInputJpaRepository repository;

    public OperationalCostInputAdapter(SpringDataOperationalCostInputJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public double findSupportHours(YearMonth month) {
        return repository.findById(month.atDay(1)).map(OperationalCostInputJpaEntity::getSupportHours).orElse(0.0);
    }

    @Override
    @Transactional
    public void saveSupportHours(YearMonth month, double hours) {
        repository.upsertSupportHours(month.atDay(1), hours);
    }
}
