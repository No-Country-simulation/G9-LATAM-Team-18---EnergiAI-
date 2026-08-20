-- V10: Limpiar facturas duplicadas por mes+año para un mismo usuario
-- Mantiene solo el registro más reciente de cada mes/año

-- 1) Primero eliminar las recomendaciones de los análisis duplicados
DELETE FROM analisis_recomendacion
WHERE analisis_id IN (
    SELECT a.id
    FROM analisis a
    JOIN factura f ON f.id = a.factura_id
    WHERE a.id NOT IN (
        SELECT MAX(a2.id)
        FROM analisis a2
        JOIN factura f2 ON f2.id = a2.factura_id
        GROUP BY a2.usuario_id, f2.mes, COALESCE(f2.anio, EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER)
    )
);

-- 2) Ahora eliminar los análisis duplicados (mismo usuario, mismo mes, mismo año)
--    manteniendo solo el más reciente (id mayor)
DELETE FROM analisis 
WHERE id IN (
    SELECT a.id
    FROM analisis a
    JOIN factura f ON f.id = a.factura_id
    WHERE a.id NOT IN (
        SELECT MAX(a2.id)
        FROM analisis a2
        JOIN factura f2 ON f2.id = a2.factura_id
        GROUP BY a2.usuario_id, f2.mes, COALESCE(f2.anio, EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER)
    )
);

-- 3) Las facturas huérfanas (sin analisis que las referencie) 
--    quedan disponibles para limpieza futura si se desea
