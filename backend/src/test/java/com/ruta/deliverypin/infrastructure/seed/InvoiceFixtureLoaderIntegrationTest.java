package com.ruta.deliverypin.infrastructure.seed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integracion con Postgres real (auditoria tecnica, Tanda 2): cubre justo lo que
 * un test con mocks de JdbcTemplate no puede probar de verdad -- que las sentencias SQL
 * crudas de InvoiceFixtureLoader son validas contra el esquema real (Flyway) y que la
 * resincronizacion de la secuencia despues de insertar IDs explicitos deja la tabla lista
 * para que JPA (GenerationType.IDENTITY) siga insertando sin colisionar.
 *
 * Ahora relevante en produccion, no solo en desarrollo: sin ERP real que conectar, el primer
 * arranque en cualquier entorno (incluida produccion) carga por aqui el dataset inicial
 * (ver README/deploy/docker-compose.yml, servicio "seed" de un solo uso).
 */
@SpringBootTest
@Testcontainers
class InvoiceFixtureLoaderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "test_secret_for_testcontainers_integration_min_32_chars_long");
        registry.add("app.admin.bootstrap-password", () -> "testcontainers_admin_password");
    }

    @Autowired
    private InvoiceFixtureLoader loader;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("DELETE FROM delivery_log");
        jdbc.update("DELETE FROM delivery_invoice_line");
        jdbc.update("DELETE FROM delivery_invoice");
    }

    @Test
    void load_insertsInvoicesAndLines_withTheExplicitIdsFromTheFile() throws Exception {
        loader.load("classpath:seed/test-invoices.json");

        Long invoiceCount = jdbc.queryForObject("SELECT count(*) FROM delivery_invoice", Long.class);
        Long lineCount = jdbc.queryForObject("SELECT count(*) FROM delivery_invoice_line", Long.class);
        assertThat(invoiceCount).isEqualTo(2L);
        assertThat(lineCount).isEqualTo(2L);

        String number = jdbc.queryForObject("SELECT number FROM delivery_invoice WHERE id = 501", String.class);
        assertThat(number).isEqualTo("TEST/SEED/0001");
    }

    @Test
    void load_alreadyHasData_doesNotReimportOrDuplicate() throws Exception {
        // El propio README lo documenta: "si ya tienes facturas cargadas, volver a levantar
        // los servicios no las reemplaza ni vuelve a importar el JSON". Este test verifica
        // ese comportamiento contra Postgres real, no solo lo lee en la documentacion.
        loader.load("classpath:seed/test-invoices.json");
        loader.load("classpath:seed/test-invoices.json");

        Long invoiceCount = jdbc.queryForObject("SELECT count(*) FROM delivery_invoice", Long.class);
        assertThat(invoiceCount).isEqualTo(2L);
    }

    @Test
    void load_resyncsTheSequence_soANewJpaInsertNeverCollidesWithAnExplicitId() throws Exception {
        // El motivo real de que este loader exista en JdbcTemplate y no en JPA (ver su
        // propio Javadoc): preservar IDs explicitos del JSON. Sin el SELECT setval(...) del
        // final de load(), el primer INSERT posterior via JPA (GenerationType.IDENTITY,
        // que sigue la secuencia interna de Postgres) intentaria reutilizar un id ya usado
        // (1, 2, 3...) y fallaria por violar la clave primaria.
        loader.load("classpath:seed/test-invoices.json");

        Long newId = jdbc.queryForObject(
                "INSERT INTO delivery_invoice (number, partner_name, invoice_date, state, requires_pin, confirmed, failed_pin_attempts) "
                        + "VALUES ('POST-SEED', 'Cliente Nuevo', CURRENT_DATE, 'draft', true, false, 0) RETURNING id",
                Long.class);

        assertThat(newId).isGreaterThan(502L);
    }
}
