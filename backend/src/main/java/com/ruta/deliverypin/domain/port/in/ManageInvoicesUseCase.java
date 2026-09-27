package com.ruta.deliverypin.domain.port.in;

import com.ruta.deliverypin.domain.model.AdminInvoiceView;
import com.ruta.deliverypin.domain.model.InvoiceLine;

import java.util.List;

/** Casos de uso de gestion de facturas para el panel administrativo. */
public interface ManageInvoicesUseCase {

    List<AdminInvoiceView> list(String query);

    AdminInvoiceView create(CreateInvoiceCommand command);

    AdminInvoiceView publish(Long id);

    record CreateInvoiceCommand(
            String number, String partnerName, String deliveryAddress,
            Double latitude, Double longitude, boolean requiresPin, List<InvoiceLine> lines
    ) {
    }
}
