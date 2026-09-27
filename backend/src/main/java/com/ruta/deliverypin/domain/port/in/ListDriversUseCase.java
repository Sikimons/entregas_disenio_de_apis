package com.ruta.deliverypin.domain.port.in;

import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.domain.model.PageResult;

public interface ListDriversUseCase {

    PageResult<Driver> listAll(PageRequest pageRequest);
}
