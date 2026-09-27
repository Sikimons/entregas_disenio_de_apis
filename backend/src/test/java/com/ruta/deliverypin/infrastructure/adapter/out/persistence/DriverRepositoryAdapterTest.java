package com.ruta.deliverypin.infrastructure.adapter.out.persistence;

import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.domain.model.Role;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.DriverJpaEntity;
import com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository.SpringDataDriverJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cubre el adaptador de persistencia de usuarios (auditoria tecnica, Tanda 2). */
class DriverRepositoryAdapterTest {

    private final SpringDataDriverJpaRepository jpaRepository = mock(SpringDataDriverJpaRepository.class);
    private final DriverRepositoryAdapter adapter = new DriverRepositoryAdapter(jpaRepository);

    private DriverJpaEntity entity(Long id, String username) {
        return new DriverJpaEntity(id, username, "hash", "Nombre Completo", Role.CONDUCTOR, true, Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void findByUsername_present_mapsToDomain() {
        when(jpaRepository.findByUsernameIgnoreCase("conductor1")).thenReturn(Optional.of(entity(1L, "conductor1")));

        Optional<Driver> result = adapter.findByUsername("conductor1");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("conductor1");
        assertThat(result.get().getRole()).isEqualTo(Role.CONDUCTOR);
    }

    @Test
    void findByUsername_missing_returnsEmpty() {
        when(jpaRepository.findByUsernameIgnoreCase("nadie")).thenReturn(Optional.empty());

        assertThat(adapter.findByUsername("nadie")).isEmpty();
    }

    @Test
    void findById_delegatesAndMaps() {
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity(1L, "conductor1")));

        assertThat(adapter.findById(1L)).isPresent();
    }

    @Test
    void existsByUsername_delegatesToRepository() {
        when(jpaRepository.existsByUsernameIgnoreCase("admin")).thenReturn(true);
        when(jpaRepository.existsByUsernameIgnoreCase("nadie")).thenReturn(false);

        assertThat(adapter.existsByUsername("admin")).isTrue();
        assertThat(adapter.existsByUsername("nadie")).isFalse();
    }

    @Test
    void findAll_mapsPageContentAndMetadata_sortedById() {
        var page = new PageImpl<>(List.of(entity(1L, "admin"), entity(2L, "conductor1")),
                org.springframework.data.domain.PageRequest.of(0, 20), 2);
        when(jpaRepository.findAll(any(org.springframework.data.domain.PageRequest.class))).thenReturn(page);

        var result = adapter.findAll(new PageRequest(0, 20));

        assertThat(result.content()).extracting("username").containsExactly("admin", "conductor1");
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void save_delegatesToJpaRepository_andMapsBackToDomain() {
        Driver newDriver = Driver.createNew("conductor2", "hash", "Conductor Dos", Role.CONDUCTOR);
        when(jpaRepository.save(any())).thenReturn(entity(3L, "conductor2"));

        Driver saved = adapter.save(newDriver);

        assertThat(saved.getId()).isEqualTo(3L);
        assertThat(saved.getUsername()).isEqualTo("conductor2");
    }

    @Test
    void deleteById_delegatesToJpaRepository() {
        adapter.deleteById(7L);

        verify(jpaRepository, times(1)).deleteById(7L);
    }
}
