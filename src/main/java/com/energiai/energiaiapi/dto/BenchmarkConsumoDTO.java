package com.energiai.energiaiapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Benchmark informativo de consumo por tipo de inmueble y estacion.
 * Default: hoja {@code metricas_final}. Rollback: {@code Parametros!B10:E28} via {@code APP_COSTOS_UMBRALES=parametros}.
 * NO clasifica: la categoria de eficiencia la define siempre el modelo ONNX.
 */
@Schema(name = "BenchmarkConsumo",
        description = "Rango de consumo esperado (kWh) para el tipo de inmueble en esa estacion "
                + "(default: hoja metricas_final). Referencia informativa; la clasificacion la hace el ONNX. "
                + "Rollback a Parametros: APP_COSTOS_UMBRALES=parametros")
public record BenchmarkConsumoDTO(

        @JsonProperty("tipo_inmueble")
        @Schema(example = "Departamento")
        String tipoInmueble,

        @Schema(example = "invierno")
        String estacion,

        @JsonProperty("umbral_eficiente_kwh")
        @Schema(description = "Consumo hasta el cual el rango DS es Eficiente (metricas_final, Depto/invierno)",
                example = "249.3")
        double umbralEficienteKwh,

        @JsonProperty("umbral_moderado_kwh")
        @Schema(description = "Consumo hasta el cual el rango DS es Moderado (metricas_final, Depto/invierno = 365.7; "
                + "Parametros = 419.3)",
                example = "365.7")
        double umbralModeradoKwh,

        @JsonProperty("consumo_kwh")
        @Schema(example = "320")
        double consumoKwh,

        @JsonProperty("brecha_vs_eficiente_kwh")
        @Schema(description = "consumo - umbral_eficiente (negativo = por debajo del umbral)",
                example = "70.7")
        double brechaVsEficienteKwh,

        @JsonProperty("posicion_rango")
        @Schema(description = "Ubicacion del consumo en el rango DS",
                example = "moderado",
                allowableValues = {"dentro_eficiente", "moderado", "sobre_moderado"})
        String posicionRango
) {
}
