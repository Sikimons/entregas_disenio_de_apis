package com.ruta.deliverypin.domain.port.in;

import com.ruta.deliverypin.domain.model.Driver;

public interface UpdateDriverUseCase {

    Driver update(Long id, UpdateDriverCommand command);

    record UpdateDriverCommand(String fullName, boolean active, String rawPassword) {
    }
}
