package com.ruta.deliverypin.domain.port.out;

import com.ruta.deliverypin.domain.model.AdminInvoiceView;
import com.ruta.deliverypin.domain.model.InvoiceLine;

import java.util.List;

/** Puerto de salida para la gestion de facturas desde el panel administrativo. */
public interface InvoiceAdminPort {

    List<AdminInvoiceView> list(String query);

    AdminInvoiceView create(String number, String partnerName, String address, Double latitude, Double longitude,
                             boolean requiresPin, List<InvoiceLine> lines);

    AdminInvoiceView publish(Long id);
}
