package com.ruta.deliverypin.domain.port.in;

import com.ruta.deliverypin.domain.model.DeliveryAttemptSummary;
import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.domain.model.PageResult;

public interface ListDriverDeliveryHistoryUseCase {

    PageResult<DeliveryAttemptSummary> list(Long driverId, PageRequest pageRequest);
}
