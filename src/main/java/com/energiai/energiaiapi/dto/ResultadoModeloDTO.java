package com.energiai.energiaiapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Resultado de la clasificacion que el frontend calcula con ONNX y envia al backend.
 * Es opcional en el request: si no viene, el backend clasifica con ONNX.
 */
@Schema(name = "ResultadoModelo", description = "Clasificacion previa del FE (opcional)")
public record ResultadoModeloDTO(

        @Schema(description = "Etiqueta de la clase ganadora", example = "Eficiente",
                allowableValues = {"Eficiente", "Moderado", "Ineficiente"})
        String categoria,

        @Schema(description = "Probabilidades por clase (0..1)",
                example = "{\"Eficiente\":0.7,\"Moderado\":0.2,\"Ineficiente\":0.1}")
        Map<String, Double> probabilidades
) {
}
