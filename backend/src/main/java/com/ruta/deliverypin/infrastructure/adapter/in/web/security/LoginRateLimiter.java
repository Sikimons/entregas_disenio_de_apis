package com.ruta.deliverypin.infrastructure.adapter.in.web.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ruta.deliverypin.domain.exception.TooManyLoginAttemptsException;
import com.ruta.deliverypin.infrastructure.config.AppProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * Limita los intentos de POST /api/v1/auth/login por IP + usuario, en memoria (bucket4j),
 * para mitigar fuerza bruta de contrasenas sin depender de Redis en este piloto.
 *
 * Corrige dos hallazgos de la recalificacion tecnica en vivo (docs/EVALUACION_TECNICA.md
 * §18, N4):
 * 1) Antes un `ConcurrentHashMap` nunca vaciaba sus entradas; un atacante rotando IPs o
 *    usuarios hacia crecer la memoria sin limite. Ahora es una cache de Caffeine (mismo
 *    patron que CacheConfig) con `expireAfterAccess`: una entrada inactiva durante una
 *    ventana completa ya se relleno sola (bucket4j `refillIntervally`), asi que expirarla
 *    no relaja la proteccion real -- un intento rechazado cuenta como acceso, asi que un
 *    atacante que sigue insistiendo mantiene su entrada viva.
 * 2) Antes CADA intento consumia el cupo, incluido un login correcto; un usuario legitimo
 *    que inicia sesion varias veces seguidas (o un script como verify_delivery.py) podia
 *    terminar bloqueado el solo. `recordSuccess` invalida la entrada tras un login
 *    exitoso, asi que solo los intentos fallidos consumen el cupo.
 */
@Component
public class LoginRateLimiter {

    private final Cache<String, Bucket> buckets;
    private final AppProperties.LoginRateLimit config;

    public LoginRateLimiter(AppProperties appProperties) {
        this.config = appProperties.getLoginRateLimit();
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofSeconds(config.getWindowSeconds()))
                .maximumSize(100_000)
                .build();
    }

    public void checkAllowed(String clientIp, String username) {
        Bucket bucket = buckets.get(key(clientIp, username), k -> newBucket());
        if (!bucket.tryConsume(1)) {
            throw new TooManyLoginAttemptsException();
        }
    }

    /** Llamar solo tras un login exitoso: libera el cupo para que no penalice al usuario legitimo. */
    public void recordSuccess(String clientIp, String username) {
        buckets.invalidate(key(clientIp, username));
    }

    private static String key(String clientIp, String username) {
        return clientIp + "|" + (username == null ? "" : username.toLowerCase(Locale.ROOT));
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(config.getMaxAttempts())
                .refillIntervally(config.getMaxAttempts(), Duration.ofSeconds(config.getWindowSeconds()))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
