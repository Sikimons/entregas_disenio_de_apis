package com.ruta.deliverypin.infrastructure.adapter.in.web.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Cache en memoria de respuestas exitosas ya servidas, por clave de idempotencia (Tanda 2,
 * auditoria tecnica). Header opcional "Idempotency-Key" en POST /driver/deliveries/confirm
 * e /incident: un reintento del cliente con la misma clave (mala conectividad, timeout
 * antes de recibir la respuesta) devuelve la respuesta ya guardada en vez de ejecutar de
 * nuevo el caso de uso.
 *
 * Solo se cachea el camino de EXITO, nunca un rechazo de negocio (PIN invalido, factura
 * inexistente): si el primer intento fallo, no persistio nada, asi que reintentarlo
 * simplemente vuelve a evaluar la misma regla -- cachear el fallo no aportaria nada y
 * complicaria el manejo de excepciones sin necesidad.
 *
 * "/confirm" ya es idempotente por regla de dominio (factura ya confirmada -> 409, ver
 * LocalInvoiceAdapter.confirmDelivery); esta cache cierra el borde mas angosto que esa
 * regla no cubre (el reintento llega ANTES de que el primer intento alcance a bloquear la
 * fila). "/incident" SI se beneficia de forma directa: a diferencia de una confirmacion, no
 * hay ninguna marca de estado que impida crear dos registros de incidencia iguales ante un
 * reintento -- esta cache es la unica proteccion contra esa duplicacion.
 */
@Component
public class IdempotencyKeyStore {

    private final Cache<String, Object> responses = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(50_000)
            .build();

    @SuppressWarnings("unchecked")
    public <T> T getCached(String key) {
        return (T) responses.getIfPresent(key);
    }

    public void put(String key, Object response) {
        responses.put(key, response);
    }
}
