package com.ruta.deliverypin.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del Circuit Breaker que protege el adaptador de facturas
 * (LocalInvoiceAdapter, la "simulacion del ERP" descrita en Fase1 §3). Activado por
 * defecto: circuitBreakerEnabled=true. Se puede apagar por variable de entorno para
 * comparar el comportamiento con y sin el patron.
 */
@ConfigurationProperties(prefix = "app.resilience")
public class ResilienceProperties {

    private boolean circuitBreakerEnabled = true;
    private float failureRateThreshold = 50f;
    private long waitDurationOpenStateSeconds = 15L;

    /**
     * Retry (Fase8): solo se aplica a las lecturas del "ERP" (LocalInvoiceAdapter), nunca a
     * escrituras que ya sostienen un bloqueo pesimista de fila (ver protectedRead() vs.
     * protectedCall()) - reintentar dentro de una transaccion con lock arriesgaria mantenerlo
     * abierto mas tiempo del necesario.
     */
    private boolean retryEnabled = true;
    private int retryMaxAttempts = 3;
    private long retryWaitDurationMillis = 200L;

    public boolean isCircuitBreakerEnabled() {
        return circuitBreakerEnabled;
    }

    public void setCircuitBreakerEnabled(boolean circuitBreakerEnabled) {
        this.circuitBreakerEnabled = circuitBreakerEnabled;
    }

    public float getFailureRateThreshold() {
        return failureRateThreshold;
    }

    public void setFailureRateThreshold(float failureRateThreshold) {
        this.failureRateThreshold = failureRateThreshold;
    }

    public long getWaitDurationOpenStateSeconds() {
        return waitDurationOpenStateSeconds;
    }

    public void setWaitDurationOpenStateSeconds(long waitDurationOpenStateSeconds) {
        this.waitDurationOpenStateSeconds = waitDurationOpenStateSeconds;
    }

    public boolean isRetryEnabled() {
        return retryEnabled;
    }

    public void setRetryEnabled(boolean retryEnabled) {
        this.retryEnabled = retryEnabled;
    }

    public int getRetryMaxAttempts() {
        return retryMaxAttempts;
    }

    public void setRetryMaxAttempts(int retryMaxAttempts) {
        this.retryMaxAttempts = retryMaxAttempts;
    }

    public long getRetryWaitDurationMillis() {
        return retryWaitDurationMillis;
    }

    public void setRetryWaitDurationMillis(long retryWaitDurationMillis) {
        this.retryWaitDurationMillis = retryWaitDurationMillis;
    }
}
