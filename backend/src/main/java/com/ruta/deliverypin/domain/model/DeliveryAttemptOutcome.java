package com.ruta.deliverypin.domain.model;

/**
 * Resultado de un intento de entrega registrado en el log de auditoria.
 */
public enum DeliveryAttemptOutcome {
    /** Datos y operaciones de entregas. */
    CONFIRMED,
    /** Datos y operaciones de entregas. */
    REJECTED,
    /** El conductor no pudo entregar y reporto una incidencia (sin intentar PIN). */
    INCIDENT
}
