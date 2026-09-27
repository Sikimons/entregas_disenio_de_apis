package com.ruta.deliverypin.infrastructure.adapter.in.web;

import com.ruta.deliverypin.domain.exception.DeliveryAlreadyConfirmedException;
import com.ruta.deliverypin.domain.exception.DeliveryRejectedException;
import com.ruta.deliverypin.domain.exception.DriverAlreadyExistsException;
import com.ruta.deliverypin.domain.exception.DriverNotFoundException;
import com.ruta.deliverypin.domain.exception.ErpUnavailableException;
import com.ruta.deliverypin.domain.exception.InvalidCredentialsException;
import com.ruta.deliverypin.domain.exception.InvalidPinException;
import com.ruta.deliverypin.domain.exception.SelfAccountModificationException;
import com.ruta.deliverypin.domain.exception.TooManyLoginAttemptsException;
import com.ruta.deliverypin.infrastructure.adapter.out.invoice.SimulatedErpFailureToggle;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de prueba usado solo por GlobalExceptionHandlerTest: cada endpoint lanza
 * una excepcion de dominio (o tecnica) distinta, para verificar que el manejador global
 * la traduce al codigo HTTP y formato correctos.
 */
@RestController
public class ThrowingTestController {

    @GetMapping("/test/throw/invalid-credentials")
    public void invalidCredentials() {
        throw new InvalidCredentialsException();
    }

    @GetMapping("/test/throw/too-many-login-attempts")
    public void tooManyLoginAttempts() {
        throw new TooManyLoginAttemptsException();
    }

    @GetMapping("/test/throw/already-confirmed")
    public void alreadyConfirmed() {
        throw new DeliveryAlreadyConfirmedException();
    }

    @GetMapping("/test/throw/invalid-pin")
    public void invalidPin() {
        throw new InvalidPinException();
    }

    @GetMapping("/test/throw/delivery-rejected")
    public void deliveryRejected() {
        throw new DeliveryRejectedException("La factura no existe.");
    }

    @GetMapping("/test/throw/integrity-violation")
    public void integrityViolation() {
        throw new DataIntegrityViolationException("fk violation");
    }

    @GetMapping("/test/throw/duplicate-key")
    public void duplicateKey() {
        throw new DuplicateKeyException("duplicate");
    }

    @GetMapping("/test/throw/data-access")
    public void dataAccess() {
        throw new DataAccessResourceFailureException("db down");
    }

    @GetMapping("/test/throw/erp-unavailable")
    public void erpUnavailable() {
        throw new ErpUnavailableException();
    }

    @GetMapping("/test/throw/circuit-open")
    public void circuitOpen() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("test");
        throw CallNotPermittedException.createCallNotPermittedException(circuitBreaker);
    }

    @GetMapping("/test/throw/simulated-failure")
    public void simulatedFailure() {
        throw new SimulatedErpFailureToggle.SimulatedErpFailureException();
    }

    @GetMapping("/test/throw/driver-exists")
    public void driverExists() {
        throw new DriverAlreadyExistsException("conductor1");
    }

    @GetMapping("/test/throw/self-modification")
    public void selfModification() {
        throw new SelfAccountModificationException();
    }

    @GetMapping("/test/throw/driver-not-found/{id}")
    public void driverNotFound(@PathVariable Long id) {
        throw new DriverNotFoundException(id);
    }

    @GetMapping("/test/throw/illegal-argument")
    public void illegalArgument() {
        throw new IllegalArgumentException("argumento invalido");
    }

    @PostMapping("/test/throw/validate")
    public void validate(@Valid @RequestBody ValidatedBody body) {
    }

    public record ValidatedBody(@NotBlank String name) {
    }
}
