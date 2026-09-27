-- Corrige un hallazgo real de la recalificacion tecnica (docs/EVALUACION_TECNICA.md §18, N3):
-- los indices trigram de V2 quedaron sobre la columna cruda (number/partner_name), pero la
-- busqueda real (SpringDataInvoiceJpaRepository) es JPQL "lower(i.number) like lower(...)",
-- que en SQL se traduce a "lower(number) LIKE ...". Postgres nunca podia usar esos indices
-- para esa expresion (verificado con EXPLAIN: seguia en Seq Scan incluso forzando
-- enable_seqscan=off). Un indice funcional sobre "lower(number)" si coincide con la
-- expresion de la consulta y el operador ~~ (LIKE) que gin_trgm_ops soporta.
--
-- No se reescriben los dos indices de V2 en su propio archivo (Flyway valida el checksum de
-- migraciones ya aplicadas): se eliminan aqui y se reemplazan por los funcionales.
--
-- Sin CREATE INDEX CONCURRENTLY a proposito: el dataset de este piloto es pequeno (decenas o
-- cientos de filas), el bloqueo que toma un CREATE INDEX normal dura milisegundos, y esta
-- migracion ya corre al arrancar, antes de que el healthcheck de Actuator reporte listo.
-- CONCURRENTLY exigiria sacar esta sentencia de la transaccion de Flyway (arriesgando un
-- indice INVALID a medio crear si el proceso se interrumpe), un costo que no se justifica aqui.
DROP INDEX IF EXISTS delivery_invoice_number_trgm;
DROP INDEX IF EXISTS delivery_invoice_partner_name_trgm;

CREATE INDEX delivery_invoice_number_lower_trgm ON delivery_invoice USING gin (lower(number) gin_trgm_ops);
CREATE INDEX delivery_invoice_partner_name_lower_trgm ON delivery_invoice USING gin (lower(partner_name) gin_trgm_ops);
