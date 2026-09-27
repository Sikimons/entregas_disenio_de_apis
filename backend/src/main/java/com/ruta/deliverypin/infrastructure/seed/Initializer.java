package com.ruta.deliverypin.infrastructure.seed;

import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.model.Role;
import com.ruta.deliverypin.domain.port.out.DriverRepositoryPort;
import com.ruta.deliverypin.domain.port.out.PasswordEncoderPort;
import com.ruta.deliverypin.infrastructure.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Unico punto de entrada para poblar datos de muestra en el arranque: se activa con una
 * sola variable (app.seed.enabled) y, al activarse, carga en orden todo lo que corresponde
 * -- el conductor de muestra y las facturas de demo/invoices.json -- y termina el proceso
 * si app.seed.one-shot=true (para el servicio "seed" del compose, que siembra y sale en
 * vez de quedar escuchando en el puerto 8080).
 *
 * Antes esto eran tres beans separados (uno por paso), coordinados con @Order porque
 * Spring no garantiza el orden de arranque entre beans sin @Order explicito (bug real de
 * reproducibilidad ya corregido, ver historial en docs/EVALUACION_TECNICA.md §18). Con un
 * solo runner, los tres pasos son simples llamadas secuenciales dentro de un mismo metodo:
 * ya no hace falta @Order entre ellos, y desaparece esa clase entera de bugs.
 *
 * Solo queda una relacion de orden real: debe correr despues de AdminBootstrapRunner
 * (@Order(0), sin condicion, siempre crea el admin), porque en modo "one-shot" este
 * runner termina el proceso -- si corriera antes, el admin nunca llegaria a crearse.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Order(1)
public class Initializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(Initializer.class);
    private static final String SAMPLE_DRIVER_USERNAME = "conductor";
    private static final String SAMPLE_DRIVER_PASSWORD = "conductor123";

    private final DriverRepositoryPort driverRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final InvoiceFixtureLoader invoiceFixtureLoader;
    private final AppProperties appProperties;
    private final ConfigurableApplicationContext context;

    public Initializer(
            DriverRepositoryPort driverRepository,
            PasswordEncoderPort passwordEncoder,
            InvoiceFixtureLoader invoiceFixtureLoader,
            AppProperties appProperties,
            ConfigurableApplicationContext context
    ) {
        this.driverRepository = driverRepository;
        this.passwordEncoder = passwordEncoder;
        this.invoiceFixtureLoader = invoiceFixtureLoader;
        this.appProperties = appProperties;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        bootstrapSampleDriver();
        invoiceFixtureLoader.load(appProperties.getSeed().getFile());
        if (appProperties.getSeed().isOneShot()) {
            System.exit(SpringApplication.exit(context, () -> 0));
        }
    }

    private void bootstrapSampleDriver() {
        if (driverRepository.existsByUsername(SAMPLE_DRIVER_USERNAME)) {
            return;
        }
        Driver sampleDriver = Driver.createNew(
                SAMPLE_DRIVER_USERNAME, passwordEncoder.encode(SAMPLE_DRIVER_PASSWORD), "Conductor Demo", Role.CONDUCTOR
        );
        driverRepository.save(sampleDriver);
        log.info("Conductor de muestra creado: {}", SAMPLE_DRIVER_USERNAME);
    }
}
