package com.ruta.deliverypin.infrastructure.adapter.out.cost;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.YearMonth;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integracion con un Postgres REAL (Testcontainers): cubre justamente lo que un
 * mock del repositorio no puede probar de verdad -- que el upsert de horas de soporte
 * (N7, docs/EVALUACION_TECNICA.md §18) es atomico bajo concurrencia real. Antes de esta
 * correccion, "findById -> setear -> save" tenia una ventana de carrera: dos guardados
 * concurrentes para el mismo mes podian leer los dos "no existe" y el segundo insert
 * terminaba en una violacion de clave primaria (month duplicado).
 */
@SpringBootTest
@Testcontainers
class OperationalCostInputAdapterIntegrationTest {

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
    private OperationalCostInputAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void concurrentSaves_sameMonth_neverThrowAndLeaveExactlyOneRow() throws InterruptedException {
        YearMonth month = YearMonth.of(2026, 9);
        int attempts = 10;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger failures = new AtomicInteger();

        for (int i = 1; i <= attempts; i++) {
            double hours = i;
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    adapter.saveSupportHours(month, hours);
                } catch (Exception ex) {
                    failures.incrementAndGet();
                }
            });
        }

        ready.await();
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // El punto de esta prueba: ninguna de las 10 escrituras concurrentes revienta con una
        // clave duplicada (lo que pasaba con "findById -> setear -> save").
        assertThat(failures.get()).isZero();

        Integer rowCount = jdbc.queryForObject(
                "select count(*) from operational_cost_input where month = ?", Integer.class, month.atDay(1));
        assertThat(rowCount).isEqualTo(1);

        double savedHours = adapter.findSupportHours(month);
        assertThat(savedHours).isBetween(1.0, 10.0);
    }

    @Test
    void saveSupportHours_updatesExistingMonth() {
        YearMonth month = YearMonth.of(2026, 10);

        adapter.saveSupportHours(month, 5.0);
        adapter.saveSupportHours(month, 12.5);

        assertThat(adapter.findSupportHours(month)).isEqualTo(12.5);
    }
}
