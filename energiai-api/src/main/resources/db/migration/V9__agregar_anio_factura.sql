-- V9: Agregar campo anio a factura para validar duplicados mes+año
ALTER TABLE factura ADD COLUMN IF NOT EXISTS anio INTEGER;

-- Asignar año actual a registros existentes que no tengan año
UPDATE factura SET anio = EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER WHERE anio IS NULL;

-- Crear índice para búsqueda eficiente de duplicados por usuario+mes+año
-- (El usuario está en analisis, que tiene FK a factura)
CREATE INDEX IF NOT EXISTS idx_factura_mes_anio ON factura(mes, anio);
