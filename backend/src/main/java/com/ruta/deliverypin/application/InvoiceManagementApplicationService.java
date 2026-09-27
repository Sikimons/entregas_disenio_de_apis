package com.ruta.deliverypin.application;

import com.ruta.deliverypin.domain.model.AdminInvoiceView;
import com.ruta.deliverypin.domain.port.in.ManageInvoicesUseCase;
import com.ruta.deliverypin.domain.port.out.InvoiceAdminPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvoiceManagementApplicationService implements ManageInvoicesUseCase {

    private final InvoiceAdminPort invoiceAdminPort;

    public InvoiceManagementApplicationService(InvoiceAdminPort invoiceAdminPort) {
        this.invoiceAdminPort = invoiceAdminPort;
    }

    @Override
    public List<AdminInvoiceView> list(String query) {
        return invoiceAdminPort.list(query);
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
