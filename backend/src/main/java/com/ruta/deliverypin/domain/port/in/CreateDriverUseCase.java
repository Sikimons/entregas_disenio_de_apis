package com.ruta.deliverypin.domain.port.in;

import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.model.Role;

public interface CreateDriverUseCase {

    Driver create(CreateDriverCommand command);

    record CreateDriverCommand(String username, String rawPassword, String fullName, Role role) {
    }
}
