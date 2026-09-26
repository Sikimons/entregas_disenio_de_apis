package com.ruta.deliverypin.domain.port.in;

import com.ruta.deliverypin.domain.model.Role;

public interface LoginUseCase {

    AuthResult login(String username, String rawPassword);

    record AuthResult(String token, String username, String fullName, Role role) {
    }
}
