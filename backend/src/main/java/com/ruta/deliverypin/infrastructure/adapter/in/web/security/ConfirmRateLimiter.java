package com.ruta.deliverypin.infrastructure.adapter.in.web.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ruta.deliverypin.domain.exception.TooManyConfirmAttemptsException;
import com.ruta.deliverypin.infrastructure.config.AppProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Limita POST /api/v1/driver/deliveries/confirm por conductor autenticado (Tanda 2,
 * auditoria tecnica: "acotar el impacto de un PIN adivinado por fuerza bruta mas alla del
 * bloqueo de 5 intentos por factura"). Mismo patron que LoginRateLimiter (bucket4j en
 * memoria, con Caffeine para no acumular buckets inactivos sin limite).
 *
 * Se limita por driverId, no por IP+driverId como el login: un conductor real cambia de IP
 * seguido en red movil durante su turno, y el token JWT ya identifica de forma confiable de
 * quien es el intento (a diferencia del login, donde todavia no hay identidad verificada).
 * No hay "recordSuccess": a diferencia del login (donde un usuario legitimo puede iniciar
 * sesion varias veces seguidas sin culpa), confirmar entregas espaciadas en el tiempo nunca
 * se acerca al limite real durante un uso normal.
 */
@Component
public class ConfirmRateLimiter {

    private final Cache<Long, Bucket> buckets;
    private final AppProperties.ConfirmRateLimit config;

    public ConfirmRateLimiter(AppProperties appProperties) {
        this.config = appProperties.getConfirmRateLimit();
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofSeconds(config.getWindowSeconds()))
                .maximumSize(100_000)
                .build();
    }

    public void checkAllowed(Long driverId) {
        Bucket bucket = buckets.get(driverId, id -> newBucket());
        if (!bucket.tryConsume(1)) {
            throw new TooManyConfirmAttemptsException();
        }
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(config.getMaxAttempts())
                .refillIntervally(config.getMaxAttempts(), Duration.ofSeconds(config.getWindowSeconds()))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
