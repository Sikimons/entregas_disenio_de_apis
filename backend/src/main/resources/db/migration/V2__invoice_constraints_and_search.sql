-- Fase8: cierra dos observaciones de RA3/RA4 sobre delivery_invoice (V1__baseline.sql):
-- 1) "state" no tenia CHECK ni enum en base (solo se validaba en Java); los tres unicos
--    valores reales son draft/posted/cancel (ver LocalInvoiceAdapter y demo/invoices.json).
-- 2) confirmed_at era "timestamp" sin zona horaria; pasa a timestamptz para que JPA lo
--    mapee como java.time.Instant sin ambiguedad de zona (mismo tipo que delivery_log.created_at).
-- Tambien agrega el indice trigram que la busqueda por ILIKE '%q%' necesitaba (RA3 §5.2/§9).

ALTER TABLE delivery_invoice
    ADD CONSTRAINT delivery_invoice_state_check CHECK (state IN ('draft', 'posted', 'cancel'));

ALTER TABLE delivery_invoice
    ALTER COLUMN confirmed_at TYPE timestamptz USING confirmed_at AT TIME ZONE 'UTC';

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX delivery_invoice_number_trgm ON delivery_invoice USING gin (number gin_trgm_ops);
CREATE INDEX delivery_invoice_partner_name_trgm ON delivery_invoice USING gin (partner_name gin_trgm_ops);
