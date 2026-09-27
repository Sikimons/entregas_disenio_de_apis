package com.ruta.deliverypin.infrastructure.adapter.in.web;

import com.ruta.deliverypin.domain.model.DeliveryPhoto;
import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.domain.model.StoredPhoto;
import com.ruta.deliverypin.domain.port.in.ConfirmDeliveryUseCase;
import com.ruta.deliverypin.domain.port.in.GetDriverDeliveryPhotoUseCase;
import com.ruta.deliverypin.domain.port.in.ListDriverDeliveryHistoryUseCase;
import com.ruta.deliverypin.domain.port.in.ListInvoiceLinesUseCase;
import com.ruta.deliverypin.domain.port.in.ReportIncidentUseCase;
import com.ruta.deliverypin.domain.port.in.SearchPendingInvoicesUseCase;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.ConfirmDeliveryRequest;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.ConfirmDeliveryResponse;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.DeliveryAttemptResponse;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.InvoiceLineResponse;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.InvoiceResponse;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.PageResponse;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.ReportIncidentRequest;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.ReportIncidentResponse;
import com.ruta.deliverypin.infrastructure.adapter.in.web.security.CurrentDriverResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Pantalla unica del conductor: buscar factura y confirmar entrega con PIN + GPS,
 * o reportar una incidencia cuando no se puede entregar.
 */
@Tag(name = "Entregas del conductor", description = "Busqueda de facturas pendientes, confirmacion de entrega con PIN/GPS, incidencias e historial propio.")
@RestController
@RequestMapping("/api/v1/driver")
public class DriverDeliveryController {

    private final SearchPendingInvoicesUseCase searchPendingInvoicesUseCase;
    private final ListInvoiceLinesUseCase listInvoiceLinesUseCase;
    private final ConfirmDeliveryUseCase confirmDeliveryUseCase;
    private final ReportIncidentUseCase reportIncidentUseCase;
    private final ListDriverDeliveryHistoryUseCase listDriverDeliveryHistoryUseCase;
    private final GetDriverDeliveryPhotoUseCase getDriverDeliveryPhotoUseCase;
    private final CurrentDriverResolver currentDriverResolver;

    public DriverDeliveryController(
            SearchPendingInvoicesUseCase searchPendingInvoicesUseCase,
            ListInvoiceLinesUseCase listInvoiceLinesUseCase,
            ConfirmDeliveryUseCase confirmDeliveryUseCase,
            ReportIncidentUseCase reportIncidentUseCase,
            ListDriverDeliveryHistoryUseCase listDriverDeliveryHistoryUseCase,
            GetDriverDeliveryPhotoUseCase getDriverDeliveryPhotoUseCase,
            CurrentDriverResolver currentDriverResolver
    ) {
        this.searchPendingInvoicesUseCase = searchPendingInvoicesUseCase;
        this.listInvoiceLinesUseCase = listInvoiceLinesUseCase;
        this.confirmDeliveryUseCase = confirmDeliveryUseCase;
        this.reportIncidentUseCase = reportIncidentUseCase;
        this.listDriverDeliveryHistoryUseCase = listDriverDeliveryHistoryUseCase;
        this.getDriverDeliveryPhotoUseCase = getDriverDeliveryPhotoUseCase;
        this.currentDriverResolver = currentDriverResolver;
    }

    @Operation(summary = "Buscar facturas pendientes de entrega", description = "Facturas publicadas, con PIN habilitado y aun no confirmadas, filtradas por numero o cliente.")
    @ApiResponse(responseCode = "200", description = "Facturas que coinciden con la busqueda (hasta 20)")
    @GetMapping("/invoices")
    public List<InvoiceResponse> search(@RequestParam("q") String query) {
        return searchPendingInvoicesUseCase.search(query).stream().map(InvoiceResponse::from).toList();
    }

    @Operation(summary = "Lineas de una factura", description = "Productos vendidos en la factura, para el checklist de entrega del conductor.")
    @ApiResponse(responseCode = "200", description = "Lineas de la factura")
    @GetMapping("/invoices/{id}/lines")
    public List<InvoiceLineResponse> lines(@PathVariable Long id) {
        return listInvoiceLinesUseCase.list(id).stream().map(InvoiceLineResponse::from).toList();
    }

