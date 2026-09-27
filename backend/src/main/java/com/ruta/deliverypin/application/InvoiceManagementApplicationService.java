package com.ruta.deliverypin.application;

import com.ruta.deliverypin.domain.model.AdminInvoiceView;
import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.domain.model.PageResult;
import com.ruta.deliverypin.domain.port.in.ManageInvoicesUseCase;
import com.ruta.deliverypin.domain.port.out.InvoiceAdminPort;
import org.springframework.stereotype.Service;

@Service
public class InvoiceManagementApplicationService implements ManageInvoicesUseCase {

    private final InvoiceAdminPort invoiceAdminPort;

    public InvoiceManagementApplicationService(InvoiceAdminPort invoiceAdminPort) {
        this.invoiceAdminPort = invoiceAdminPort;
    }

    @Override
    public PageResult<AdminInvoiceView> list(String query, PageRequest pageRequest) {
        return invoiceAdminPort.list(query, pageRequest);
    }

    @Override
    public AdminInvoiceView create(CreateInvoiceCommand command) {
        return invoiceAdminPort.create(command.number(), command.partnerName(), command.deliveryAddress(),
                command.latitude(), command.longitude(), command.requiresPin(), command.lines());
    }

    @Override
    public AdminInvoiceView publish(Long id) {
        return invoiceAdminPort.publish(id);
    }
}
