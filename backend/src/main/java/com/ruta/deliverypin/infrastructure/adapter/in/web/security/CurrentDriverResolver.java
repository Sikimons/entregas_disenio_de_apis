package com.ruta.deliverypin.infrastructure.adapter.in.web.security;

import com.ruta.deliverypin.domain.exception.DriverNotFoundException;
import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.port.in.FindDriverByUsernameUseCase;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resuelve el Driver de dominio autenticado en la peticion actual, para los
 * controladores que necesitan la identidad de quien confirma una entrega.
 */
@Component
public class CurrentDriverResolver {

    private final FindDriverByUsernameUseCase findDriverByUsernameUseCase;

    public CurrentDriverResolver(FindDriverByUsernameUseCase findDriverByUsernameUseCase) {
        this.findDriverByUsernameUseCase = findDriverByUsernameUseCase;
    }

    public Driver resolve() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return findDriverByUsernameUseCase.findByUsername(username)
                .orElseThrow(() -> new DriverNotFoundException(username));
    }
}
