package com.ruta.deliverypin.domain.model;

import java.time.Instant;

/**
 * Registro (auditoria) de un intento de entrega, sea que haya terminado en
 * confirmacion, rechazo (p.ej. PIN incorrecto) o incidencia reportada por el conductor.
 */
public class DeliveryAttempt {

    private final Long id;
    private final Long invoiceId;
    private final String invoiceNumber;
    private final String partnerName;
    private final String deliveryAddress;
    private final Driver driver;
    private final DeliveryAttemptOutcome outcome;
    private final GeoLocation location;
    private final String detail;
    private final StoredPhoto photo;
    private final Double distanceFromExpectedMeters;
    private final Instant createdAt;

    private DeliveryAttempt(Long id, Long invoiceId, String invoiceNumber, String partnerName, String deliveryAddress,
                             Driver driver, DeliveryAttemptOutcome outcome, GeoLocation location, String detail,
                             StoredPhoto photo, Double distanceFromExpectedMeters, Instant createdAt) {
        this.id = id;
        this.invoiceId = invoiceId;
        this.invoiceNumber = invoiceNumber;
        this.partnerName = partnerName;
        this.deliveryAddress = deliveryAddress;
        this.driver = driver;
        this.outcome = outcome;
        this.location = location;
        this.detail = detail;
        this.photo = photo;
        this.distanceFromExpectedMeters = distanceFromExpectedMeters;
        this.createdAt = createdAt;
    }

    public static DeliveryAttempt succeeded(Long invoiceId, String invoiceNumber, String partnerName, String deliveryAddress,
                                             Driver driver, GeoLocation location, StoredPhoto photo, Double distanceFromExpectedMeters) {
        return new DeliveryAttempt(null, invoiceId, invoiceNumber, partnerName, deliveryAddress, driver,
                DeliveryAttemptOutcome.CONFIRMED, location, null, photo, distanceFromExpectedMeters, Instant.now());
    }

    public static DeliveryAttempt failed(Long invoiceId, String invoiceNumber, String partnerName, String deliveryAddress,
                                          Driver driver, GeoLocation location, String errorMessage) {
        return new DeliveryAttempt(null, invoiceId, invoiceNumber, partnerName, deliveryAddress, driver,
                DeliveryAttemptOutcome.REJECTED, location, truncate(errorMessage), null, null, Instant.now());
    }

    public static DeliveryAttempt incident(Long invoiceId, String invoiceNumber, String partnerName, String deliveryAddress,
                                            Driver driver, GeoLocation location, String reason, String notes) {
        String detail = (notes == null || notes.isBlank()) ? reason : reason + ": " + notes;
        return new DeliveryAttempt(null, invoiceId, invoiceNumber, partnerName, deliveryAddress, driver,
                DeliveryAttemptOutcome.INCIDENT, location, truncate(detail), null, null, Instant.now());
    }

    public static DeliveryAttempt reconstitute(Long id, Long invoiceId, String invoiceNumber, String partnerName, String deliveryAddress,
                                                Driver driver, DeliveryAttemptOutcome outcome, GeoLocation location, String detail,
                                                StoredPhoto photo, Double distanceFromExpectedMeters, Instant createdAt) {
        return new DeliveryAttempt(id, invoiceId, invoiceNumber, partnerName, deliveryAddress, driver, outcome,
                location, detail, photo, distanceFromExpectedMeters, createdAt);
    }

    private static String truncate(String message) {
        return message != null && message.length() > 255 ? message.substring(0, 255) : message;
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

    public Driver getDriver() {
        return driver;
    }

    public DeliveryAttemptOutcome getOutcome() {
        return outcome;
    }

    public GeoLocation getLocation() {
        return location;
    }

    public String getDetail() {
        return detail;
    }

    public StoredPhoto getPhoto() {
        return photo;
    }

    public Double getDistanceFromExpectedMeters() {
        return distanceFromExpectedMeters;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