    @Operation(summary = "Confirmar una entrega", description = "Bloquea la factura, valida el PIN y guarda la evidencia (foto, GPS) en una sola transaccion. "
            + "Los datos de la factura (numero, cliente, direccion) se resuelven desde la base por invoiceId, no desde este cuerpo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrega confirmada; se guarda la evidencia"),
            @ApiResponse(responseCode = "400", description = "Foto invalida (tamano, formato) o factura no encontrada/no publicada"),
            @ApiResponse(responseCode = "409", description = "La entrega ya habia sido confirmada"),
            @ApiResponse(responseCode = "422", description = "PIN incorrecto"),
            @ApiResponse(responseCode = "503", description = "Circuito abierto: el ERP simulado no esta disponible")
    })
    @PostMapping("/deliveries/confirm")
    public ConfirmDeliveryResponse confirmDelivery(@Valid @RequestBody ConfirmDeliveryRequest request) {
        Driver driver = currentDriverResolver.resolve();
        DeliveryPhoto photo = new DeliveryPhoto(request.photoBase64(), request.photoFilename(), request.photoContentType());
        var command = new ConfirmDeliveryUseCase.ConfirmDeliveryCommand(
                request.invoiceId(), request.pin(), request.latitude(), request.longitude(), photo
        );
        boolean photoUploaded = confirmDeliveryUseCase.confirmDelivery(command, driver);
        String message = "Entrega confirmada correctamente. Evidencia guardada.";
        return new ConfirmDeliveryResponse(true, message, photoUploaded);
    }

    @Operation(summary = "Reportar una incidencia", description = "Registra que la entrega no se pudo completar (motivo y ubicacion), sin bloquear ni cancelar la factura.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incidencia registrada"),
            @ApiResponse(responseCode = "400", description = "La factura indicada no existe")
    })
    @PostMapping("/deliveries/incident")
    public ReportIncidentResponse reportIncident(@Valid @RequestBody ReportIncidentRequest request) {
        Driver driver = currentDriverResolver.resolve();
        var command = new ReportIncidentUseCase.ReportIncidentCommand(
                request.invoiceId(), request.invoiceNumber(), request.partnerName(), request.deliveryAddress(),
                request.reason(), request.notes(), request.latitude(), request.longitude()
        );
        reportIncidentUseCase.reportIncident(command, driver);
        return new ReportIncidentResponse(true, "Incidencia reportada correctamente.");
    }

    @Operation(summary = "Historial propio de entregas", description = "Entregas e incidencias del conductor autenticado, paginado (tamano maximo 100).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina del historial"),
            @ApiResponse(responseCode = "400", description = "'size' supera el maximo permitido")
    })
    @GetMapping("/deliveries/history")
    public PageResponse<DeliveryAttemptResponse> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Driver driver = currentDriverResolver.resolve();
        var result = listDriverDeliveryHistoryUseCase.list(driver.getId(), new PageRequest(page, size));
        return PageResponse.from(result, DeliveryAttemptResponse::from);
    }

    @Operation(summary = "Foto de una entrega propia", description = "Devuelve la foto de evidencia de una entrega del conductor autenticado (filtrada por driverId, evita IDOR).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagen de la evidencia (JPEG o PNG)"),
            @ApiResponse(responseCode = "404", description = "No existe o no pertenece al conductor autenticado")
    })
    @GetMapping("/deliveries/{id}/photo")
    public ResponseEntity<byte[]> deliveryPhoto(@PathVariable Long id) {
        Driver driver = currentDriverResolver.resolve();
        return getDriverDeliveryPhotoUseCase.getPhoto(id, driver.getId())
                .map(this::toImageResponse)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ResponseEntity<byte[]> toImageResponse(StoredPhoto photo) {
        MediaType mediaType = photo.contentType() != null
                ? MediaType.parseMediaType(photo.contentType())
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(mediaType).body(photo.data());
    }
}
