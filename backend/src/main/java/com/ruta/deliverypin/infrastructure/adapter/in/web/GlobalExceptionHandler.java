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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones de dominio a respuestas HTTP. Es el unico lugar de la
 * aplicacion que conoce el mapeo entre reglas de negocio y codigos de estado.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    public ResponseEntity<Map<String, String>> handleTooManyLoginAttempts(TooManyLoginAttemptsException ex) {
        log.warn("Limite de intentos de login excedido");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(DeliveryAlreadyConfirmedException.class)
    public ResponseEntity<Map<String, String>> handleAlreadyConfirmed(DeliveryAlreadyConfirmedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidPinException.class)
    public ResponseEntity<Map<String, String>> handleInvalidPin(InvalidPinException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(DeliveryRejectedException.class)
    public ResponseEntity<Map<String, String>> handleDeliveryRejected(DeliveryRejectedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "La operacion no se pudo completar porque el registro esta referenciado por otros datos."));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleStorage(DataAccessException ex) {
        log.error("Fallo de acceso a datos no clasificado", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "No se pudo guardar o consultar la informacion. Intente nuevamente."));
    }

    @ExceptionHandler(ErpUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleErpUnavailable(ErpUnavailableException ex) {
        log.error("ERP simulado no disponible: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<Map<String, String>> handleCircuitBreakerOpen(CallNotPermittedException ex) {
        log.error("Circuit breaker abierto: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Servicio de facturacion temporalmente no disponible. Intenta nuevamente en unos segundos."));
    }

    @ExceptionHandler(SimulatedErpFailureToggle.SimulatedErpFailureException.class)
    public ResponseEntity<Map<String, String>> handleSimulatedFailure(SimulatedErpFailureToggle.SimulatedErpFailureException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DuplicateKeyException ex) {
        // Mensaje generico a proposito: este handler no sabe de que tabla vino la
        // violacion de unicidad sin inspeccionar la excepcion envuelta del driver JDBC.
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Ya existe un registro con ese valor unico."));
    }

    @ExceptionHandler(DriverAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleDriverAlreadyExists(DriverAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(SelfAccountModificationException.class)
    public ResponseEntity<Map<String, String>> handleSelfAccountModification(SelfAccountModificationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(DriverNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleDriverNotFound(DriverNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        // "message" siempre presente (el frontend solo lee err.response.data.message);
        // "errors" se conserva para quien quiera mostrar el detalle por campo.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Uno o mas campos no son validos.");
        body.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
