package com.energiai.energiaiapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Request legacy para modelo_rf.onnx (10 floats). */
@Schema(name = "OnnxRfLegacyRequest", description = "10 features crudas para modelo_rf.onnx (legacy)")
public record OnnxRfLegacyRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Float param1,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Float param2,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Float param3,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Float param4,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Float param5,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Float param6,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Float param7,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Float param8,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Float param9,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Float param10
) {
    public float[] aVector() {
        return new float[]{
                param1, param2, param3, param4, param5,
                param6, param7, param8, param9, param10
        };
    }
}
