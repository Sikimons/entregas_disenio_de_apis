package com.ruta.deliverypin.domain.model;

/**
 * Volumen de entregas confirmadas y bytes de evidencia fotografica en un rango de fechas,
 * calculado en la base de datos (sin cargar el contenido de las fotos a memoria).
 * Insumo para el calculo de costo por entrega verificada (showback, Fase1 §4.5).
 */
public record DeliveryVolumeSummary(long confirmedDeliveries, long evidencePhotoBytes) {
}
