package com.energiai.energiaiapi.dto;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(name = "OnnxPruebaResponse",
        description = "Resultado de inferencia aislada (sin costo/recomendaciones)")
public record OnnxPruebaResponse(

        @Schema(example = "MODERADO")
        CategoriaEficiencia categoria,

        @Schema(example = "{\"Eficiente\":0.32,\"Moderado\":0.41,\"Ineficiente\":0.27}")
        Map<String, Double> probabilidades,

        @Schema(example = "xgboost-v2")
        String modeloVersion,

        @Schema(example = "BACKEND_ONNX")
        String fuenteClasificacion,

        @ArraySchema(arraySchema = @Schema(description = "Vector float[22] enviado a ONNX"),
                schema = @Schema(type = "number", format = "float"))
        float[] vectorEnviado
) {
}
