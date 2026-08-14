-- Desglose de costos con estacionalidad (Datasetup FINAL.xlsx, hoja Parametros).
-- Solo se completa para analisis guardados por usuario autenticado (modo historial);
-- queda null en los registros anteriores a esta version.
ALTER TABLE analisis
    ADD COLUMN IF NOT EXISTS estacion_calculo VARCHAR(20),
    ADD COLUMN IF NOT EXISTS costo_bruto_mensual DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS pct_estacional DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS pct_ajuste_total DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS pct_ahorro_potencial DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS costo_ajustado_mensual DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS ahorro_potencial_mensual DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS ahorro_potencial_anual DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS costo_anual_estimado DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS costo_anual_estacionalizado DOUBLE PRECISION,
    -- Version de parametros_costos.json vigente al momento del analisis: permite auditar
    -- resultados viejos si mañana cambian los recargos.
    ADD COLUMN IF NOT EXISTS parametros_costos_version VARCHAR(50);
