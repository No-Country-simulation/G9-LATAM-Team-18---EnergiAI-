-- =============================================================
-- V3 - Mes de la factura (categorico, opcional)
-- Valores esperados en API/persistencia: enero … diciembre
-- =============================================================

ALTER TABLE factura ADD COLUMN IF NOT EXISTS mes VARCHAR(255);
