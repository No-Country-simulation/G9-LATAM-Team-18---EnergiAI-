-- Corregir años incorrectos basándose en la fecha de creación del análisis
-- Los registros de Sep-Dic fueron asignados 2026 por V9 pero deberían ser 2025
-- si fueron creados antes de 2026

UPDATE factura f
SET anio = EXTRACT(YEAR FROM a.creado_en)::INTEGER
FROM analisis a
WHERE f.id = a.factura_id
  AND f.anio != EXTRACT(YEAR FROM a.creado_en)::INTEGER
  AND f.mes IN ('septiembre', 'octubre', 'noviembre', 'diciembre', '9', '10', '11', '12')
  AND EXTRACT(YEAR FROM a.creado_en) < 2026;

-- También actualizar registros del primer trimestre que fueron creados el año anterior
UPDATE factura f
SET anio = EXTRACT(YEAR FROM a.creado_en)::INTEGER
FROM analisis a
WHERE f.id = a.factura_id
  AND f.anio != EXTRACT(YEAR FROM a.creado_en)::INTEGER
  AND f.mes IN ('enero', 'febrero', 'marzo', '1', '2', '3')
  AND EXTRACT(YEAR FROM a.creado_en) < 2026;
