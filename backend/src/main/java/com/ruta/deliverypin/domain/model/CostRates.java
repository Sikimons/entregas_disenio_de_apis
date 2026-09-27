package com.ruta.deliverypin.domain.model;

/**
 * Tarifas usadas para el showback de costo por entrega (Fase1 §4.5). Se configuran por
 * variable de entorno porque cambian poco (tarifa de nube, de almacenamiento y de soporte);
 * las horas de soporte del mes, en cambio, varian y se guardan como dato editable (OperationalCostInputPort).
 */
public record CostRates(double infrastructureMonthlyUsd, double storagePerGbUsd, double supportHourUsd) {
}
