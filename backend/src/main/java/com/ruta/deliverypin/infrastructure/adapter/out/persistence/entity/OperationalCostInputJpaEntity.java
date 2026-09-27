package com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "operational_cost_input")
public class OperationalCostInputJpaEntity {

    @Id
    private LocalDate month;

    @Column(name = "support_hours", nullable = false)
    private double supportHours;

    protected OperationalCostInputJpaEntity() {
        // requerido por JPA
    }

    public OperationalCostInputJpaEntity(LocalDate month, double supportHours) {
        this.month = month;
        this.supportHours = supportHours;
    }

    public LocalDate getMonth() {
        return month;
    }

    public double getSupportHours() {
        return supportHours;
    }

    public void setSupportHours(double supportHours) {
        this.supportHours = supportHours;
    }
}
