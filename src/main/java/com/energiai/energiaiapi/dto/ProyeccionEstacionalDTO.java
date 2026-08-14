package com.energiai.energiaiapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Costo mensual proyectado para una estacion, manteniendo consumo y tarifa actuales.
 */
@Schema(name = "ProyeccionEstacional",
        description = "Costo mensual estimado si el mismo hogar consumiera lo mismo en esa estacion")
public record ProyeccionEstacionalDTO(

        @Schema(example = "invierno", allowableValues = {"primavera", "verano", "otoño", "invierno"})
        String estacion,

        @JsonProperty("pct_estacional")
        @Schema(description = "Recargo estacional (0..1)", example = "0.1")
        double pctEstacional,

        @JsonProperty("costo_mensual_estimado")
        @Schema(description = "costo_bruto x (1 + pct_estacional + recargos accionables)", example = "234.375")
        double costoMensualEstimado,

        @JsonProperty("es_estacion_actual")
        @Schema(description = "true para la estacion inferida desde month")
        boolean esEstacionActual
) {
}
