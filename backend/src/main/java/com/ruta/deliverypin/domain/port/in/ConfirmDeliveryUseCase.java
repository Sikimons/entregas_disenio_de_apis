package com.ruta.deliverypin.domain.port.in;

import com.ruta.deliverypin.domain.model.DeliveryPhoto;
import com.ruta.deliverypin.domain.model.Driver;

public interface ConfirmDeliveryUseCase {

    /** Datos y operaciones de entregas. */
    boolean confirmDelivery(ConfirmDeliveryCommand command, Driver driver);

    record ConfirmDeliveryCommand(
            Long invoiceId, String invoiceNumber, String partnerName, String deliveryAddress,
            String pin, double latitude, double longitude, DeliveryPhoto photo
    ) {
    }
}
