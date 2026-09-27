package com.ruta.deliverypin.domain.port.out;
import com.ruta.deliverypin.domain.model.GeoLocation;
/** Valida el PIN y registra una entrega en la base propia. */
public interface DeliveryConfirmationGatewayPort {
    void confirmDelivery(Long invoiceId, String pin, GeoLocation location, String driverName);

    /**
     * Registra un intento de PIN fallido y bloquea la factura tras varios fallos
     * consecutivos (Fase1 §5.1). Se llama en una transaccion propia, DESPUES de que
     * la transaccion de confirmDelivery ya revirtio: si se hiciera dentro de esa misma
     * transaccion, el rollback por InvalidPinException tambien revertiria este contador.
     */
    void registerFailedPinAttempt(Long invoiceId);
}
