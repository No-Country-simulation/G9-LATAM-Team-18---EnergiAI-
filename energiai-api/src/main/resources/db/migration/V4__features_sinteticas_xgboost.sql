-- Features sinteticas del pipeline xgboost (persistidas al guardar analisis).
ALTER TABLE factura
    ADD COLUMN IF NOT EXISTS intensidad_por_equipo DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS horas_pico_interaccion DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS desviacion_equipos_tipo DOUBLE PRECISION;
