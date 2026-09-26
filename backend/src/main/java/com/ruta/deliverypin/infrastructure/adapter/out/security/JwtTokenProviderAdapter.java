package com.ruta.deliverypin.infrastructure.adapter.out.security;

import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.port.out.TokenProviderPort;
import com.ruta.deliverypin.infrastructure.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * Adaptador de salida: implementa TokenProviderPort emitiendo/validando JWT firmados con HMAC.
 */
@Component
public class JwtTokenProviderAdapter implements TokenProviderPort {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenProviderAdapter(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateToken(Driver driver) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getExpirationMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(driver.getUsername())
                .claims(Map.of(
                        "role", driver.getRole().name(),
                        "userId", driver.getId(),
                        "fullName", driver.getFullName()
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public Optional<String> extractUsername(String token) {
        try {
            return Optional.of(parseClaims(token).getSubject());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean isValid(String token, String username) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject().equals(username) && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
