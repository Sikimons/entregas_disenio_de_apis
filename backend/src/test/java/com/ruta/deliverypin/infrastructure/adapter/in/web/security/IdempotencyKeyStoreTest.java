package com.ruta.deliverypin.infrastructure.adapter.in.web.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyKeyStoreTest {

    private record FakeResponse(boolean success, String message) {
    }

    @Test
    void getCached_missingKey_returnsNull() {
        IdempotencyKeyStore store = new IdempotencyKeyStore();

        FakeResponse cached = store.getCached("confirm:1:key-a");

        assertThat(cached).isNull();
    }

    @Test
    void put_thenGetCached_returnsTheSameStoredResponse() {
        IdempotencyKeyStore store = new IdempotencyKeyStore();
        FakeResponse response = new FakeResponse(true, "Entrega confirmada correctamente.");

        store.put("confirm:1:key-a", response);

        assertThat((FakeResponse) store.getCached("confirm:1:key-a")).isEqualTo(response);
    }

    @Test
    void put_differentKeys_doNotCollide() {
        // Distintos conductores o distintos endpoints (confirm/incident) con la misma clave
        // de cliente no deben pisarse: DriverDeliveryController.idempotencyCacheKey()
        // antepone el endpoint y el driverId a la clave del cliente.
        IdempotencyKeyStore store = new IdempotencyKeyStore();
        FakeResponse confirmResponse = new FakeResponse(true, "confirm");
        FakeResponse incidentResponse = new FakeResponse(true, "incident");

        store.put("confirm:1:key-a", confirmResponse);
        store.put("incident:1:key-a", incidentResponse);

        assertThat((FakeResponse) store.getCached("confirm:1:key-a")).isEqualTo(confirmResponse);
        assertThat((FakeResponse) store.getCached("incident:1:key-a")).isEqualTo(incidentResponse);
    }
}
