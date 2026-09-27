package com.ruta.deliverypin.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cache Aside (Fase8): TTL y tamano maximo de las caches en memoria (Caffeine) que
 * evitan repetir contra la base lecturas inmutables de LocalInvoiceAdapter (lineas de
 * factura y ubicacion esperada), que no cambian una vez creada la factura.
 */
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    private long invoiceLinesTtlSeconds = 300L;
    private long expectedLocationTtlSeconds = 300L;
    private long maxEntries = 2000L;

    public long getInvoiceLinesTtlSeconds() {
        return invoiceLinesTtlSeconds;
    }

    public void setInvoiceLinesTtlSeconds(long invoiceLinesTtlSeconds) {
        this.invoiceLinesTtlSeconds = invoiceLinesTtlSeconds;
    }

    public long getExpectedLocationTtlSeconds() {
        return expectedLocationTtlSeconds;
    }

    public void setExpectedLocationTtlSeconds(long expectedLocationTtlSeconds) {
        this.expectedLocationTtlSeconds = expectedLocationTtlSeconds;
    }

    public long getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(long maxEntries) {
        this.maxEntries = maxEntries;
    }
}
