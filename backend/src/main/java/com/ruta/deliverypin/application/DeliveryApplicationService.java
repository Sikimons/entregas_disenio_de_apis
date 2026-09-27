package com.ruta.deliverypin.application;

import com.ruta.deliverypin.domain.exception.DeliveryRejectedException;
import com.ruta.deliverypin.domain.exception.InvalidPinException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Confirma entregas y guarda la evidencia en una misma transaccion local.
 */
@Service
public class DeliveryApplicationService implements
        SearchPendingInvoicesUseCase, ListInvoiceLinesUseCase, ConfirmDeliveryUseCase, ReportIncidentUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeliveryApplicationService.class);

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
        // N7 (docs/EVALUACION_TECNICA.md §18): antes este endpoint no verificaba el estado
        // de la factura, asi que un conductor podia leer las lineas de una factura todavia
        // en borrador (no publicada, sin PIN) con solo adivinar/enumerar su id. La busqueda
        // de facturas pendientes (searchPendingDeliveryInvoices) ya filtraba por
        // state = 'posted'; este metodo, que es la otra puerta de entrada al mismo dato,
        // ahora exige lo mismo.
        Invoice invoice = invoiceQueryPort.findById(invoiceId)
                .orElseThrow(() -> new DeliveryRejectedException("La factura no existe."));
        if (!"posted".equals(invoice.state())) {
            throw new DeliveryRejectedException("La factura debe estar publicada para consultar sus lineas.");
        }
        return invoiceQueryPort.findInvoiceLines(invoiceId);
    }

    @Override
    public boolean confirmDelivery(ConfirmDeliveryCommand command, Driver driver) {
        // Numero, cliente y direccion se toman de la factura real (invoiceId), no de lo
        // que declare el cliente HTTP en ConfirmDeliveryRequest: asi la evidencia queda
        // atada a datos verificados, no a lo que el conductor haya podido escribir.
        Invoice invoice = invoiceQueryPort.findById(command.invoiceId())
                .orElseThrow(() -> new DeliveryRejectedException("La factura no existe."));
        String invoiceNumber = invoice.number();
        String partnerName = invoice.partnerName();
        String deliveryAddress = invoice.deliveryAddress();

        GeoLocation location = new GeoLocation(command.latitude(), command.longitude());

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
                    command.invoiceId(), invoiceNumber, partnerName, deliveryAddress,
                    driver, location, storedPhoto, distanceFromExpectedMeters
            ));
            });
            log.info("Entrega confirmada: factura {} por conductor {}", invoiceNumber, driver.getUsername());
        } catch (DeliveryRejectedException e) {
            // La transaccion de confirmDelivery (arriba) ya reverti todo, incluida
            // cualquier escritura que hubiera intentado hacer dentro de ella. El contador
            // de intentos fallidos se registra aqui, en una transaccion nueva e
            // independiente, precisamente para que sobreviva a ese rollback.
            if (e instanceof InvalidPinException) {
                deliveryConfirmationGateway.registerFailedPinAttempt(command.invoiceId());
                log.warn("PIN invalido para factura {} (conductor {})", invoiceNumber, driver.getUsername());
            } else {
                log.info("Entrega rechazada: factura {} por conductor {}: {}", invoiceNumber, driver.getUsername(), e.getMessage());
            }
            deliveryAttemptRepository.save(DeliveryAttempt.failed(
                    command.invoiceId(), invoiceNumber, partnerName, deliveryAddress, driver, location, e.getMessage()
            ));
            throw e;
        }

        return true;
    }

    @Override
    public void reportIncident(ReportIncidentCommand command, Driver driver) {
        // Igual que en confirmDelivery: se valida que la factura exista y se usan sus
        // datos reales, no los que declare el cliente HTTP.
        Invoice invoice = invoiceQueryPort.findById(command.invoiceId())
                .orElseThrow(() -> new DeliveryRejectedException("La factura no existe."));

        GeoLocation location = command.latitude() != null && command.longitude() != null
                ? new GeoLocation(command.latitude(), command.longitude())
                : null;

        deliveryAttemptRepository.save(DeliveryAttempt.incident(
                command.invoiceId(), invoice.number(), invoice.partnerName(), invoice.deliveryAddress(),
                driver, location, command.reason(), command.notes()
        ));
        log.info("Incidencia reportada: factura {} por conductor {}: {}", invoice.number(), driver.getUsername(), command.reason());
    }
}
