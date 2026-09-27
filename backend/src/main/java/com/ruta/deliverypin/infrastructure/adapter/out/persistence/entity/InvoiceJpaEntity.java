package com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Entidad JPA de "delivery_invoice" (Fase8): reemplaza el acceso por JdbcTemplate/SQL
 * directo de LocalInvoiceAdapter, que era la ultima tabla de negocio sin mapear con JPA
 * (RA4, "dos estrategias de persistencia conviviendo en el mismo backend"). El bloqueo
 * pesimista para confirmDelivery/publish/registerFailedPinAttempt ya no se hace con
 * "SELECT ... FOR UPDATE" en SQL, sino con @Lock(PESSIMISTIC_WRITE) en el repositorio
 * (ver SpringDataInvoiceJpaRepository.findByIdForUpdate), que Hibernate traduce al mismo
 * "FOR UPDATE" sobre la misma fila.
 */
@Entity
@Table(name = "delivery_invoice")
public class InvoiceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String number;

    @Column(name = "partner_name", nullable = false, length = 255)
    private String partnerName;

    @Column(name = "delivery_address", length = 255)
    private String deliveryAddress;

    @Column(name = "expected_latitude")
    private Double expectedLatitude;

    @Column(name = "expected_longitude")
    private Double expectedLongitude;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(nullable = false, length = 20)
    private String state;

    @Column(name = "requires_pin", nullable = false)
    private boolean requiresPin;

    @Column(length = 6)
    private String pin;

    @Column(nullable = false)
    private boolean confirmed;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    private Double latitude;

    private Double longitude;

    @Column(name = "driver_name", length = 255)
    private String driverName;

    @Column(name = "failed_pin_attempts", nullable = false)
    private int failedPinAttempts;

    @Column(name = "pin_locked_until")
    private Instant pinLockedUntil;

    // Auditoria (Tanda 2, Fase2 §6): fuera del constructor a proposito, para no agregar un
    // 18vo/19vo parametro posicional a uno que ya tiene 17 y que varios tests construyen
    // directamente. Se completan con setters, igual que "confirmed"/"pin" en publish()/
    // confirmDelivery() de LocalInvoiceAdapter -- nunca viajan en el alta inicial de la fila.
    @Column(name = "created_by", length = 80)
    private String createdBy;

    @Column(name = "published_by", length = 80)
    private String publishedBy;

    protected InvoiceJpaEntity() {
        // requerido por JPA
    }

    public InvoiceJpaEntity(Long id, String number, String partnerName, String deliveryAddress,
                             Double expectedLatitude, Double expectedLongitude, LocalDate invoiceDate, String state,
                             boolean requiresPin, String pin, boolean confirmed, Instant confirmedAt,
                             Double latitude, Double longitude, String driverName, int failedPinAttempts, Instant pinLockedUntil) {
        this.id = id;
        this.number = number;
        this.partnerName = partnerName;
        this.deliveryAddress = deliveryAddress;
        this.expectedLatitude = expectedLatitude;
        this.expectedLongitude = expectedLongitude;
        this.invoiceDate = invoiceDate;
        this.state = state;
        this.requiresPin = requiresPin;
        this.pin = pin;
        this.confirmed = confirmed;
        this.confirmedAt = confirmedAt;
        this.latitude = latitude;
        this.longitude = longitude;
        this.driverName = driverName;
        this.failedPinAttempts = failedPinAttempts;
        this.pinLockedUntil = pinLockedUntil;
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public Double getExpectedLatitude() {
        return expectedLatitude;
    }

    public Double getExpectedLongitude() {
        return expectedLongitude;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isRequiresPin() {
        return requiresPin;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public int getFailedPinAttempts() {
        return failedPinAttempts;
    }

    public void setFailedPinAttempts(int failedPinAttempts) {
        this.failedPinAttempts = failedPinAttempts;
    }

    public Instant getPinLockedUntil() {
        return pinLockedUntil;
    }

    public void setPinLockedUntil(Instant pinLockedUntil) {
        this.pinLockedUntil = pinLockedUntil;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(String publishedBy) {
        this.publishedBy = publishedBy;
    }
}
