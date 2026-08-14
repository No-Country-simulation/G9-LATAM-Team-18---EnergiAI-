-- =============================================================
-- V2 - Actualizacion del contrato de variables de la factura
-- - consumo_kwh (int) -> consumo_mensual (float)
-- - horas_alto_consumo -> horas_promedio_uso
-- - area_inmueble -> estacion_anio (texto categorico)
-- - tiene_calentador_electrico -> tiene_calentador
-- =============================================================

ALTER TABLE factura RENAME COLUMN consumo_kwh TO consumo_mensual;
ALTER TABLE factura ALTER COLUMN consumo_mensual TYPE DOUBLE PRECISION
    USING consumo_mensual::double precision;

ALTER TABLE factura RENAME COLUMN horas_alto_consumo TO horas_promedio_uso;

ALTER TABLE factura DROP COLUMN IF EXISTS area_inmueble;
ALTER TABLE factura ADD COLUMN IF NOT EXISTS estacion_anio VARCHAR(255);

ALTER TABLE factura RENAME COLUMN tiene_calentador_electrico TO tiene_calentador;
