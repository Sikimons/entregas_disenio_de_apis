package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

import com.ruta.deliverypin.domain.model.DeliveryAttempt;

import java.time.Instant;

public record DeliveryAttemptResponse(
        Long id,
        Long invoiceId,
        String invoiceNumber,
        String partnerName,
        String deliveryAddress,
        String driverName,
        String outcome,
        Double latitude,
        Double longitude,
        String detail,
        boolean hasPhoto,
        Double distanceFromExpectedMeters,
        Instant createdAt
) {
    public static DeliveryAttemptResponse from(DeliveryAttempt attempt) {
        return new DeliveryAttemptResponse(
                attempt.getId(),
                attempt.getInvoiceId(),
                attempt.getInvoiceNumber(),
                attempt.getPartnerName(),
                attempt.getDeliveryAddress(),
                attempt.getDriver().getFullName(),
                attempt.getOutcome().name(),
                attempt.getLocation() != null ? attempt.getLocation().latitude() : null,
                attempt.getLocation() != null ? attempt.getLocation().longitude() : null,
                attempt.getDetail(),
                attempt.getPhoto() != null,
                attempt.getDistanceFromExpectedMeters(),
                attempt.getCreatedAt()
        );
    }
}
