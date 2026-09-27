package com.ruta.deliverypin.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Sigue en JdbcTemplate a proposito (Fase8, ver RA4): es una carga masiva de datos de
 * siembra que debe preservar los ids y el orden exactos de demo/invoices.json (los
 * scripts de siembra/pruebas de carga los referencian) y despues resincronizar la secuencia
 * de Postgres. Con GenerationType.IDENTITY, JPA nunca permite fijar el id al insertar
 * (siempre pide a la base que lo genere), asi que este caso concreto de "insert masivo
 * con id explicito" no tiene equivalente JPA razonable. LocalInvoiceAdapter y
 * OperationalCostInputAdapter -el camino real de negocio- ya usan JPA.
 *
 * @Order(2) explicito: debe correr antes que SeedExitRunner (ver su Javadoc).
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Order(2)
public class SeedInvoiceLoader implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final TransactionTemplate transactions;
    private final ResourceLoader resources;

    @Value("${app.seed.file:file:/app/demo/invoices.json}")
    private String file;

    public SeedInvoiceLoader(JdbcTemplate jdbc, ObjectMapper mapper, TransactionTemplate transactions, ResourceLoader resources) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.transactions = transactions;
        this.resources = resources;
    }

    private Object value(JsonNode row, String key) {
        return mapper.convertValue(row.get(key), Object.class);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        JsonNode data;
        try (var input = resources.getResource(file).getInputStream()) {
            data = mapper.readTree(input);
        }

        transactions.executeWithoutResult(status -> {
            jdbc.execute("LOCK TABLE delivery_invoice IN EXCLUSIVE MODE");
            if (jdbc.queryForObject("SELECT count(*) FROM delivery_invoice", Long.class) > 0) {
                return;
            }

            for (var row : data) {
                jdbc.update("""
                        INSERT INTO delivery_invoice (id, number, partner_name, delivery_address, expected_latitude, expected_longitude,
                          invoice_date, state, requires_pin, pin, confirmed, confirmed_at, latitude, longitude, driver_name)
                        VALUES (?, ?, ?, ?, ?, ?, CAST(? AS date), ?, ?, ?, ?, CAST(? AS timestamp), ?, ?, ?)
                        """,
                        value(row, "id"),
                        value(row, "number"),
                        value(row, "partner_name"),
                        value(row, "delivery_address"),
                        value(row, "expected_latitude"),
                        value(row, "expected_longitude"),
                        value(row, "invoice_date"),
                        value(row, "state"),
                        value(row, "requires_pin"),
                        value(row, "pin"),
                        value(row, "confirmed"),
                        value(row, "confirmed_at"),
                        value(row, "latitude"),
                        value(row, "longitude"),
                        value(row, "driver_name")
                );

                for (var line : row.path("lines")) {
                    jdbc.update(
                            "INSERT INTO delivery_invoice_line (id, invoice_id, description, quantity) VALUES (?, ?, ?, ?)",
                            value(line, "id"),
                            value(row, "id"),
                            value(line, "description"),
                            value(line, "quantity")
                    );
                }
            }

            jdbc.execute("SELECT setval(pg_get_serial_sequence('delivery_invoice','id'), COALESCE((SELECT max(id) FROM delivery_invoice), 1))");
            jdbc.execute("SELECT setval(pg_get_serial_sequence('delivery_invoice_line','id'), COALESCE((SELECT max(id) FROM delivery_invoice_line), 1))");
        });
    }
}
