package com.ruta.deliverypin.infrastructure.adapter.out.persistence;

import com.ruta.deliverypin.domain.model.DeliveryAttemptOutcome;
import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.domain.model.StoredPhoto;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository.SpringDataDeliveryAttemptJpaRepository;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository.SpringDataDeliveryAttemptJpaRepository.AttemptSummaryProjection;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository.SpringDataDeliveryAttemptJpaRepository.DriverOutcomeCountProjection;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository.SpringDataDeliveryAttemptJpaRepository.PhotoProjection;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository.SpringDataDeliveryAttemptJpaRepository.VolumeProjection;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Cubre el mapeo de proyecciones JPA a modelos de dominio (auditoria tecnica, Tanda 2):
 * esta clase quedo con 2.3% de cobertura (6 de 258 instrucciones) porque solo se ejercitaba
 * indirectamente via los tests de integracion de otros adaptadores. Es la que alimenta el
 * historial paginado, el mapa y las metricas del tablero admin -- justo lo que
 * "Datos y persistencia" (auditoria §6) pide revisar.
 */
class DeliveryAttemptRepositoryAdapterTest {

    private final SpringDataDeliveryAttemptJpaRepository jpaRepository = mock(SpringDataDeliveryAttemptJpaRepository.class);
    private final DeliveryAttemptRepositoryAdapter adapter = new DeliveryAttemptRepositoryAdapter(jpaRepository);

    private AttemptSummaryProjection summaryProjection(Long id, String driverName, DeliveryAttemptOutcome outcome, boolean hasPhoto) {
        AttemptSummaryProjection projection = mock(AttemptSummaryProjection.class);
        when(projection.getId()).thenReturn(id);
        when(projection.getInvoiceId()).thenReturn(1L);
        when(projection.getInvoiceNumber()).thenReturn("F-001");
        when(projection.getPartnerName()).thenReturn("Cliente");
        when(projection.getDeliveryAddress()).thenReturn("Direccion");
        when(projection.getDriverName()).thenReturn(driverName);
        when(projection.getOutcome()).thenReturn(outcome);
        when(projection.getLatitude()).thenReturn(-2.17);
        when(projection.getLongitude()).thenReturn(-79.92);
        when(projection.getDetail()).thenReturn(null);
        when(projection.getHasPhoto()).thenReturn(hasPhoto);
        when(projection.getDistanceFromExpectedMeters()).thenReturn(12.5);
        when(projection.getCreatedAt()).thenReturn(Instant.parse("2026-09-01T10:00:00Z"));
        return projection;
    }

