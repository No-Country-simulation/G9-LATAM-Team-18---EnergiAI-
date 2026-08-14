package com.energiai.energiaiapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Desglose estacional tal como quedo congelado al guardar el analisis. Es la foto persistida:
 * no se recalcula en lectura, para que un cambio posterior en los parametros no altere el
 * historial. La proyeccion por estacion no se persiste; se obtiene al analizar
 * ({@code POST /api/analisis}).
 */
@Schema(name = "CostosGuardados",
        description = "Costos con estacionalidad congelados al momento de guardar el analisis")
public record CostosGuardadosDTO(

        @Schema(example = "invierno", nullable = true)
        String estacion,

        @JsonProperty("costo_bruto_mensual")
        @Schema(example = "240.0", nullable = true)
        Double costoBrutoMensual,

        @JsonProperty("pct_estacional")
        @Schema(example = "0.1", nullable = true)
        Double pctEstacional,

        @JsonProperty("pct_ajuste_total")
        @Schema(example = "0.4", nullable = true)
        Double pctAjusteTotal,

        @JsonProperty("costo_ajustado_mensual")
        @Schema(example = "336.0", nullable = true)
        Double costoAjustadoMensual,

        @JsonProperty("pct_ahorro_potencial")
        @Schema(example = "0.3", nullable = true)
        Double pctAhorroPotencial,

        @JsonProperty("ahorro_potencial_mensual")
        @Schema(example = "72.0", nullable = true)
        Double ahorroPotencialMensual,

        @JsonProperty("ahorro_potencial_anual")
        @Schema(example = "864.0", nullable = true)
        Double ahorroPotencialAnual,

        @JsonProperty("costo_anual_estimado")
        @Schema(example = "4032.0", nullable = true)
        Double costoAnualEstimado,

        @JsonProperty("costo_anual_estacionalizado")
        @Schema(example = "3931.32", nullable = true)
        Double costoAnualEstacionalizado,

        @JsonProperty("parametros_version")
        @Schema(example = "datasetup-final-final-v1", nullable = true)
        String parametrosVersion
) {
}
