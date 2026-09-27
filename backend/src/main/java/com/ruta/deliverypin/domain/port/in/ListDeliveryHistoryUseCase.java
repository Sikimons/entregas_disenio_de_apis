package com.ruta.deliverypin.domain.port.in;

import com.ruta.deliverypin.domain.model.DeliveryAttemptSummary;
import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.domain.model.PageResult;

public interface ListDeliveryHistoryUseCase {

    PageResult<DeliveryAttemptSummary> list(PageRequest pageRequest);
}
