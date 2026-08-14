package com.energiai.energiaiapi.dto;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Schema(name = "AnalisisResponse", description = "Resultado de clasificacion + negocio + eco ONNX")
public record AnalisisResponse(

        @Schema(description = "Categoria ganadora", example = "MODERADO")
        CategoriaEficiencia categoria,

        @Schema(description = "Probabilidades por etiqueta de negocio (suman ~1)",
                example = "{\"Eficiente\":0.32,\"Moderado\":0.41,\"Ineficiente\":0.27}")
        Map<String, Double> probabilidades,

        @Schema(description = "Costo mensual estimado (consumo × tarifa)", example = "240.375")
        double costoEstimadoMensual,

        @Schema(description = "Indice IIE = consumo/personas; null si no hay numero_personas",
                example = "80.125", nullable = true)
        Double indiceEficiencia,

        @ArraySchema(schema = @Schema(example = "Evita equipos de alto consumo en horario pico."))
        List<String> recomendaciones,

        @Schema(description = "Version del modelo reportada", example = "xgboost-v2")
        String modeloVersion,

        @Schema(description = "FRONTEND_ONNX | BACKEND_ONNX | BACKEND_FALLBACK", example = "BACKEND_ONNX")
        String fuenteClasificacion,

        @Schema(description = "true si se persistio en historial")
        boolean guardado,

        @Schema(description = "Id del analisis si guardado", nullable = true)
        Long analisisId,

        Instant creadoEn,

        @JsonProperty("consulta_modelo")
        @Schema(description = "Eco de parametros DS enviados al preproceso")
        ConsultaModeloDTO consultaModelo,

        @JsonProperty("features_sinteticas")
        @Schema(description = "Sinteticas A/B/C")
        FeaturesSinteticasDTO featuresSinteticas,

        @JsonProperty("vector_onnx")
        @ArraySchema(arraySchema = @Schema(description = "Tensor float[22] (orden metadata_backend.json)"),
                schema = @Schema(type = "number", format = "float"))
        float[] vectorOnnx,

        @JsonProperty("costos")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "Desglose con estacionalidad y recargos. Solo con JWT; omitido en modo invitado. "
                + "Incluye `benchmark` (umbrales metricas_final por default) y `fuente_umbrales`.",
                nullable = true)
        CostosEstacionalesDTO costos,

        @JsonProperty("historial_resumen")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "Comparativa contra los analisis previos del usuario. Solo con JWT",
                nullable = true)
        ResumenHistorialDTO historialResumen
) {
}
