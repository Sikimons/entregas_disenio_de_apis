package com.ruta.deliverypin.infrastructure.adapter.in.web;

import com.ruta.deliverypin.domain.model.InvoiceLine;
import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.domain.port.in.ManageInvoicesUseCase;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.AdminInvoiceResponse;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Depende del puerto de entrada ManageInvoicesUseCase, no del adaptador de salida
 * LocalInvoiceAdapter directamente (correccion de la violacion hexagonal senalada
 * en la evaluacion: RA2/RA4).
 */
@Tag(name = "Facturas (admin)", description = "Alta, publicacion y consulta de facturas desde el panel administrativo (incluye el PIN vigente).")
@RestController
@RequestMapping("/api/v1/admin/invoices")
public class AdminInvoiceController {

    private final ManageInvoicesUseCase manageInvoicesUseCase;

    public AdminInvoiceController(ManageInvoicesUseCase manageInvoicesUseCase) {
        this.manageInvoicesUseCase = manageInvoicesUseCase;
    }

    public record Product(@NotBlank String description, @NotNull @Positive Double quantity) {}

    public record CreateInvoice(@NotBlank @Size(max = 60) String number, @NotBlank @Size(max = 255) String partnerName,
        @Size(max = 255) String deliveryAddress, @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @DecimalMin("-180") @DecimalMax("180") Double longitude, boolean requiresPin,
        @NotEmpty List<@Valid Product> products) {}

    @Operation(summary = "Listar facturas", description = "Facturas filtradas por numero o cliente, paginado (tamano maximo 100), incluido su estado y PIN vigente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de facturas"),
            @ApiResponse(responseCode = "400", description = "'size' supera el maximo permitido")
    })
    @GetMapping
    public PageResponse<AdminInvoiceResponse> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var result = manageInvoicesUseCase.list(q, new PageRequest(page, size));
        return PageResponse.from(result, AdminInvoiceResponse::from);
    }

    @Operation(summary = "Crear una factura", description = "Crea la factura en borrador, con sus lineas de producto. No genera PIN todavia: eso ocurre al publicarla.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Factura creada; el header Location apunta al recurso"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos (campos requeridos, coordenadas fuera de rango, sin productos)"),
            @ApiResponse(responseCode = "409", description = "Ya existe una factura con ese numero")
    })
    @PostMapping
    public ResponseEntity<AdminInvoiceResponse> create(@Valid @RequestBody CreateInvoice request, UriComponentsBuilder uriBuilder) {
        var created = manageInvoicesUseCase.create(new ManageInvoicesUseCase.CreateInvoiceCommand(
                request.number(), request.partnerName(), request.deliveryAddress(), request.latitude(), request.longitude(),
                request.requiresPin(), request.products().stream().map(p -> new InvoiceLine(null, p.description(), p.quantity())).toList()
        ));
        var location = uriBuilder.path("/api/v1/admin/invoices/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(AdminInvoiceResponse.from(created));
    }

    @Operation(summary = "Publicar una factura", description = "Genera el PIN de 6 digitos y deja la factura lista para que el conductor la confirme. "
            + "El PIN se debe comunicar al cliente por un canal operativo aparte (Fase1 §3).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Factura publicada, con su PIN"),
            @ApiResponse(responseCode = "400", description = "La factura no existe o ya estaba publicada")
    })
    @PostMapping("/{id}/publish")
    public AdminInvoiceResponse publish(@PathVariable Long id) {
        return AdminInvoiceResponse.from(manageInvoicesUseCase.publish(id));
    }
}
