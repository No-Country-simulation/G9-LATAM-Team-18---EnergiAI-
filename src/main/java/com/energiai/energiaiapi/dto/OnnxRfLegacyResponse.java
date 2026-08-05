package com.energiai.energiaiapi.dto;

public record OnnxRfLegacyResponse(
        long prediccion,
        double[] probabilidades,
        String modelo
) {
}
