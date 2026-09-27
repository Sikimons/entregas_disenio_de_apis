package com.ruta.deliverypin.infrastructure.adapter.out.invoice;

import com.ruta.deliverypin.domain.model.AdminInvoiceView;
import com.ruta.deliverypin.domain.model.Invoice;
import com.ruta.deliverypin.domain.model.InvoiceLine;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.InvoiceJpaEntity;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.InvoiceLineJpaEntity;

/**
 * Traduccion entidad JPA (delivery_invoice/delivery_invoice_line) &lt;-&gt; modelo de dominio
 * (Fase8): mismo rol que DeliveryAttemptPersistenceMapper, para la tabla que hasta ahora
 * se leia/escribia con JdbcTemplate directo desde LocalInvoiceAdapter.
 */
final class InvoicePersistenceMapper {

    private InvoicePersistenceMapper() {
    }

    static Invoice toInvoice(InvoiceJpaEntity entity) {
        return new Invoice(entity.getId(), entity.getNumber(), entity.getPartnerName(), entity.getDeliveryAddress(),
                entity.getInvoiceDate().toString(), entity.getState(), entity.getExpectedLatitude(), entity.getExpectedLongitude());
    }

    static AdminInvoiceView toAdminView(InvoiceJpaEntity entity) {
        return new AdminInvoiceView(entity.getId(), entity.getNumber(), entity.getPartnerName(), entity.getDeliveryAddress(),
                entity.getInvoiceDate().toString(), entity.getState(), entity.isRequiresPin(), entity.getPin(), entity.isConfirmed(),
                entity.getExpectedLatitude(), entity.getExpectedLongitude(), entity.getCreatedBy(), entity.getPublishedBy());
    }

    static InvoiceLine toLine(InvoiceLineJpaEntity entity) {
        return new InvoiceLine(entity.getId(), entity.getDescription(), entity.getQuantity());
    }
}
