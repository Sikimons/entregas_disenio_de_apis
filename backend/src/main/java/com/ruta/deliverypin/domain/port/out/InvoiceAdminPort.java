package com.ruta.deliverypin.domain.port.out;

import com.ruta.deliverypin.domain.model.AdminInvoiceView;
import com.ruta.deliverypin.domain.model.InvoiceLine;
import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.domain.model.PageResult;

import java.util.List;

/** Puerto de salida para la gestion de facturas desde el panel administrativo. */
public interface InvoiceAdminPort {

    PageResult<AdminInvoiceView> list(String query, PageRequest pageRequest);

    AdminInvoiceView create(String number, String partnerName, String address, Double latitude, Double longitude,
                             boolean requiresPin, List<InvoiceLine> lines, String createdBy);

    AdminInvoiceView publish(Long id, String publishedBy);
}
