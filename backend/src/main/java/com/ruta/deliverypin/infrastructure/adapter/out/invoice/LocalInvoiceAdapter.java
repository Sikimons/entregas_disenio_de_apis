package com.ruta.deliverypin.infrastructure.adapter.out.invoice;

import com.ruta.deliverypin.domain.exception.DeliveryAlreadyConfirmedException;
import com.ruta.deliverypin.domain.exception.DeliveryRejectedException;
import com.ruta.deliverypin.domain.exception.ErpUnavailableException;
import com.ruta.deliverypin.domain.exception.InvalidPinException;
import com.ruta.deliverypin.domain.model.AdminInvoiceView;
import com.ruta.deliverypin.domain.model.GeoLocation;
import com.ruta.deliverypin.domain.model.Invoice;
import com.ruta.deliverypin.domain.model.InvoiceLine;
import com.ruta.deliverypin.domain.port.out.DeliveryConfirmationGatewayPort;
import com.ruta.deliverypin.domain.port.out.InvoiceAdminPort;
import com.ruta.deliverypin.domain.port.out.InvoiceQueryPort;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.InvoiceJpaEntity;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.InvoiceLineJpaEntity;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository.SpringDataInvoiceJpaRepository;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository.SpringDataInvoiceLineJpaRepository;
import com.ruta.deliverypin.infrastructure.config.ResilienceProperties;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Simula el ERP vigente (Fase1 §3) sobre la propia base del sistema. Migrado en la Fase8
 * de JdbcTemplate/SQL directo a JPA (SpringDataInvoiceJpaRepository/SpringDataInvoiceLineJpaRepository +
 * InvoicePersistenceMapper), cerrando la doble estrategia de persistencia senalada en RA4:
 * ya no es el unico camino de acceso a datos del backend que no pasa por Spring Data.
 */
@Repository
public class LocalInvoiceAdapter implements InvoiceQueryPort, DeliveryConfirmationGatewayPort, InvoiceAdminPort {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_PIN_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 5;

    private final SpringDataInvoiceJpaRepository invoices;
    private final SpringDataInvoiceLineJpaRepository lines;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final ResilienceProperties resilienceProperties;
    private final SimulatedErpFailureToggle failureToggle;

    public LocalInvoiceAdapter(
            SpringDataInvoiceJpaRepository invoices,
            SpringDataInvoiceLineJpaRepository lines,
            CircuitBreaker erpGatewayCircuitBreaker,
            Retry erpGatewayRetry,
            ResilienceProperties resilienceProperties,
            SimulatedErpFailureToggle failureToggle
    ) {
        this.invoices = invoices;
        this.lines = lines;
        this.circuitBreaker = erpGatewayCircuitBreaker;
        this.retry = erpGatewayRetry;
        this.resilienceProperties = resilienceProperties;
        this.failureToggle = failureToggle;
    }

    /**
     * Envuelve cada escritura con el Circuit Breaker "erpGateway" (activado por defecto,
     * app.resilience.circuit-breaker-enabled). Si el breaker esta abierto, rechaza sin
     * tocar la base de datos. Sin Retry: reintentar una escritura que ya sostiene un
     * bloqueo pesimista de fila (findByIdForUpdate) prolongaria ese lock sin necesidad.
     */
    private <T> T protectedCall(Supplier<T> action) {
        // El chequeo de fallo simulado va DENTRO del supplier: asi el Circuit Breaker (y el
        // Retry, en las lecturas) lo ven como una llamada fallida real, en vez de que la
        // excepcion escape sin que ninguno de los dos se entere.
        Supplier<T> guarded = () -> {
            failureToggle.maybeFail();
            return action.get();
        };
        if (!resilienceProperties.isCircuitBreakerEnabled()) {
            return guarded.get();
        }
        try {
            return circuitBreaker.executeSupplier(guarded);
        } catch (CallNotPermittedException ex) {
            throw new ErpUnavailableException();
        }
    }

    /**
     * Igual que protectedCall(), pero ademas reintenta (Retry "erpGatewayRetry") ante un
     * fallo transitorio simulado: solo tiene sentido en lecturas idempotentes, nunca en
     * las escrituras (ver protectedCall()).
     */
    private <T> T protectedRead(Supplier<T> action) {
        if (!resilienceProperties.isRetryEnabled()) {
            return protectedCall(action);
        }
        Supplier<T> withRetry = Retry.decorateSupplier(retry, () -> protectedCall(action));
        return withRetry.get();
    }

    @Override
    public List<Invoice> searchPendingDeliveryInvoices(String query) {
        String escaped = SpringDataInvoiceJpaRepository.escapeLike(query);
        return protectedRead(() -> invoices.searchPendingDeliveryInvoices(escaped, PageRequest.of(0, 20))
                .stream().map(InvoicePersistenceMapper::toInvoice).toList());
    }

    @Override
    // N7 (docs/EVALUACION_TECNICA.md §18): "unless" evita cachear un resultado vacio -- sin
    // esto, consultar un invoiceId antes de que tenga lineas (p. ej. justo antes de que
    // termine de crearse) dejaba esa lista vacia en cache hasta que expirara el TTL, aunque
    // las lineas ya existieran en la base para cuando se volviera a consultar.
    @Cacheable(cacheNames = "invoiceLines", key = "#invoiceId", unless = "#result.isEmpty()")
    public List<InvoiceLine> findInvoiceLines(Long invoiceId) {
        return protectedRead(() -> lines.findByInvoice_IdOrderById(invoiceId)
                .stream().map(InvoicePersistenceMapper::toLine).toList());
    }

