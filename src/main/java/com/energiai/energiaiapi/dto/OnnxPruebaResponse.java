package com.energiai.energiaiapi.dto;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Resultado de inferencia de prueba (mismo contrato de clasificacion que /api/analisis)")
public record OnnxPruebaResponse(
        CategoriaEficiencia categoria,
        Map<String, Double> probabilidades,
        String modeloVersion,
        String fuenteClasificacion,
        float[] vectorEnviado
) {
}
