package com.ruta.deliverypin.domain.port.out;

import com.ruta.deliverypin.domain.model.GeoLocation;
import com.ruta.deliverypin.domain.model.Invoice;
import com.ruta.deliverypin.domain.model.InvoiceLine;

import java.util.List;
import java.util.Optional;

/** Datos y operaciones de entregas. */
public interface InvoiceQueryPort {

    /**
     * Busca facturas publicadas, con PIN de entrega habilitado y aun no confirmadas,
     * que coincidan con el numero de factura o el nombre del cliente.
     */
    List<Invoice> searchPendingDeliveryInvoices(String query);

    /**
     * Lista los productos vendidos en la factura, para el checklist de entrega del conductor.
     */
    List<InvoiceLine> findInvoiceLines(Long invoiceId);

    /** Datos y operaciones de entregas. */
    Optional<GeoLocation> findExpectedLocation(Long invoiceId);

    /**
     * Datos canonicos de la factura (numero, cliente, direccion), para no confiar en
     * lo que el cliente HTTP declara al confirmar una entrega o reportar una incidencia.
     */
    Optional<Invoice> findById(Long invoiceId);
}
