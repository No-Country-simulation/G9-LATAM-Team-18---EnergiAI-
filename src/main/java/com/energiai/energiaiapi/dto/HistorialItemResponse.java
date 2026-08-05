package com.energiai.energiaiapi.dto;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;

import java.time.Instant;
import java.util.List;

/**
 * Item del historial de analisis de un usuario autenticado.
 */
public record HistorialItemResponse(

        Long id,
        Instant creadoEn,
        CategoriaEficiencia categoria,
        double probabilidad,
        double costoEstimadoMensual,
        Double indiceEficiencia,
        Double consumoMensual,
        String tipoInmueble,
        List<String> recomendaciones
) {
}
