package com.ruta.deliverypin.domain.port.in;

import com.ruta.deliverypin.domain.model.AdminInvoiceView;
import com.ruta.deliverypin.domain.model.InvoiceLine;
import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.domain.model.PageResult;

import java.util.List;

/** Casos de uso de gestion de facturas para el panel administrativo. */
public interface ManageInvoicesUseCase {

    PageResult<AdminInvoiceView> list(String query, PageRequest pageRequest);

    AdminInvoiceView create(CreateInvoiceCommand command, String createdBy);

    AdminInvoiceView publish(Long id, String publishedBy);

    record CreateInvoiceCommand(
            String number, String partnerName, String deliveryAddress,
            Double latitude, Double longitude, boolean requiresPin, List<InvoiceLine> lines
    ) {
    }
}
