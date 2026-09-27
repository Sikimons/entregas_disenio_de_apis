-- Completa la migracion a timestamptz que V2 dejo a medias: su comentario decia que
-- confirmed_at pasaba a timestamptz "mismo tipo que delivery_log.created_at", pero esa
-- columna (y pin_locked_until, y app_user.created_at) seguian en timestamp sin zona
-- horaria (docs/EVALUACION_TECNICA.md §18.1/§21). Las entidades JPA correspondientes ya
-- mapean estos campos a java.time.Instant, asi que este cambio no requiere tocar codigo
-- Java: solo alinea el esquema con lo que Hibernate ya asumia.
ALTER TABLE delivery_log ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';
ALTER TABLE delivery_invoice ALTER COLUMN pin_locked_until TYPE timestamptz USING pin_locked_until AT TIME ZONE 'UTC';
ALTER TABLE app_user ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';
