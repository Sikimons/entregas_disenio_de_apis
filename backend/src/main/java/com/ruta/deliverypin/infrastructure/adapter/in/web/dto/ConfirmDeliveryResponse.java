package com.ruta.deliverypin.infrastructure.adapter.in.web.dto;

public record ConfirmDeliveryResponse(boolean success, String message, boolean photoUploaded) {
}
