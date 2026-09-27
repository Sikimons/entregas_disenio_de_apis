package com.ruta.deliverypin.infrastructure.adapter.out.invoice;

import com.ruta.deliverypin.domain.exception.DeliveryRejectedException;
import com.ruta.deliverypin.domain.exception.InvalidPinException;
import com.ruta.deliverypin.domain.model.AdminInvoiceView;
import com.ruta.deliverypin.domain.model.GeoLocation;
import com.ruta.deliverypin.domain.model.InvoiceLine;
import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository.SpringDataInvoiceJpaRepository;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository.SpringDataInvoiceLineJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de integracion con un Postgres REAL (Testcontainers), no un mock de JdbcTemplate:
 * cubre justamente lo que un mock no puede probar de verdad -- el bloqueo por PIN persistido
 * en la base (Fase1 §5.1) y que dos confirmaciones concurrentes sobre la misma factura no
 * confirmen ambas (el SELECT ... FOR UPDATE de locked() en LocalInvoiceAdapter).
 * El esquema lo aplica Flyway al arrancar el contexto (V1__baseline.sql, Fase4): no hay
 * schema.sql ni ddl-auto de por medio.
 */
@SpringBootTest
@Testcontainers
class LocalInvoiceAdapterIntegrationTest {

    @Container
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // SecretsGuardRunner exige un secreto real (o APP_SEED_ENABLED=true, que arrastraria
        // infrastructure.seed.Initializer y un archivo que este test no necesita): se le da un secreto
        // de prueba propio, sin tocar el arranque normal de la app.
        registry.add("app.jwt.secret", () -> "test_secret_for_testcontainers_integration_min_32_chars_long");
        registry.add("app.admin.bootstrap-password", () -> "testcontainers_admin_password");
    }

    @Autowired
    private LocalInvoiceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @SpyBean
    private SpringDataInvoiceLineJpaRepository lineRepository;

    @SpyBean
    private SpringDataInvoiceJpaRepository invoiceRepository;

    @BeforeEach
    void cleanDatabase() {
        // Aislar cada test: delivery_log referencia delivery_invoice/app_user (FK de la Fase4),
        // hay que borrar en ese orden.
        jdbc.update("DELETE FROM delivery_log");
        jdbc.update("DELETE FROM delivery_invoice_line");
        jdbc.update("DELETE FROM delivery_invoice");
    }

    private AdminInvoiceView createAndPublish(String number) {
        adapter.create(number, "Cliente Testcontainers", "Direccion X", null, null, true,
                List.of(new InvoiceLine(null, "item", 1.0)), "admin");
        AdminInvoiceView draft = adapter.list(number, new PageRequest(0, 20)).content().get(0);
        return adapter.publish(draft.id(), "admin");
    }

    @Test
    void createAndPublish_recordCreatedByAndPublishedBy() {
        // Tanda 2 (auditoria tecnica, Fase2 §6: "no se registra quien creo o publico cada
        // factura"). createAndPublish() usa el mismo username ("admin") para ambos pasos a
        // proposito de la fixture; este test verifica que cada llamada persiste el suyo.
        AdminInvoiceView published = createAndPublish("F-IT-009");

        assertThat(published.createdBy()).isEqualTo("admin");
        assertThat(published.publishedBy()).isEqualTo("admin");
    }

    @Test
    void createPublishConfirm_withCorrectPin_confirmsDelivery() {
        AdminInvoiceView published = createAndPublish("F-IT-001");
        assertThat(published.state()).isEqualTo("posted");
        assertThat(published.pin()).matches("\\d{6}");

        adapter.confirmDelivery(published.id(), published.pin(), new GeoLocation(-2.17, -79.92), "Conductor IT");

        AdminInvoiceView confirmed = adapter.list("F-IT-001", new PageRequest(0, 20)).content().get(0);
        assertThat(confirmed.confirmed()).isTrue();
    }

    @Test
    void confirmDelivery_wrongPinFiveTimes_locksInvoice() {
        AdminInvoiceView published = createAndPublish("F-IT-002");

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> adapter.confirmDelivery(published.id(), "000000".equals(published.pin()) ? "111111" : "000000",
                    new GeoLocation(-2.17, -79.92), "Conductor IT"))
                    .isInstanceOf(InvalidPinException.class);
            adapter.registerFailedPinAttempt(published.id());
        }

        // La sexta, incluso con el PIN correcto, debe rechazarse por bloqueo (no por PIN invalido).
        assertThatThrownBy(() -> adapter.confirmDelivery(published.id(), published.pin(), new GeoLocation(-2.17, -79.92), "Conductor IT"))
                .isInstanceOf(DeliveryRejectedException.class)
                .hasMessageContaining("bloqueada");
    }

    @Test
    void concurrentConfirmations_onlyOneSucceeds() throws InterruptedException {
        AdminInvoiceView published = createAndPublish("F-IT-003");
        int attempts = 8;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        for (int i = 0; i < attempts; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    adapter.confirmDelivery(published.id(), published.pin(), new GeoLocation(-2.17, -79.92), "Conductor IT");
                    succeeded.incrementAndGet();
                } catch (Exception ex) {
                    rejected.incrementAndGet();
                }
            });
        }

        ready.await();
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(succeeded.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(attempts - 1);

        AdminInvoiceView finalState = adapter.list("F-IT-003", new PageRequest(0, 20)).content().get(0);
        assertThat(finalState.confirmed()).isTrue();
    }

    @Test
    void findInvoiceLines_secondCall_isServedFromCache_notFromRepository() {
        // Cache Aside (Fase8): las lineas de una factura no cambian tras crearla, asi que
        // la segunda llamada no debe volver a golpear el repositorio (@Cacheable en
        // LocalInvoiceAdapter.findInvoiceLines).
        adapter.create("F-IT-004", "Cliente Cache", "Direccion Y", null, null, true,
                List.of(new InvoiceLine(null, "item cacheado", 3.0)), "admin");
        Long invoiceId = adapter.list("F-IT-004", new PageRequest(0, 20)).content().get(0).id();
        Mockito.clearInvocations(lineRepository);

        List<InvoiceLine> first = adapter.findInvoiceLines(invoiceId);
        List<InvoiceLine> second = adapter.findInvoiceLines(invoiceId);

        assertThat(first).hasSize(1);
        assertThat(second).isEqualTo(first);
        Mockito.verify(lineRepository, Mockito.times(1)).findByInvoice_IdOrderById(invoiceId);
    }

    @Test
    void findInvoiceLines_emptyResult_isNotCached_repositoryQueriedEachTime() {
        // N7 (docs/EVALUACION_TECNICA.md §18): antes @Cacheable tambien cacheaba una lista
        // vacia (cache "negativa"); con "unless" en la anotacion, un resultado vacio no se
        // guarda, asi que la siguiente consulta vuelve a tocar la base en vez de arrastrar
        // el vacio hasta que expire el TTL.
        adapter.create("F-IT-005", "Cliente Sin Lineas", "Direccion W", null, null, true, List.of(), "admin");
        Long invoiceId = adapter.list("F-IT-005", new PageRequest(0, 20)).content().get(0).id();
        Mockito.clearInvocations(lineRepository);

        List<InvoiceLine> first = adapter.findInvoiceLines(invoiceId);
        List<InvoiceLine> second = adapter.findInvoiceLines(invoiceId);

        assertThat(first).isEmpty();
        assertThat(second).isEmpty();
        Mockito.verify(lineRepository, Mockito.times(2)).findByInvoice_IdOrderById(invoiceId);
    }

    @Test
    void findExpectedLocation_presentResult_isCached_secondCallServedFromCache() {
        // Regresion real detectada con verify_delivery.py (no con mocks, ver el comentario
        // en LocalInvoiceAdapter.findExpectedLocation): al arreglar la cache negativa, un
        // "unless" mal escrito ("#result.isEmpty()" sobre un Optional, que Spring desenvuelve
        // antes de evaluar la SpEL) rompia esta llamada con SpelEvaluationException cada vez
        // que la ubicacion SI estaba presente -- es decir, en el caso mas comun.
        adapter.create("F-IT-007", "Cliente Con Ubicacion", "Direccion U", -2.171, -79.922, true, List.of(), "admin");
        Long invoiceId = adapter.list("F-IT-007", new PageRequest(0, 20)).content().get(0).id();
        Mockito.clearInvocations(invoiceRepository);

        Optional<GeoLocation> first = adapter.findExpectedLocation(invoiceId);
        Optional<GeoLocation> second = adapter.findExpectedLocation(invoiceId);

        assertThat(first).contains(new GeoLocation(-2.171, -79.922));
        assertThat(second).isEqualTo(first);
        Mockito.verify(invoiceRepository, Mockito.times(1)).findById(invoiceId);
    }

    @Test
    void findExpectedLocation_emptyResult_isNotCached_repositoryQueriedEachTime() {
        adapter.create("F-IT-008", "Cliente Sin Ubicacion", "Direccion T", null, null, true, List.of(), "admin");
        Long invoiceId = adapter.list("F-IT-008", new PageRequest(0, 20)).content().get(0).id();
        Mockito.clearInvocations(invoiceRepository);

        Optional<GeoLocation> first = adapter.findExpectedLocation(invoiceId);
        Optional<GeoLocation> second = adapter.findExpectedLocation(invoiceId);

        assertThat(first).isEmpty();
        assertThat(second).isEmpty();
        Mockito.verify(invoiceRepository, Mockito.times(2)).findById(invoiceId);
    }

    @Test
    void list_withUnderscoreInQuery_treatsItAsLiteralNotAsWildcard() {
        // N3/N7 (docs/EVALUACION_TECNICA.md §18): sin escapar "_" (comodin de un solo
        // caracter en LIKE), buscar "F-IT-A_1" tambien traia "F-IT-AB1".
        adapter.create("F-IT-A_1", "Cliente Escape", "Direccion Z", null, null, true, List.of(), "admin");
        adapter.create("F-IT-AB1", "Cliente Escape", "Direccion Z", null, null, true, List.of(), "admin");

        List<AdminInvoiceView> matches = adapter.list("F-IT-A_1", new PageRequest(0, 20)).content();

        assertThat(matches).extracting(AdminInvoiceView::number).containsExactly("F-IT-A_1");
    }
}
