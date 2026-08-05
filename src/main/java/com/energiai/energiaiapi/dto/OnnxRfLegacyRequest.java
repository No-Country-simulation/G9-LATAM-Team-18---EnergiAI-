package com.energiai.energiaiapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Request legacy para modelo_rf.onnx (10 floats). */
public record OnnxRfLegacyRequest(
        @NotNull Float param1,
        @NotNull Float param2,
        @NotNull Float param3,
        @NotNull Float param4,
        @NotNull Float param5,
        @NotNull Float param6,
        @NotNull Float param7,
        @NotNull Float param8,
        @NotNull Float param9,
        @NotNull Float param10
) {
    public float[] aVector() {
        return new float[]{
                param1, param2, param3, param4, param5,
                param6, param7, param8, param9, param10
        };
    }
}
