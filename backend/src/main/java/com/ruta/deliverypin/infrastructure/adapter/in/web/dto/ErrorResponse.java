package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * Forma unica de error de toda la API (auditoria tecnica, Tanda 2): antes cada
 * {@code @ExceptionHandler} de GlobalExceptionHandler devolvia un {@code Map<String,String>}
 * ad-hoc, sin tipo ni schema real en OpenAPI ("essentially untyped" en el contrato
 * publicado). "errors" y "path" se omiten cuando no aplican (validacion de campos, o
 * cualquier error sin contexto de ruta) para no ensuciar la respuesta con nulls.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Forma unica de error de la API. Todas las respuestas 4xx/5xx usan este contrato.")
public record ErrorResponse(
        @Schema(description = "Mensaje legible para mostrar al usuario.", example = "El PIN ingresado no es correcto.")
        String message,
        @Schema(description = "Detalle por campo, solo presente en errores de validacion (400 de Bean Validation).",
                example = "{\"pin\": \"El PIN debe tener 6 digitos\"}")
        Map<String, String> errors,
        @Schema(description = "Ruta donde ocurrio el error.", example = "/api/v1/driver/deliveries/confirm")
        String path,
        @Schema(description = "Instante del error, en UTC.")
        Instant timestamp
) {

    public static ErrorResponse of(String message, String path) {
        return new ErrorResponse(message, null, path, Instant.now());
    }

    public static ErrorResponse of(String message, Map<String, String> errors, String path) {
        return new ErrorResponse(message, errors, path, Instant.now());
    }
}
