package com.ruta.deliverypin.infrastructure.adapter.in.web;

import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.model.Role;
import com.ruta.deliverypin.domain.port.in.ConfirmDeliveryUseCase;
import com.ruta.deliverypin.domain.port.in.GetDriverDeliveryPhotoUseCase;
import com.ruta.deliverypin.domain.port.in.ListDriverDeliveryHistoryUseCase;
import com.ruta.deliverypin.domain.port.in.ListInvoiceLinesUseCase;
import com.ruta.deliverypin.domain.port.in.ReportIncidentUseCase;
import com.ruta.deliverypin.domain.port.in.SearchPendingInvoicesUseCase;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.ConfirmDeliveryRequest;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.ReportIncidentRequest;
import com.ruta.deliverypin.infrastructure.adapter.in.web.security.ConfirmRateLimiter;
import com.ruta.deliverypin.infrastructure.adapter.in.web.security.CurrentDriverResolver;
import com.ruta.deliverypin.infrastructure.adapter.in.web.security.IdempotencyKeyStore;
import com.ruta.deliverypin.infrastructure.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre el punto mas critico de la Tanda 2 (auditoria tecnica): que "Idempotency-Key"
 * de verdad evite repetir el caso de uso -- sobre todo en /incident, que a diferencia de
 * /confirm no tiene ninguna regla de dominio que impida crear dos registros iguales.
 * Unitario y directo (mismo patron que AdminDashboardControllerTest): el controlador se
 * construye a mano con sus dependencias mockeadas, sin levantar el contexto de Spring ni
 * la cadena de seguridad (eso ya lo cubre AdminDriverControllerWebMvcTest para el patron
 * de autorizacion por rol, que es igual en todos los controladores).
 */
class DriverDeliveryControllerTest {

    private final SearchPendingInvoicesUseCase searchUseCase = mock(SearchPendingInvoicesUseCase.class);
    private final ListInvoiceLinesUseCase linesUseCase = mock(ListInvoiceLinesUseCase.class);
    private final ConfirmDeliveryUseCase confirmUseCase = mock(ConfirmDeliveryUseCase.class);
    private final ReportIncidentUseCase incidentUseCase = mock(ReportIncidentUseCase.class);
    private final ListDriverDeliveryHistoryUseCase historyUseCase = mock(ListDriverDeliveryHistoryUseCase.class);
    private final GetDriverDeliveryPhotoUseCase photoUseCase = mock(GetDriverDeliveryPhotoUseCase.class);
    private final CurrentDriverResolver currentDriverResolver = mock(CurrentDriverResolver.class);

    private DriverDeliveryController controller;

    @BeforeEach
    void setUp() {
        Driver driver = new Driver(10L, "conductor1", "hash", "Conductor Uno", Role.CONDUCTOR, true, Instant.now());
        when(currentDriverResolver.resolve()).thenReturn(driver);

        // Instancias reales (no mocks) de los dos componentes nuevos de la Tanda 2: son el
        // objeto bajo prueba tanto como el controlador mismo -- mockearlos ocultaria
        // exactamente el comportamiento que este test quiere verificar.
        ConfirmRateLimiter confirmRateLimiter = new ConfirmRateLimiter(new AppProperties());
        IdempotencyKeyStore idempotencyKeyStore = new IdempotencyKeyStore();

        controller = new DriverDeliveryController(
                searchUseCase, linesUseCase, confirmUseCase, incidentUseCase,
                historyUseCase, photoUseCase, currentDriverResolver, confirmRateLimiter, idempotencyKeyStore
        );
    }

    private ConfirmDeliveryRequest confirmRequest() {
        return new ConfirmDeliveryRequest(1L, "123456", -2.17, -79.92, "base64photo", "foto.jpg", "image/jpeg");
    }

    private ReportIncidentRequest incidentRequest() {
        return new ReportIncidentRequest(1L, "F-001", "Cliente", "Direccion", "Cliente ausente", "Nadie respondio", -2.17, -79.92);
    }