    @Test
    void findAllOrderByCreatedAtDesc_mapsProjectionToSummary_preservingPageMetadata() {
        Page<AttemptSummaryProjection> page = new PageImpl<>(
                List.of(summaryProjection(1L, "Conductor Uno", DeliveryAttemptOutcome.CONFIRMED, true)),
                org.springframework.data.domain.PageRequest.of(0, 20), 1);
        when(jpaRepository.findSummaries(any())).thenReturn(page);

        var result = adapter.findAllOrderByCreatedAtDesc(new PageRequest(0, 20));

        assertThat(result.content()).hasSize(1);
        var summary = result.content().get(0);
        assertThat(summary.id()).isEqualTo(1L);
        assertThat(summary.driverName()).isEqualTo("Conductor Uno");
        assertThat(summary.outcome()).isEqualTo(DeliveryAttemptOutcome.CONFIRMED);
        assertThat(summary.hasPhoto()).isTrue();
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void findAllByDriverOrderByCreatedAtDesc_delegatesToDriverScopedQuery() {
        Page<AttemptSummaryProjection> page = new PageImpl<>(
                List.of(summaryProjection(2L, "Conductor Dos", DeliveryAttemptOutcome.INCIDENT, false)));
        when(jpaRepository.findSummariesByDriverId(org.mockito.ArgumentMatchers.eq(5L), any())).thenReturn(page);

        var result = adapter.findAllByDriverOrderByCreatedAtDesc(5L, new PageRequest(0, 20));

        assertThat(result.content()).extracting("driverName").containsExactly("Conductor Dos");
        assertThat(result.content().get(0).hasPhoto()).isFalse();
    }

    @Test
    void findPhotoByAttemptId_mapsProjectionToStoredPhoto() {
        PhotoProjection projection = mock(PhotoProjection.class);
        byte[] bytes = {1, 2, 3};
        when(projection.getPhoto()).thenReturn(bytes);
        when(projection.getPhotoContentType()).thenReturn("image/jpeg");
        when(jpaRepository.findPhotoById(9L)).thenReturn(Optional.of(projection));

        Optional<StoredPhoto> result = adapter.findPhotoByAttemptId(9L);

        assertThat(result).isPresent();
        assertThat(result.get().data()).isEqualTo(bytes);
        assertThat(result.get().contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void findPhotoByAttemptId_missing_returnsEmpty() {
        when(jpaRepository.findPhotoById(9L)).thenReturn(Optional.empty());

        assertThat(adapter.findPhotoByAttemptId(9L)).isEmpty();
    }

    @Test
    void findPhotoByAttemptIdAndDriverId_filtersByBothIds() {
        // La variante usada por DriverDeliveryController.deliveryPhoto() (evita IDOR: un
        // conductor no puede ver la foto de una entrega ajena por el solo id del intento).
        PhotoProjection projection = mock(PhotoProjection.class);
        when(projection.getPhoto()).thenReturn(new byte[]{9});
        when(projection.getPhotoContentType()).thenReturn("image/png");
        when(jpaRepository.findPhotoByIdAndDriverId(9L, 10L)).thenReturn(Optional.of(projection));
        when(jpaRepository.findPhotoByIdAndDriverId(9L, 99L)).thenReturn(Optional.empty());

        assertThat(adapter.findPhotoByAttemptIdAndDriverId(9L, 10L)).isPresent();
        assertThat(adapter.findPhotoByAttemptIdAndDriverId(9L, 99L)).isEmpty();
    }

    @Test
    void findAllBetween_mapsEachProjectionInTheRange() {
        // La lista se construye en una variable ANTES del when(...).thenReturn(...): anidar
        // aqui mismo las llamadas a summaryProjection() (que abren su propio ciclo
        // mock()/when()/thenReturn()) corrompe el registro global de "stubbing en curso" de
        // Mockito, que no tolera un when()/thenReturn() empezando otro sin terminar el de
        // afuera -- por eso este mismo patron aparece ya resuelto en los tests de arriba.
        List<AttemptSummaryProjection> projections = List.of(
                summaryProjection(1L, "Conductor Uno", DeliveryAttemptOutcome.CONFIRMED, true),
                summaryProjection(2L, "Conductor Dos", DeliveryAttemptOutcome.REJECTED, false));
        when(jpaRepository.findSummariesByCreatedAtBetween(any(), any())).thenReturn(projections);

        var result = adapter.findAllBetween(Instant.EPOCH, Instant.now());

        assertThat(result).hasSize(2);
        assertThat(result).extracting("outcome")
                .containsExactly(DeliveryAttemptOutcome.CONFIRMED, DeliveryAttemptOutcome.REJECTED);
    }

    private DriverOutcomeCountProjection countProjection(String driverName, DeliveryAttemptOutcome outcome, long total) {
        DriverOutcomeCountProjection projection = mock(DriverOutcomeCountProjection.class);
        when(projection.getDriverName()).thenReturn(driverName);
        when(projection.getOutcome()).thenReturn(outcome);
        when(projection.getTotal()).thenReturn(total);
        return projection;
    }

    @Test
    void metricsBetween_aggregatesCountsPerDriver_acrossTheThreeOutcomes() {
        // La agregacion real ocurre en SQL (GROUP BY); esta parte solo reorganiza las filas
        // (driver, outcome, total) en un DriverMetric por conductor. Verifica que el mapeo
        // por "switch(outcome)" reparta cada conteo en la columna correcta.
        List<DriverOutcomeCountProjection> rows = List.of(
                countProjection("Conductor Uno", DeliveryAttemptOutcome.CONFIRMED, 5L),
                countProjection("Conductor Uno", DeliveryAttemptOutcome.REJECTED, 2L),
                countProjection("Conductor Dos", DeliveryAttemptOutcome.INCIDENT, 1L));
        when(jpaRepository.countByDriverAndOutcomeBetween(any(), any())).thenReturn(rows);

        var metrics = adapter.metricsBetween(Instant.EPOCH, Instant.now());

        assertThat(metrics).hasSize(2);
        var uno = metrics.stream().filter(m -> m.driverName().equals("Conductor Uno")).findFirst().orElseThrow();
        assertThat(uno.confirmed()).isEqualTo(5L);
        assertThat(uno.rejected()).isEqualTo(2L);
        assertThat(uno.incident()).isZero();
        assertThat(uno.total()).isEqualTo(7L);

        var dos = metrics.stream().filter(m -> m.driverName().equals("Conductor Dos")).findFirst().orElseThrow();
        assertThat(dos.incident()).isEqualTo(1L);
    }

    @Test
    void metricsBetween_sortsByTotalDescending() {
        List<DriverOutcomeCountProjection> rows = List.of(
                countProjection("Conductor Bajo", DeliveryAttemptOutcome.CONFIRMED, 1L),
                countProjection("Conductor Alto", DeliveryAttemptOutcome.CONFIRMED, 10L));
        when(jpaRepository.countByDriverAndOutcomeBetween(any(), any())).thenReturn(rows);

        var metrics = adapter.metricsBetween(Instant.EPOCH, Instant.now());

        assertThat(metrics).extracting("driverName").containsExactly("Conductor Alto", "Conductor Bajo");
    }

    @Test
    void summarizeConfirmedBetween_mapsVolumeProjection() {
        VolumeProjection projection = mock(VolumeProjection.class);
        when(projection.getConfirmed()).thenReturn(42L);
        when(projection.getPhotoBytes()).thenReturn(1_048_576L);
        when(jpaRepository.summarizeConfirmedBetween(any(), any())).thenReturn(projection);

        var summary = adapter.summarizeConfirmedBetween(Instant.EPOCH, Instant.now());

        assertThat(summary.confirmedDeliveries()).isEqualTo(42L);
        assertThat(summary.evidencePhotoBytes()).isEqualTo(1_048_576L);
    }

    @Test
    void save_delegatesToJpaRepository_andMapsBackToDomain() {
        var attempt = com.ruta.deliverypin.domain.model.DeliveryAttempt.incident(
                1L, "F-001", "Cliente", "Direccion",
                new com.ruta.deliverypin.domain.model.Driver(10L, "conductor1", "hash", "Conductor Uno",
                        com.ruta.deliverypin.domain.model.Role.CONDUCTOR, true, Instant.now()),
                null, "Cliente ausente", "notas"
        );
        var entity = com.ruta.deliverypin.infrastructure.adapter.out.persistence.DeliveryAttemptPersistenceMapper.toEntity(attempt);
        when(jpaRepository.save(any())).thenReturn(entity);

        var saved = adapter.save(attempt);

        assertThat(saved.getOutcome()).isEqualTo(com.ruta.deliverypin.domain.model.DeliveryAttemptOutcome.INCIDENT);
    }
}
