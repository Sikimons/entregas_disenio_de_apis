package com.ruta.deliverypin.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Cache Aside (Fase8): dos caches en memoria (Caffeine) para lecturas de LocalInvoiceAdapter
 * que no cambian una vez creada la factura (lineas de producto, ubicacion esperada). Un
 * SimpleCacheManager con una CaffeineCache por nombre, en vez de CaffeineCacheManager
 * unico, porque cada una necesita su propio TTL (ResilienceConfig ya sigue el mismo
 * patron de "un bean por patron/politica" para el Circuit Breaker y el Retry).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(CacheProperties properties) {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                new CaffeineCache("invoiceLines", Caffeine.newBuilder()
                        .expireAfterWrite(properties.getInvoiceLinesTtlSeconds(), TimeUnit.SECONDS)
                        .maximumSize(properties.getMaxEntries())
                        .build()),
                new CaffeineCache("expectedLocation", Caffeine.newBuilder()
                        .expireAfterWrite(properties.getExpectedLocationTtlSeconds(), TimeUnit.SECONDS)
                        .maximumSize(properties.getMaxEntries())
                        .build())
        ));
        return manager;
    }
}
