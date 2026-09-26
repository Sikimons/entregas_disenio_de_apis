package com.ruta.deliverypin.infrastructure.adapter.out.invoice;

import com.ruta.deliverypin.domain.exception.DeliveryRejectedException;
import com.ruta.deliverypin.domain.model.GeoLocation;
import com.ruta.deliverypin.domain.model.Invoice;
import com.ruta.deliverypin.domain.model.InvoiceLine;
import com.ruta.deliverypin.domain.port.out.DeliveryConfirmationGatewayPort;
import com.ruta.deliverypin.domain.port.out.InvoiceQueryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Facturas, productos y confirmaciones en la base propia de la aplicacion. */
@Repository
public class LocalInvoiceAdapter implements InvoiceQueryPort, DeliveryConfirmationGatewayPort {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    public LocalInvoiceAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<Invoice> searchPendingDeliveryInvoices(String query) {
        String like = "%" + query + "%";
        return jdbc.query("""
            SELECT * FROM delivery_invoice WHERE requires_pin AND state = 'posted' AND NOT confirmed
            AND (number ILIKE ? OR partner_name ILIKE ?) ORDER BY invoice_date DESC, id DESC LIMIT 20
            """, (rs, n) -> new Invoice(rs.getLong("id"), rs.getString("number"), rs.getString("partner_name"),
                rs.getString("delivery_address"), rs.getString("invoice_date"), rs.getString("state"),
                rs.getObject("expected_latitude", Double.class), rs.getObject("expected_longitude", Double.class)), like, like);
    }

    @Override
    public List<InvoiceLine> findInvoiceLines(Long invoiceId) {
        return jdbc.query("SELECT id, description, quantity FROM delivery_invoice_line WHERE invoice_id = ? ORDER BY id",
            (rs, n) -> new InvoiceLine(rs.getLong("id"), rs.getString("description"), rs.getDouble("quantity")), invoiceId);
    }

    @Override
    public Optional<GeoLocation> findExpectedLocation(Long invoiceId) {
        return jdbc.query("SELECT expected_latitude, expected_longitude FROM delivery_invoice WHERE id = ? AND expected_latitude IS NOT NULL AND expected_longitude IS NOT NULL",
            (rs, n) -> new GeoLocation(rs.getDouble(1), rs.getDouble(2)), invoiceId).stream().findFirst();
    }

    private Map<String, Object> locked(Long id) {
        var rows = jdbc.queryForList("SELECT * FROM delivery_invoice WHERE id = ? FOR UPDATE", id);
        if (rows.isEmpty()) throw new DeliveryRejectedException("La factura no existe.");
        return rows.get(0);
    }

    @Override
    @Transactional
    public void confirmDelivery(Long invoiceId, String pin, GeoLocation location, String driverName) {
        var invoice = locked(invoiceId);
        if (!Boolean.TRUE.equals(invoice.get("requires_pin"))) throw new DeliveryRejectedException("Esta factura no requiere PIN de entrega.");
        if (!"posted".equals(invoice.get("state"))) throw new DeliveryRejectedException("La factura debe estar publicada para confirmar la entrega.");
        if (Boolean.TRUE.equals(invoice.get("confirmed"))) throw new DeliveryRejectedException("La entrega de esta factura ya fue confirmada.");
        if (pin == null || !pin.equals(invoice.get("pin"))) throw new DeliveryRejectedException("El PIN ingresado no es correcto.");
        jdbc.update("UPDATE delivery_invoice SET confirmed = true, confirmed_at = CURRENT_TIMESTAMP AT TIME ZONE 'UTC', latitude = ?, longitude = ?, driver_name = ? WHERE id = ?",
            location.latitude(), location.longitude(), driverName, invoiceId);
    }

    public List<Map<String, Object>> list(String query) {
        String like = "%" + query + "%";
        return jdbc.queryForList("SELECT * FROM delivery_invoice WHERE number ILIKE ? OR partner_name ILIKE ? ORDER BY id DESC LIMIT 1000", like, like);
    }

    @Transactional
    public Map<String, Object> publish(Long id) {
        var invoice = locked(id);
        if ("cancel".equals(invoice.get("state"))) throw new DeliveryRejectedException("No se puede publicar una factura cancelada.");
        String pin = (String) invoice.get("pin");
        if (Boolean.TRUE.equals(invoice.get("requires_pin")) && (pin == null || pin.isBlank())) pin = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        jdbc.update("UPDATE delivery_invoice SET state = 'posted', pin = ? WHERE id = ?", pin, id);
        return locked(id);
    }

    @Transactional
    public Map<String, Object> create(String number, String partnerName, String address, Double latitude, Double longitude,
                                      boolean requiresPin, List<InvoiceLine> lines) {
        Long id = jdbc.queryForObject("""
            INSERT INTO delivery_invoice (number, partner_name, delivery_address, expected_latitude, expected_longitude, requires_pin)
            VALUES (?, ?, ?, ?, ?, ?) RETURNING id
            """, Long.class, number, partnerName, address, latitude, longitude, requiresPin);
        for (var line : lines) jdbc.update("INSERT INTO delivery_invoice_line (invoice_id, description, quantity) VALUES (?, ?, ?)", id, line.description(), line.quantity());
        return locked(id);
    }
}
