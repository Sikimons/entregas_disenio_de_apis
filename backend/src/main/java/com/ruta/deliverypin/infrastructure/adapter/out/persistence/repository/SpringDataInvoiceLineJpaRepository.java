package com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository;

import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.InvoiceLineJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataInvoiceLineJpaRepository extends JpaRepository<InvoiceLineJpaEntity, Long> {

    List<InvoiceLineJpaEntity> findByInvoice_IdOrderById(Long invoiceId);
}
