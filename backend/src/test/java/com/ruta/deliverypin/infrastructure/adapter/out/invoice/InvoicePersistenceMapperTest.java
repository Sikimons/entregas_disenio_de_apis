package com.ruta.deliverypin.infrastructure.adapter.out.invoice;

import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.InvoiceJpaEntity;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.InvoiceLineJpaEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class InvoicePersistenceMapperTest {

    @Test
    void toInvoice_mapsAllFields() {
        InvoiceJpaEntity entity = new InvoiceJpaEntity(1L, "F-001", "Cliente", "Direccion", -2.1, -79.9,
                LocalDate.of(2026, 9, 1), "posted", true, "123456", false, null, null, null, null, 0, null);

        var invoice = InvoicePersistenceMapper.toInvoice(entity);

        assertThat(invoice.id()).isEqualTo(1L);
        assertThat(invoice.number()).isEqualTo("F-001");
        assertThat(invoice.partnerName()).isEqualTo("Cliente");
        assertThat(invoice.deliveryAddress()).isEqualTo("Direccion");
        assertThat(invoice.invoiceDate()).isEqualTo("2026-09-01");
        assertThat(invoice.state()).isEqualTo("posted");
        assertThat(invoice.expectedLatitude()).isEqualTo(-2.1);
        assertThat(invoice.expectedLongitude()).isEqualTo(-79.9);
    }

    @Test
    void toAdminView_includesPinAndConfirmationState() {
        InvoiceJpaEntity entity = new InvoiceJpaEntity(2L, "F-002", "Cliente 2", null, null, null,
                LocalDate.of(2026, 9, 2), "posted", true, "654321", true, Instant.parse("2026-09-02T10:00:00Z"),
                -2.0, -79.8, "Conductor 1", 0, null);

        var view = InvoicePersistenceMapper.toAdminView(entity);

        assertThat(view.pin()).isEqualTo("654321");
        assertThat(view.confirmed()).isTrue();
        assertThat(view.requiresPin()).isTrue();
        assertThat(view.state()).isEqualTo("posted");
    }

    @Test
    void toLine_mapsDescriptionAndQuantity() {
        InvoiceLineJpaEntity entity = new InvoiceLineJpaEntity(5L, null, "Producto", 3.5);

        var line = InvoicePersistenceMapper.toLine(entity);

        assertThat(line.id()).isEqualTo(5L);
        assertThat(line.description()).isEqualTo("Producto");
        assertThat(line.quantity()).isEqualTo(3.5);
    }
}
