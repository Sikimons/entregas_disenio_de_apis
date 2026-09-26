package com.ruta.deliverypin.infrastructure.adapter.in.web;

import com.ruta.deliverypin.domain.model.InvoiceLine;
import com.ruta.deliverypin.infrastructure.adapter.out.invoice.LocalInvoiceAdapter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/invoices")
public class AdminInvoiceController {
    private final LocalInvoiceAdapter invoices;
    public AdminInvoiceController(LocalInvoiceAdapter invoices) { this.invoices = invoices; }
    public record Product(@NotBlank String description, @NotNull @Positive Double quantity) {}
    public record CreateInvoice(@NotBlank @Size(max=60) String number, @NotBlank @Size(max=255) String partnerName,
        @Size(max=255) String deliveryAddress, @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @DecimalMin("-180") @DecimalMax("180") Double longitude, boolean requiresPin,
        @NotEmpty List<@Valid Product> products) {}

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(defaultValue="") String q) { return invoices.list(q); }
    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody CreateInvoice request) {
        return invoices.create(request.number(), request.partnerName(), request.deliveryAddress(), request.latitude(), request.longitude(),
            request.requiresPin(), request.products().stream().map(p -> new InvoiceLine(null, p.description(), p.quantity())).toList());
    }
    @PostMapping("/{id}/publish")
    public Map<String, Object> publish(@PathVariable Long id) { return invoices.publish(id); }
}
