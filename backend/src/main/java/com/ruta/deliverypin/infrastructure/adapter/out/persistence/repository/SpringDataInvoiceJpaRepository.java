package com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository;

import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.InvoiceJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataInvoiceJpaRepository extends JpaRepository<InvoiceJpaEntity, Long> {

    /**
     * Bloqueo pesimista de fila (reemplaza "SELECT ... FOR UPDATE" en SQL directo,
     * ver LocalInvoiceAdapter.locked()); Hibernate genera exactamente esa clausula.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InvoiceJpaEntity i where i.id = :id")
    Optional<InvoiceJpaEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select i from InvoiceJpaEntity i
            where i.requiresPin = true and i.state = 'posted' and i.confirmed = false
              and (lower(i.number) like lower(concat('%', :q, '%')) escape '\\'
                or lower(i.partnerName) like lower(concat('%', :q, '%')) escape '\\')
            order by i.invoiceDate desc, i.id desc
            """)
    List<InvoiceJpaEntity> searchPendingDeliveryInvoices(@Param("q") String query, Pageable pageable);

    @Query(value = """
            select i from InvoiceJpaEntity i
            where lower(i.number) like lower(concat('%', :q, '%')) escape '\\'
               or lower(i.partnerName) like lower(concat('%', :q, '%')) escape '\\'
            order by i.id desc
            """,
            countQuery = """
            select count(i) from InvoiceJpaEntity i
            where lower(i.number) like lower(concat('%', :q, '%')) escape '\\'
               or lower(i.partnerName) like lower(concat('%', :q, '%')) escape '\\'
            """)
    Page<InvoiceJpaEntity> search(@Param("q") String query, Pageable pageable);

    /**
     * Escapa "\", "%" y "_" (en ese orden) para que un termino de busqueda con esos
     * caracteres se trate como texto literal, no como comodin de LIKE -- sin esto,
     * buscar "A_1" traia tambien "AB1", "A21", etc. Las dos consultas de arriba usan
     * "escape '\'" para que Postgres respete este escape. N3/N7,
     * docs/EVALUACION_TECNICA.md §18.
     */
    static String escapeLike(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
