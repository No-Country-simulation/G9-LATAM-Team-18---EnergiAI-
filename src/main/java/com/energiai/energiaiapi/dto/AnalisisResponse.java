package com.energiai.energiaiapi.dto;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Respuesta de POST /api/analisis. Combina la clasificacion (del frontend o del
 * fallback) con el calculo de negocio del backend (costo, indice, recomendaciones).
 */
public record AnalisisResponse(

        CategoriaEficiencia categoria,
        Map<String, Double> probabilidades,
        double costoEstimadoMensual,
        Double indiceEficiencia,
        List<String> recomendaciones,
        String modeloVersion,
        String fuenteClasificacion,
        boolean guardado,
        Long analisisId,
        Instant creadoEn
) {
}
