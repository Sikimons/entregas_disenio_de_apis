-- Tanda 2 (auditoria tecnica, brecha senalada en Fase2 §6): "Auditoria de facturas: no se
-- registra quien creo o publico cada una". Se guarda el username (no una FK a app_user)
-- porque el mismo patron ya existe en delivery_log.driver_name -- un texto simple que
-- sobrevive aunque la cuenta se elimine despues, en vez de una referencia que se rompa o
-- que obligue a un ON DELETE especial solo por motivos de auditoria.
ALTER TABLE delivery_invoice
    ADD COLUMN created_by varchar(80),
    ADD COLUMN published_by varchar(80);
