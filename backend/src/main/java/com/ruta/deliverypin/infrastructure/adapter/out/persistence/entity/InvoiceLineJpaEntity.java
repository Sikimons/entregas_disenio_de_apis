package com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "delivery_invoice_line")
public class InvoiceLineJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceJpaEntity invoice;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private double quantity;

    protected InvoiceLineJpaEntity() {
        // requerido por JPA
    }

    public InvoiceLineJpaEntity(Long id, InvoiceJpaEntity invoice, String description, double quantity) {
        this.id = id;
        this.invoice = invoice;
        this.description = description;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public InvoiceJpaEntity getInvoice() {
        return invoice;
    }

    public String getDescription() {
        return description;
    }

    public double getQuantity() {
        return quantity;
    }
}