    @Override
    // "unless" no puede escribirse "#result.isEmpty()" aqui: Spring desenvuelve el Optional
    // antes de evaluar la SpEL, asi que #result YA es el GeoLocation (o null si estaba
    // vacio) -- no el Optional. Con "#result.isEmpty()" cualquier llamada con ubicacion
    // presente rompia en runtime (SpelEvaluationException: "isEmpty() cannot be found on
    // type GeoLocation"), y JwtAuthenticationFilter la reportaba como si fuera un 401,
    // ocultando el error real. Se detecto en vivo con verify_delivery.py, no con las
    // pruebas (los mocks de los tests no pasan por este proxy de cache real).
    @Cacheable(cacheNames = "expectedLocation", key = "#invoiceId", unless = "#result == null")
    public Optional<GeoLocation> findExpectedLocation(Long invoiceId) {
        return protectedRead(() -> invoices.findById(invoiceId)
                .filter(i -> i.getExpectedLatitude() != null && i.getExpectedLongitude() != null)
                .map(i -> new GeoLocation(i.getExpectedLatitude(), i.getExpectedLongitude())));
    }

    @Override
    public Optional<Invoice> findById(Long invoiceId) {
        return protectedRead(() -> invoices.findById(invoiceId).map(InvoicePersistenceMapper::toInvoice));
    }

    private InvoiceJpaEntity locked(Long id) {
        return invoices.findByIdForUpdate(id).orElseThrow(() -> new DeliveryRejectedException("La factura no existe."));
    }

    @Override
    @Transactional
    public void confirmDelivery(Long invoiceId, String pin, GeoLocation location, String driverName) {
        protectedCall(() -> {
            InvoiceJpaEntity invoice = locked(invoiceId);
            if (!invoice.isRequiresPin()) throw new DeliveryRejectedException("Esta factura no requiere PIN de entrega.");
            if (!"posted".equals(invoice.getState())) throw new DeliveryRejectedException("La factura debe estar publicada para confirmar la entrega.");
            if (invoice.isConfirmed()) throw new DeliveryAlreadyConfirmedException();
            Instant lockedUntil = invoice.getPinLockedUntil();
            if (lockedUntil != null && lockedUntil.isAfter(Instant.now())) {
                throw new DeliveryRejectedException("Factura bloqueada temporalmente por intentos de PIN fallidos. Intenta nuevamente mas tarde.");
            }
            // OJO: no se actualiza aqui el contador de intentos. Esta transaccion se revierte
            // por completo si se lanza InvalidPinException (asi debe ser: la entrega NO se
            // confirmo), y esa reversion se lleva con ella cualquier cambio hecho aqui mismo.
            // Por eso registerFailedPinAttempt() es un metodo/puerto separado, que el llamador
            // (DeliveryApplicationService) invoca DESPUES de que esta transaccion ya termino.
            if (pin == null || !pin.equals(invoice.getPin())) {
                throw new InvalidPinException();
            }
            invoice.setConfirmed(true);
            invoice.setConfirmedAt(Instant.now());
            invoice.setLatitude(location.latitude());
            invoice.setLongitude(location.longitude());
            invoice.setDriverName(driverName);
            invoice.setFailedPinAttempts(0);
            invoice.setPinLockedUntil(null);
            return null;
        });
    }

    /**
     * PIN sin limite de intentos (Fase1 §5.1, riesgo "Intentos de adivinacion del PIN").
     * No se hashea el PIN: Fase1 §3 exige que la administracion pueda leerlo para
     * comunicarlo manualmente al cliente; en su lugar se bloquea la factura tras
     * MAX_PIN_ATTEMPTS fallos consecutivos. Transaccion propia e independiente de
     * confirmDelivery (ver comentario ahi).
     */
    @Override
    @Transactional
    public void registerFailedPinAttempt(Long invoiceId) {
        protectedCall(() -> {
            InvoiceJpaEntity invoice = locked(invoiceId);
            int attempts = invoice.getFailedPinAttempts() + 1;
            invoice.setFailedPinAttempts(attempts);
            if (attempts >= MAX_PIN_ATTEMPTS) {
                invoice.setPinLockedUntil(Instant.now().plusSeconds(LOCK_MINUTES * 60));
            }
            return null;
        });
    }

    @Override
    public List<AdminInvoiceView> list(String query) {
        String escaped = SpringDataInvoiceJpaRepository.escapeLike(query);
        return protectedRead(() -> invoices.search(escaped, PageRequest.of(0, 1000))
                .stream().map(InvoicePersistenceMapper::toAdminView).toList());
    }

    @Override
    @Transactional
    public AdminInvoiceView publish(Long id) {
        return protectedCall(() -> {
            InvoiceJpaEntity invoice = locked(id);
            if ("cancel".equals(invoice.getState())) throw new DeliveryRejectedException("No se puede publicar una factura cancelada.");
            String pin = invoice.getPin();
            if (invoice.isRequiresPin() && (pin == null || pin.isBlank())) {
                pin = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
            }
            invoice.setState("posted");
            invoice.setPin(pin);
            return InvoicePersistenceMapper.toAdminView(invoice);
        });
    }

    @Override
    @Transactional
    public AdminInvoiceView create(String number, String partnerName, String address, Double latitude, Double longitude,
                                      boolean requiresPin, List<InvoiceLine> requestedLines) {
        return protectedCall(() -> {
            InvoiceJpaEntity invoice = new InvoiceJpaEntity(null, number, partnerName, address, latitude, longitude,
                    LocalDate.now(), "draft", requiresPin, null, false, null, null, null, null, 0, null);
            invoice = invoices.save(invoice);
            for (InvoiceLine line : requestedLines) {
                lines.save(new InvoiceLineJpaEntity(null, invoice, line.description(), line.quantity()));
            }
            return InvoicePersistenceMapper.toAdminView(invoice);
        });
    }
}
