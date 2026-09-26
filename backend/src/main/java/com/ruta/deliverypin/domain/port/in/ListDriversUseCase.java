package com.ruta.deliverypin.domain.port.in;

import com.ruta.deliverypin.domain.model.Driver;

import java.util.List;

public interface ListDriversUseCase {

    List<Driver> listAll();
}
