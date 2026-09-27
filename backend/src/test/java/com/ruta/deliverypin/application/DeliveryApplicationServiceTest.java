package com.ruta.deliverypin.application;

import com.ruta.deliverypin.domain.exception.DeliveryAlreadyConfirmedException;
import com.ruta.deliverypin.domain.exception.InvalidPinException;
import com.ruta.deliverypin.domain.model.DeliveryAttemptOutcome;
import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.model.GeoLocation;
import com.ruta.deliverypin.domain.model.Invoice;
import com.ruta.deliverypin.domain.model.Role;
import com.ruta.deliverypin.domain.port.in.ConfirmDeliveryUseCase.ConfirmDeliveryCommand;
import com.ruta.deliverypin.domain.port.in.ReportIncidentUseCase.ReportIncidentCommand;
import com.ruta.deliverypin.domain.port.out.DeliveryAttemptRepositoryPort;
import com.ruta.deliverypin.domain.port.out.DeliveryConfirmationGatewayPort;
import com.ruta.deliverypin.domain.port.out.InvoiceQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryApplicationServiceTest {

    @Mock
    private InvoiceQueryPort invoiceQueryPort;
    @Mock
    private DeliveryConfirmationGatewayPort deliveryConfirmationGateway;
    @Mock
    private DeliveryAttemptRepositoryPort deliveryAttemptRepository;

    private final TransactionTemplate transactions = mock(TransactionTemplate.class);
    private DeliveryApplicationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new DeliveryApplicationService(invoiceQueryPort, deliveryConfirmationGateway, deliveryAttemptRepository, transactions);
        // TransactionTemplate.executeWithoutResult ejecuta el callback como si la
        // transaccion estuviera activa: en la prueba, simplemente lo invocamos directo.
        doAnswer(invocation -> {
            Consumer<Object> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactions).executeWithoutResult(any());
        // confirmDelivery/reportIncident ahora resuelven la factura real (numero, cliente,
        // direccion) por invoiceId antes de todo, en vez de confiar en el cliente HTTP.
        // lenient(): el test de "factura no existe" usa un invoiceId distinto (99L) y no
        // necesita este stub; sin lenient(), Mockito lo marca como stubbing innecesario ahi.
        org.mockito.Mockito.lenient().when(invoiceQueryPort.findById(1L)).thenReturn(Optional.of(
                new Invoice(1L, "F-001", "Cliente", "Direccion", "2026-01-01", "posted", null, null)));
    }

    private Driver driver() {
        return new Driver(10L, "conductor1", "hash", "Conductor Uno", Role.CONDUCTOR, true, Instant.now());
    }

    @Test
    void confirmDelivery_success_savesConfirmedAttempt() {
        var command = new ConfirmDeliveryCommand(1L, "123456", -2.171, -79.922, null);
        when(invoiceQueryPort.findExpectedLocation(1L)).thenReturn(Optional.empty());

        boolean result = service.confirmDelivery(command, driver());

        assertThat(result).isTrue();
        verify(deliveryConfirmationGateway).confirmDelivery(eq(1L), eq("123456"), any(GeoLocation.class), eq("Conductor Uno"));
        verify(deliveryAttemptRepository).save(argThatOutcomeIs(DeliveryAttemptOutcome.CONFIRMED));
    }

    @Test
    void confirmDelivery_invalidPin_savesRejectedAttemptAndRethrows() {
        var command = new ConfirmDeliveryCommand(1L, "000000", -2.171, -79.922, null);
        doThrow(new InvalidPinException()).when(deliveryConfirmationGateway)
                .confirmDelivery(eq(1L), eq("000000"), any(GeoLocation.class), eq("Conductor Uno"));

        assertThatThrownBy(() -> service.confirmDelivery(command, driver()))
                .isInstanceOf(InvalidPinException.class);

        // registerFailedPinAttempt se llama en una transaccion aparte, DESPUES de que la
        // de confirmDelivery ya revirtio (ver comentario en DeliveryApplicationService).
        verify(deliveryConfirmationGateway).registerFailedPinAttempt(1L);
        verify(deliveryAttemptRepository).save(argThatOutcomeIs(DeliveryAttemptOutcome.REJECTED));
    }

    @Test
    void confirmDelivery_alreadyConfirmed_doesNotRegisterFailedPinAttempt() {
        var command = new ConfirmDeliveryCommand(1L, "123456", -2.171, -79.922, null);
        doThrow(new DeliveryAlreadyConfirmedException()).when(deliveryConfirmationGateway)
                .confirmDelivery(eq(1L), eq("123456"), any(GeoLocation.class), eq("Conductor Uno"));

        assertThatThrownBy(() -> service.confirmDelivery(command, driver()))
                .isInstanceOf(DeliveryAlreadyConfirmedException.class);

        verify(deliveryConfirmationGateway, org.mockito.Mockito.never()).registerFailedPinAttempt(any());
    }

    @Test
    void confirmDelivery_alreadyConfirmed_savesRejectedAttemptAndRethrows() {
        var command = new ConfirmDeliveryCommand(1L, "123456", -2.171, -79.922, null);
        doThrow(new DeliveryAlreadyConfirmedException()).when(deliveryConfirmationGateway)
                .confirmDelivery(eq(1L), eq("123456"), any(GeoLocation.class), eq("Conductor Uno"));

        assertThatThrownBy(() -> service.confirmDelivery(command, driver()))
                .isInstanceOf(DeliveryAlreadyConfirmedException.class);

        verify(deliveryAttemptRepository).save(argThatOutcomeIs(DeliveryAttemptOutcome.REJECTED));
    }

    @Test
    void confirmDelivery_invoiceDoesNotExist_throwsWithoutCallingGateway() {
        var command = new ConfirmDeliveryCommand(99L, "123456", -2.171, -79.922, null);
        when(invoiceQueryPort.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmDelivery(command, driver()))
                .isInstanceOf(com.ruta.deliverypin.domain.exception.DeliveryRejectedException.class);

        verify(deliveryConfirmationGateway, org.mockito.Mockito.never()).confirmDelivery(any(), any(), any(), any());
    }

    @Test
    void list_postedInvoice_returnsLines() {
        var line = new com.ruta.deliverypin.domain.model.InvoiceLine(null, "item", 2.0);
        when(invoiceQueryPort.findInvoiceLines(1L)).thenReturn(java.util.List.of(line));

        var result = service.list(1L);

        assertThat(result).containsExactly(line);
    }

    @Test
    void list_draftInvoice_rejectsWithoutReturningLines() {
        when(invoiceQueryPort.findById(2L)).thenReturn(Optional.of(
                new Invoice(2L, "F-002", "Cliente", "Direccion", "2026-01-01", "draft", null, null)));

        assertThatThrownBy(() -> service.list(2L))
                .isInstanceOf(com.ruta.deliverypin.domain.exception.DeliveryRejectedException.class);

        verify(invoiceQueryPort, org.mockito.Mockito.never()).findInvoiceLines(any());
    }

    @Test
    void list_invoiceDoesNotExist_rejectsWithoutReturningLines() {
        when(invoiceQueryPort.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.list(99L))
                .isInstanceOf(com.ruta.deliverypin.domain.exception.DeliveryRejectedException.class);

        verify(invoiceQueryPort, org.mockito.Mockito.never()).findInvoiceLines(any());
    }

    @Test
    void reportIncident_savesIncidentAttempt() {
        var command = new ReportIncidentCommand(1L, "F-001", "Cliente", "Direccion", "Cliente ausente", "No habia nadie", -2.171, -79.922);

        service.reportIncident(command, driver());

        verify(deliveryAttemptRepository).save(argThatOutcomeIs(DeliveryAttemptOutcome.INCIDENT));
    }

    private com.ruta.deliverypin.domain.model.DeliveryAttempt argThatOutcomeIs(DeliveryAttemptOutcome outcome) {
        return org.mockito.ArgumentMatchers.argThat(attempt -> attempt.getOutcome() == outcome);
    }
}
