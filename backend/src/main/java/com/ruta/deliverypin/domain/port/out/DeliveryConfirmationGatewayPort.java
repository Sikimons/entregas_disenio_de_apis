package com.ruta.deliverypin.domain.port.out;
import com.ruta.deliverypin.domain.model.GeoLocation;
/** Valida el PIN y registra una entrega en la base propia. */
public interface DeliveryConfirmationGatewayPort {
    void confirmDelivery(Long invoiceId, String pin, GeoLocation location, String driverName);
}
