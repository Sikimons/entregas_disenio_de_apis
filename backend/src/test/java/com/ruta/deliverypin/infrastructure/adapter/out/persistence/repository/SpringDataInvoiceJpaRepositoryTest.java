package com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba unitaria pura de {@link SpringDataInvoiceJpaRepository#escapeLike(String)} (N3/N7,
 * docs/EVALUACION_TECNICA.md §18): no necesita Spring ni una base de datos, solo verifica el
 * escape en sí. El efecto sobre una búsqueda real ya lo cubre
 * {@code LocalInvoiceAdapterIntegrationTest.list_withUnderscoreInQuery_treatsItAsLiteralNotAsWildcard}
 * (Testcontainers).
 */
class SpringDataInvoiceJpaRepositoryTest {

    @Test
    void escapeLike_escapesUnderscoreWildcard() {
        assertThat(SpringDataInvoiceJpaRepository.escapeLike("A_1")).isEqualTo("A\\_1");
    }

    @Test
    void escapeLike_escapesPercentWildcard() {
        assertThat(SpringDataInvoiceJpaRepository.escapeLike("100%")).isEqualTo("100\\%");
    }

    @Test
    void escapeLike_escapesBackslashBeforeOtherCharacters() {
        // El backslash se escapa primero: si se escaparan despues de "_"/"%", el backslash
        // que ESOS reemplazos insertan tambien quedaria (mal) escapado por segunda vez.
        assertThat(SpringDataInvoiceJpaRepository.escapeLike("a\\_b")).isEqualTo("a\\\\\\_b");
    }

    @Test
    void escapeLike_leavesPlainTextUnchanged() {
        assertThat(SpringDataInvoiceJpaRepository.escapeLike("F-IT-004")).isEqualTo("F-IT-004");
    }

    @Test
    void escapeLike_nullReturnsEmptyString() {
        assertThat(SpringDataInvoiceJpaRepository.escapeLike(null)).isEmpty();
    }
}