    @Test
    void confirmDelivery_withoutIdempotencyKey_alwaysCallsUseCase() {
        when(confirmUseCase.confirmDelivery(any(), any())).thenReturn(true);

        controller.confirmDelivery(confirmRequest(), null);
        controller.confirmDelivery(confirmRequest(), null);

        verify(confirmUseCase, times(2)).confirmDelivery(any(), any());
    }

    @Test
    void confirmDelivery_sameIdempotencyKeyTwice_callsUseCaseOnlyOnce_andReturnsSameResponse() {
        when(confirmUseCase.confirmDelivery(any(), any())).thenReturn(true);
        String idempotencyKey = "confirm-key-123";

        var first = controller.confirmDelivery(confirmRequest(), idempotencyKey);
        var second = controller.confirmDelivery(confirmRequest(), idempotencyKey);

        assertThat(second).isEqualTo(first);
        verify(confirmUseCase, times(1)).confirmDelivery(any(), any());
    }

    @Test
    void confirmDelivery_differentIdempotencyKeys_callsUseCaseEachTime() {
        when(confirmUseCase.confirmDelivery(any(), any())).thenReturn(true);

        controller.confirmDelivery(confirmRequest(), "key-a");
        controller.confirmDelivery(confirmRequest(), "key-b");

        verify(confirmUseCase, times(2)).confirmDelivery(any(), any());
    }

    @Test
    void confirmDelivery_exceedsRateLimit_throwsWithoutCallingUseCase() {
        AppProperties limitedProperties = new AppProperties();
        limitedProperties.getConfirmRateLimit().setMaxAttempts(1);
        limitedProperties.getConfirmRateLimit().setWindowSeconds(60);
        controller = new DriverDeliveryController(
                searchUseCase, linesUseCase, confirmUseCase, incidentUseCase, historyUseCase, photoUseCase,
                currentDriverResolver, new ConfirmRateLimiter(limitedProperties), new IdempotencyKeyStore()
        );
        when(confirmUseCase.confirmDelivery(any(), any())).thenReturn(true);
        controller.confirmDelivery(confirmRequest(), null);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.ruta.deliverypin.domain.exception.TooManyConfirmAttemptsException.class,
                () -> controller.confirmDelivery(confirmRequest(), null)
        );
        verify(confirmUseCase, times(1)).confirmDelivery(any(), any());
    }

    @Test
    void reportIncident_withoutIdempotencyKey_alwaysCallsUseCase() {
        controller.reportIncident(incidentRequest(), null);
        controller.reportIncident(incidentRequest(), null);

        verify(incidentUseCase, times(2)).reportIncident(any(), any());
    }

    @Test
    void reportIncident_sameIdempotencyKeyTwice_callsUseCaseOnlyOnce() {
        // El caso real que esta cache existe para cerrar: sin ella, un reintento de red
        // creaba DOS registros de incidencia identicos (a diferencia de /confirm, aqui no
        // hay ningun estado en la factura que lo impida).
        String idempotencyKey = "incident-key-456";

        controller.reportIncident(incidentRequest(), idempotencyKey);
        controller.reportIncident(incidentRequest(), idempotencyKey);

        verify(incidentUseCase, times(1)).reportIncident(any(), any());
    }

    @Test
    void reportIncident_and_confirmDelivery_withSameKeyValue_doNotCollide() {
        // idempotencyCacheKey() antepone el nombre del endpoint a la clave del cliente
        // (ver DriverDeliveryController): la misma clave usada en los dos endpoints no debe
        // hacer que uno "preste" su cache al otro.
        when(confirmUseCase.confirmDelivery(any(), any())).thenReturn(true);
        String sameKey = "shared-key";

        controller.confirmDelivery(confirmRequest(), sameKey);
        controller.reportIncident(incidentRequest(), sameKey);

        verify(confirmUseCase, times(1)).confirmDelivery(any(), any());
        verify(incidentUseCase, times(1)).reportIncident(any(), any());
    }
}
