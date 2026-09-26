package com.ruta.deliverypin.application;

import com.ruta.deliverypin.domain.exception.DeliveryRejectedException;
import com.ruta.deliverypin.domain.model.DeliveryAttempt;
import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.model.GeoLocation;
import com.ruta.deliverypin.domain.model.Invoice;
import com.ruta.deliverypin.domain.model.InvoiceLine;
import com.ruta.deliverypin.domain.model.StoredPhoto;
import com.ruta.deliverypin.domain.port.in.ConfirmDeliveryUseCase;
import com.ruta.deliverypin.domain.port.in.ListInvoiceLinesUseCase;
import com.ruta.deliverypin.domain.port.in.ReportIncidentUseCase;
import com.ruta.deliverypin.domain.port.in.SearchPendingInvoicesUseCase;
import com.ruta.deliverypin.domain.port.out.DeliveryAttemptRepositoryPort;
import com.ruta.deliverypin.domain.port.out.DeliveryConfirmationGatewayPort;
import com.ruta.deliverypin.domain.port.out.InvoiceQueryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Confirma entregas y guarda la evidencia en una misma transaccion local.
 */
@Service
public class DeliveryApplicationService implements
        SearchPendingInvoicesUseCase, ListInvoiceLinesUseCase, ConfirmDeliveryUseCase, ReportIncidentUseCase {

    private final InvoiceQueryPort invoiceQueryPort;
    private final DeliveryConfirmationGatewayPort deliveryConfirmationGateway;
    private final DeliveryAttemptRepositoryPort deliveryAttemptRepository;
    private final TransactionTemplate transactions;

    public DeliveryApplicationService(
            InvoiceQueryPort invoiceQueryPort,
            DeliveryConfirmationGatewayPort deliveryConfirmationGateway,
            DeliveryAttemptRepositoryPort deliveryAttemptRepository,
            TransactionTemplate transactions
    ) {
        this.invoiceQueryPort = invoiceQueryPort;
        this.deliveryConfirmationGateway = deliveryConfirmationGateway;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.transactions = transactions;
    }

    @Override
    public List<Invoice> search(String query) {
        return invoiceQueryPort.searchPendingDeliveryInvoices(query);
    }

    @Override
    public List<InvoiceLine> list(Long invoiceId) {
        return invoiceQueryPort.findInvoiceLines(invoiceId);
    }

    @Override
    public boolean confirmDelivery(ConfirmDeliveryCommand command, Driver driver) {
        GeoLocation location = new GeoLocation(command.latitude(), command.longitude());
        String invoiceNumber = command.invoiceNumber();

        StoredPhoto storedPhoto = command.photo() != null
                ? new StoredPhoto(command.photo().decodedBytes(), command.photo().contentType())
                : null;

        try {
            transactions.executeWithoutResult(status -> {
            deliveryConfirmationGateway.confirmDelivery(command.invoiceId(), command.pin(), location, driver.getFullName());

            Double distanceFromExpectedMeters = invoiceQueryPort.findExpectedLocation(command.invoiceId())
                    .map(location::distanceMetersTo)
                    .orElse(null);

            deliveryAttemptRepository.save(DeliveryAttempt.succeeded(
                    command.invoiceId(), invoiceNumber, command.partnerName(), command.deliveryAddress(),
                    driver, location, storedPhoto, distanceFromExpectedMeters
            ));
            });
        } catch (DeliveryRejectedException e) {
            deliveryAttemptRepository.save(DeliveryAttempt.failed(
                    command.invoiceId(), invoiceNumber, command.partnerName(), command.deliveryAddress(), driver, location, e.getMessage()
            ));
            throw e;
        }

        return true;
    }

    @Override
    public void reportIncident(ReportIncidentCommand command, Driver driver) {
        GeoLocation location = command.latitude() != null && command.longitude() != null
                ? new GeoLocation(command.latitude(), command.longitude())
                : null;

        deliveryAttemptRepository.save(DeliveryAttempt.incident(
                command.invoiceId(), command.invoiceNumber(), command.partnerName(), command.deliveryAddress(),
                driver, location, command.reason(), command.notes()
        ));
    }
}
