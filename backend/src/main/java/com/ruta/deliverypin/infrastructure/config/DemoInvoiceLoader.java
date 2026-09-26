package com.ruta.deliverypin.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Value;

@Component
@ConditionalOnProperty(name="app.demo.enabled", havingValue="true")
public class DemoInvoiceLoader implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final TransactionTemplate transactions;
    private final ResourceLoader resources;
    @Value("${app.demo.file:file:/app/demo/invoices.json}") private String file;
    public DemoInvoiceLoader(JdbcTemplate jdbc, ObjectMapper mapper, TransactionTemplate transactions, ResourceLoader resources) {
        this.jdbc = jdbc; this.mapper = mapper; this.transactions = transactions; this.resources = resources;
    }
    private Object value(JsonNode row, String key) { return mapper.convertValue(row.get(key), Object.class); }
    @Override public void run(ApplicationArguments args) throws Exception {
        JsonNode data;
        try (var input = resources.getResource(file).getInputStream()) { data = mapper.readTree(input); }
        transactions.executeWithoutResult(status -> {
            jdbc.execute("LOCK TABLE delivery_invoice IN EXCLUSIVE MODE");
            if (jdbc.queryForObject("SELECT count(*) FROM delivery_invoice", Long.class) > 0) return;
            for (var row : data) {
                jdbc.update("""
                    INSERT INTO delivery_invoice (id, number, partner_name, delivery_address, expected_latitude, expected_longitude,
                      invoice_date, state, requires_pin, pin, confirmed, confirmed_at, latitude, longitude, driver_name)
                    VALUES (?, ?, ?, ?, ?, ?, CAST(? AS date), ?, ?, ?, ?, CAST(? AS timestamp), ?, ?, ?)
                    """, value(row,"id"), value(row,"number"), value(row,"partner_name"), value(row,"delivery_address"),
                    value(row,"expected_latitude"), value(row,"expected_longitude"), value(row,"invoice_date"), value(row,"state"),
                    value(row,"requires_pin"), value(row,"pin"), value(row,"confirmed"), value(row,"confirmed_at"),
                    value(row,"latitude"), value(row,"longitude"), value(row,"driver_name"));
                for (var line : row.path("lines")) jdbc.update("INSERT INTO delivery_invoice_line (id, invoice_id, description, quantity) VALUES (?, ?, ?, ?)",
                    value(line,"id"), value(row,"id"), value(line,"description"), value(line,"quantity"));
            }
            jdbc.execute("SELECT setval(pg_get_serial_sequence('delivery_invoice','id'), COALESCE((SELECT max(id) FROM delivery_invoice), 1))");
            jdbc.execute("SELECT setval(pg_get_serial_sequence('delivery_invoice_line','id'), COALESCE((SELECT max(id) FROM delivery_invoice_line), 1))");
        });
    }
}
