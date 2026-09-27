package com.ruta.deliverypin.infrastructure.seed;

import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.port.out.DriverRepositoryPort;
import com.ruta.deliverypin.domain.port.out.PasswordEncoderPort;
import com.ruta.deliverypin.infrastructure.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Auditoria tecnica (Tanda 2): "infrastructure.seed" ya no es solo un atajo de desarrollo
 * -- el propio equipo confirmo que, sin ERP real que conectar, el primer arranque en
 * produccion tambien carga datos por aqui (un solo uso, ver README/deploy/docker-compose.yml).
 * Sin ningun test propio, este runner tenia 0% de cobertura.
 *
 * No se ejercita aqui la rama "one-shot" (System.exit real): forzarla mataria el proceso de
 * Surefire que corre el resto de la suite. Se deja fuera a proposito, no por descuido.
 */
class InitializerTest {

    private final DriverRepositoryPort driverRepository = mock(DriverRepositoryPort.class);
    private final PasswordEncoderPort passwordEncoder = mock(PasswordEncoderPort.class);
    private final InvoiceFixtureLoader invoiceFixtureLoader = mock(InvoiceFixtureLoader.class);
    private final ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);

    private Initializer initializerWithFile(String file) {
        AppProperties properties = new AppProperties();
        properties.getSeed().setFile(file);
        properties.getSeed().setOneShot(false);
        return new Initializer(driverRepository, passwordEncoder, invoiceFixtureLoader, properties, context);
    }

    @Test
    void run_driverDoesNotExist_createsSampleDriverAndLoadsFixtures() throws Exception {
        when(driverRepository.existsByUsername("conductor")).thenReturn(false);
        when(passwordEncoder.encode("conductor123")).thenReturn("hashed");

        initializerWithFile("file:/app/demo/invoices.json").run(null);

        verify(driverRepository).save(argThatIsSampleDriver());
        verify(invoiceFixtureLoader).load("file:/app/demo/invoices.json");
        verifyNoInteractions(context); // oneShot=false: nunca debe intentar terminar el proceso.
    }

    @Test
    void run_driverAlreadyExists_doesNotCreateItAgain() throws Exception {
        when(driverRepository.existsByUsername("conductor")).thenReturn(true);

        initializerWithFile("file:/app/demo/invoices.json").run(null);

        verify(driverRepository, never()).save(any());
        verify(invoiceFixtureLoader).load(any());
    }

    @Test
    void run_usesTheConfiguredSeedFile_notAHardcodedPath() throws Exception {
        // Si en produccion se apunta APP_SEED_FILE a un JSON propio (no demo/invoices.json),
        // este runner debe respetarlo tal cual, sin asumir la ruta de muestra.
        when(driverRepository.existsByUsername("conductor")).thenReturn(true);

        initializerWithFile("file:/opt/ruta/data/produccion-inicial.json").run(null);

        verify(invoiceFixtureLoader).load(eq("file:/opt/ruta/data/produccion-inicial.json"));
    }

    private Driver argThatIsSampleDriver() {
        return org.mockito.ArgumentMatchers.argThat(driver ->
                driver.getUsername().equals("conductor") && driver.getPasswordHash().equals("hashed"));
    }
}
