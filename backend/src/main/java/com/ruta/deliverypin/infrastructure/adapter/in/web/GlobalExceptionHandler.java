package com.ruta.deliverypin.infrastructure.adapter.in.web;

import com.ruta.deliverypin.domain.exception.DeliveryAlreadyConfirmedException;
import com.ruta.deliverypin.domain.exception.DeliveryRejectedException;
import com.ruta.deliverypin.domain.exception.DriverAlreadyExistsException;
import com.ruta.deliverypin.domain.exception.DriverNotFoundException;
import com.ruta.deliverypin.domain.exception.ErpUnavailableException;
import com.ruta.deliverypin.domain.exception.InvalidCredentialsException;
import com.ruta.deliverypin.domain.exception.InvalidPinException;
import com.ruta.deliverypin.domain.exception.SelfAccountModificationException;
import com.ruta.deliverypin.domain.exception.TooManyConfirmAttemptsException;
import com.ruta.deliverypin.domain.exception.TooManyLoginAttemptsException;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.ErrorResponse;
import com.ruta.deliverypin.infrastructure.adapter.out.invoice.SimulatedErpFailureToggle;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
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
 *
 * Todos los handlers devuelven {@link ErrorResponse} (Tanda 2 de la auditoria tecnica):
 * antes cada uno construia su propio {@code Map<String, String>} ad-hoc, sin tipo ni
 * schema real en el contrato de OpenAPI -- Swagger UI documentaba el codigo HTTP pero no
 * la forma real del cuerpo del error.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static String path(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.of(ex.getMessage(), path(request)));
    }

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyLoginAttempts(TooManyLoginAttemptsException ex, HttpServletRequest request) {
        log.warn("Limite de intentos de login excedido");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ErrorResponse.of(ex.getMessage(), path(request)));
    }

    @ExceptionHandler(TooManyConfirmAttemptsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyConfirmAttempts(TooManyConfirmAttemptsException ex, HttpServletRequest request) {
        log.warn("Limite de intentos de confirmacion excedido");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ErrorResponse.of(ex.getMessage(), path(request)));
    }

    @ExceptionHandler(DeliveryAlreadyConfirmedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyConfirmed(DeliveryAlreadyConfirmedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(ex.getMessage(), path(request)));
    }

    @ExceptionHandler(InvalidPinException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPin(InvalidPinException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ErrorResponse.of(ex.getMessage(), path(request)));
    }

    @ExceptionHandler(DeliveryRejectedException.class)
    public ResponseEntity<ErrorResponse> handleDeliveryRejected(DeliveryRejectedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(ex.getMessage(), path(request)));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(
                "La operacion no se pudo completar porque el registro esta referenciado por otros datos.", path(request)));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleStorage(DataAccessException ex, HttpServletRequest request) {
        log.error("Fallo de acceso a datos no clasificado", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErrorResponse.of(
                "No se pudo guardar o consultar la informacion. Intente nuevamente.", path(request)));
    }

    @ExceptionHandler(ErpUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleErpUnavailable(ErpUnavailableException ex, HttpServletRequest request) {
        log.error("ERP simulado no disponible: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErrorResponse.of(ex.getMessage(), path(request)));
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCircuitBreakerOpen(CallNotPermittedException ex, HttpServletRequest request) {
        log.error("Circuit breaker abierto: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErrorResponse.of(
                "Servicio de facturacion temporalmente no disponible. Intenta nuevamente en unos segundos.", path(request)));
    }

    @ExceptionHandler(SimulatedErpFailureToggle.SimulatedErpFailureException.class)
    public ResponseEntity<ErrorResponse> handleSimulatedFailure(SimulatedErpFailureToggle.SimulatedErpFailureException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErrorResponse.of(ex.getMessage(), path(request)));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateKeyException ex, HttpServletRequest request) {
        // Mensaje generico a proposito: este handler no sabe de que tabla vino la
        // violacion de unicidad sin inspeccionar la excepcion envuelta del driver JDBC.
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of("Ya existe un registro con ese valor unico.", path(request)));
    }

    @ExceptionHandler(DriverAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleDriverAlreadyExists(DriverAlreadyExistsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(ex.getMessage(), path(request)));
    }

    @ExceptionHandler(SelfAccountModificationException.class)
    public ResponseEntity<ErrorResponse> handleSelfAccountModification(SelfAccountModificationException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(ex.getMessage(), path(request)));
    }

    @ExceptionHandler(DriverNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDriverNotFound(DriverNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(ex.getMessage(), path(request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(ex.getMessage(), path(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        // "message" siempre presente (el frontend solo lee err.response.data.message);
        // "errors" se conserva para quien quiera mostrar el detalle por campo.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of("Uno o mas campos no son validos.", errors, path(request)));
    }

    /**
     * Red de seguridad final (auditoria tecnica, hallazgo P2): sin este handler, cualquier
     * excepcion no mapeada arriba (un bug real, un NullPointerException, etc.) caia en el
     * manejador por defecto de Spring Boot, que no garantiza el mismo formato que el resto
     * de la API y podria variar segun la configuracion de "server.error.*". Debe ser el
     * ULTIMO @ExceptionHandler de la clase: Spring elige el handler mas especifico
     * disponible, asi que uno mas concreto arriba siempre gana sobre este catch-all, que
     * solo actua cuando ningun otro aplica.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Excepcion no controlada", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.of("Error interno. Intenta nuevamente.", path(request)));
    }
}
