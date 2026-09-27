package com.ruta.deliverypin.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tarifas del showback de costo por entrega (Fase1 §4.5). Se configuran por variable de
 * entorno porque cambian poco (tarifa del proveedor de nube, de almacenamiento y de la hora
 * de soporte), a diferencia de las horas de soporte del mes, que son un dato operativo y se
 * guardan editable en la base (ver OperationalCostInputPort).
 */
@ConfigurationProperties(prefix = "app.cost")
public class CostProperties {

    private double infraMonthlyUsd;
    private double storagePerGbUsd;
    private double supportHourUsd;

    public double getInfraMonthlyUsd() {
        return infraMonthlyUsd;
    }

    public void setInfraMonthlyUsd(double infraMonthlyUsd) {
        this.infraMonthlyUsd = infraMonthlyUsd;
    }

    public double getStoragePerGbUsd() {
        return storagePerGbUsd;
    }

    public void setStoragePerGbUsd(double storagePerGbUsd) {
        this.storagePerGbUsd = storagePerGbUsd;
    }

    public double getSupportHourUsd() {
        return supportHourUsd;
    }

    public void setSupportHourUsd(double supportHourUsd) {
        this.supportHourUsd = supportHourUsd;
    }
}
