package com.ruta.deliverypin.domain.port.in;

import com.ruta.deliverypin.domain.model.DeliveryAttempt;
import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.domain.model.PageResult;

public interface ListDriverDeliveryHistoryUseCase {

    PageResult<DeliveryAttempt> list(Long driverId, PageRequest pageRequest);
}
