package com.ruta.deliverypin.domain.port.in;

import com.ruta.deliverypin.domain.model.InvoiceLine;

import java.util.List;

public interface ListInvoiceLinesUseCase {

    List<InvoiceLine> list(Long invoiceId);
}
