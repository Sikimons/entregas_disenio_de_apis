package com.ruta.deliverypin.domain.exception;

/**
 * Un administrador intento desactivar o eliminar su propia cuenta. Se mapea a 409:
 * evita quedarse sin acceso administrativo por error (no depende de contar cuantos
 * administradores activos quedan, solo de que no sea la propia cuenta autenticada).
 */
public class SelfAccountModificationException extends RuntimeException {
    public SelfAccountModificationException() {
        super("No puedes desactivar o eliminar tu propia cuenta.");
    }
}
