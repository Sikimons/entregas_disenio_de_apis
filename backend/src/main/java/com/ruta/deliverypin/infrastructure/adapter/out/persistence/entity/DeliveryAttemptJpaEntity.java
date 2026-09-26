package com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity;

import com.ruta.deliverypin.domain.model.DeliveryAttemptOutcome;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "delivery_log")
public class DeliveryAttemptJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long invoiceId;

    @Column(nullable = false, length = 60)
    private String invoiceNumber;

    @Column(length = 255)
    private String partnerName;

    @Column(length = 255)
    private String deliveryAddress;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private DriverJpaEntity driver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryAttemptOutcome outcome;

    private Double latitude;

    private Double longitude;

    @Column(length = 255)
    private String detail;

    @Column(columnDefinition = "bytea")
    private byte[] photo;

    @Column(length = 100)
    private String photoContentType;

    private Double distanceFromExpectedMeters;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected DeliveryAttemptJpaEntity() {
        // requerido por JPA
    }

    public DeliveryAttemptJpaEntity(Long id, Long invoiceId, String invoiceNumber, String partnerName, String deliveryAddress,
                                     DriverJpaEntity driver, DeliveryAttemptOutcome outcome, Double latitude, Double longitude, String detail,
                                     byte[] photo, String photoContentType, Double distanceFromExpectedMeters, Instant createdAt) {
        this.id = id;
        this.invoiceId = invoiceId;
        this.invoiceNumber = invoiceNumber;
        this.partnerName = partnerName;
        this.deliveryAddress = deliveryAddress;
        this.driver = driver;
        this.outcome = outcome;
        this.latitude = latitude;
        this.longitude = longitude;
        this.detail = detail;
        this.photo = photo;
        this.photoContentType = photoContentType;
        this.distanceFromExpectedMeters = distanceFromExpectedMeters;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public DriverJpaEntity getDriver() {
        return driver;
    }

    public DeliveryAttemptOutcome getOutcome() {
        return outcome;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getDetail() {
        return detail;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public Double getDistanceFromExpectedMeters() {
        return distanceFromExpectedMeters;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
