package com.ruta.deliverypin.domain.model;

/**
 * Peticion de paginacion agnostica de framework (sustituye a org.springframework.data.domain.Pageable
 * en el dominio, para no acoplar el core de la aplicacion a Spring Data).
 */
public record PageRequest(int page, int size) {

    public static final int MAX_SIZE = 100;

    public PageRequest {
        if (page < 0) {
            throw new IllegalArgumentException("La pagina no puede ser negativa.");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("El tamano de pagina debe ser mayor a cero.");
        }
        if (size > MAX_SIZE) {
            throw new IllegalArgumentException("El tamano de pagina no puede superar " + MAX_SIZE + ".");
        }
    }
}
