package com.ruta.deliverypin.domain.port.in;

import com.ruta.deliverypin.domain.model.Invoice;

import java.util.List;

public interface SearchPendingInvoicesUseCase {

    List<Invoice> search(String query);
}
