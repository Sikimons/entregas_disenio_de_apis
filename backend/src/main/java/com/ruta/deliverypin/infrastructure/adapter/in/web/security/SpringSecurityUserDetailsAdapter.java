package com.ruta.deliverypin.infrastructure.adapter.in.web.security;

import com.ruta.deliverypin.domain.port.in.FindDriverByUsernameUseCase;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Puente entre Spring Security y el caso de uso de dominio FindDriverByUsernameUseCase.
 */
@Component
public class SpringSecurityUserDetailsAdapter implements UserDetailsService {

    private final FindDriverByUsernameUseCase findDriverByUsernameUseCase;

    public SpringSecurityUserDetailsAdapter(FindDriverByUsernameUseCase findDriverByUsernameUseCase) {
        this.findDriverByUsernameUseCase = findDriverByUsernameUseCase;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return findDriverByUsernameUseCase.findByUsername(username)
                .map(DriverPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }
}
