package com.ruta.deliverypin.domain.port.in;

import com.ruta.deliverypin.domain.model.Driver;

public interface ReportIncidentUseCase {

    void reportIncident(ReportIncidentCommand command, Driver driver);

    record ReportIncidentCommand(
            Long invoiceId,
            String invoiceNumber,
            String partnerName,
            String deliveryAddress,
            String reason,
            String notes,
            Double latitude,
            Double longitude
    ) {
    }
}
