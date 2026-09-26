package com.ruta.deliverypin.domain.model;

import java.util.List;

/**
 * Resultado paginado agnostico de framework.
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}
