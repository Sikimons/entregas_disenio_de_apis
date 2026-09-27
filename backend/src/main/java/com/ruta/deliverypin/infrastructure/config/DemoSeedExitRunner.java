package com.ruta.deliverypin.infrastructure.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Convierte al backend en un "job" de una sola pasada cuando se levanta solo para sembrar
 * datos de demostracion (deploy/docker-compose.yml y docker-compose.demo.yml, servicio
 * "seed", profile "demo"): corre Flyway + DemoInvoiceLoader + DemoDriverBootstrapRunner y
 * termina, en vez de quedar escuchando en el puerto 8080 con APP_DEMO_ENABLED=true (lo que
 * dejaba el SecretsGuardRunner desactivado de forma permanente en el backend real).
 *
 * Corrige un bug real de reproducibilidad encontrado en vivo (docs/EVALUACION_TECNICA.md §18,
 * verificado con "docker compose down -v && up --build" repetido): esta clase supo asumir
 * que, sin @Order en los demas ApplicationRunner/CommandLineRunner de app.demo.*, el
 * "ordenamiento estable de Spring" los ejecutaba por orden alfabetico de escaneo de
 * componentes (AdminBootstrapRunner, DemoDriverBootstrapRunner, DemoInvoiceLoader,
 * DemoSeedExitRunner). Eso es falso: sin @Order, todos quedan "empatados" en
 * Ordered.LOWEST_PRECEDENCE, y Spring desempata por el orden de registro de los bean
 * definitions durante el escaneo -- que no es alfabetico de forma garantizada y puede variar
 * entre builds de la misma imagen. En la practica, esto hacia que el seed a veces terminara
 * (System.exit) antes de que AdminBootstrapRunner/DemoDriverBootstrapRunner/DemoInvoiceLoader
 * llegaran a correr, dejando la base sin el admin ni el conductor de demostracion. Ahora los
 * tres declaran @Order(0)/@Order(1)/@Order(2) explicito, y esta clase se queda en
 * @Order(LOWEST_PRECEDENCE): un orden explicito siempre ordena antes que uno implicito, asi
 * que la garantia deja de depender de una casualidad del classpath.
 */
@Component
@ConditionalOnProperty(name = "app.demo.seed-only", havingValue = "true")
@Order(Ordered.LOWEST_PRECEDENCE)
public class DemoSeedExitRunner implements ApplicationRunner {

    private final ConfigurableApplicationContext context;

    public DemoSeedExitRunner(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
