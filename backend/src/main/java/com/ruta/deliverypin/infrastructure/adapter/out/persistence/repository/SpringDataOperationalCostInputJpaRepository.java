package com.ruta.deliverypin.infrastructure.adapter.out.persistence.repository;

import com.ruta.deliverypin.infrastructure.adapter.out.persistence.entity.OperationalCostInputJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface SpringDataOperationalCostInputJpaRepository extends JpaRepository<OperationalCostInputJpaEntity, LocalDate> {

    /**
     * Upsert atomico (N7, docs/EVALUACION_TECNICA.md §18): reemplaza el "findById -> setear
     * -> save" que hacia OperationalCostInputAdapter, que bajo dos guardados concurrentes del
     * mismo mes podia terminar en una clave duplicada (los dos leian "no existe" antes de que
     * el primero insertara). Una sola sentencia SQL con ON CONFLICT no tiene esa ventana.
     * clearAutomatically=true: evita que el contexto de persistencia sirva una instancia vieja
     * de esta entidad si, dentro de la misma transaccion, se la vuelve a leer despues.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            insert into operational_cost_input (month, support_hours)
            values (:month, :hours)
            on conflict (month) do update set support_hours = excluded.support_hours
            """, nativeQuery = true)
    void upsertSupportHours(@Param("month") LocalDate month, @Param("hours") double hours);
}
