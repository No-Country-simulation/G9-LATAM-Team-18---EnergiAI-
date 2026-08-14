package com.energiai.energiaiapi.dto;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Comparativa del analisis actual contra los analisis ya guardados del usuario.
 * Solo tiene sentido en modo historial (usuario autenticado); en modo invitado viaja en null.
 * Los promedios excluyen el analisis en curso: se calculan sobre lo que ya estaba persistido.
 */
@Schema(name = "ResumenHistorial", description = """
        Comparativa contra el historial persistido del usuario.
        Los promedios se calculan sobre los analisis previos (no incluyen el actual).
        """)
public record ResumenHistorialDTO(

        @JsonProperty("analisis_previos")
        @Schema(description = "Cantidad de analisis ya guardados por el usuario", example = "4")
        int analisisPrevios,

        @JsonProperty("consumo_promedio_kwh")
        @Schema(description = "Promedio de consumo de los analisis previos", example = "300.0",
                nullable = true)
        Double consumoPromedioKwh,

        @JsonProperty("costo_promedio_mensual")
        @Schema(description = "Promedio del costo mensual guardado en los analisis previos",
                example = "225.0", nullable = true)
        Double costoPromedioMensual,

        @JsonProperty("variacion_consumo_pct")
        @Schema(description = "Variacion del consumo actual vs el promedio previo (0.083 = +8.3%)",
                example = "0.0833", nullable = true)
        Double variacionConsumoPct,

        @JsonProperty("variacion_costo_pct")
        @Schema(description = "Variacion del costo actual vs el promedio previo", example = "0.0833",
                nullable = true)
        Double variacionCostoPct,

        @Schema(description = "Direccion del consumo respecto del promedio previo (umbral 5%)",
                example = "al_alza",
                allowableValues = {"al_alza", "a_la_baja", "estable", "sin_referencia"})
        String tendencia,

        @JsonProperty("ultimo_analisis_en")
        @Schema(nullable = true)
        Instant ultimoAnalisisEn,

        @JsonProperty("ultima_categoria")
        @Schema(example = "MODERADO", nullable = true)
        CategoriaEficiencia ultimaCategoria,

        @JsonProperty("ultimo_consumo_kwh")
        @Schema(example = "310.0", nullable = true)
        Double ultimoConsumoKwh,

        @JsonProperty("analisis_misma_estacion")
        @Schema(description = "Analisis previos que caen en la misma estacion que el actual", example = "2")
        int analisisMismaEstacion,

        @JsonProperty("consumo_promedio_misma_estacion_kwh")
        @Schema(description = "Promedio de consumo en la misma estacion (comparacion like-for-like)",
                example = "315.0", nullable = true)
        Double consumoPromedioMismaEstacionKwh,

        @JsonProperty("variacion_vs_misma_estacion_pct")
        @Schema(description = "Variacion del consumo actual vs el promedio de la misma estacion",
                example = "-0.0159", nullable = true)
        Double variacionVsMismaEstacionPct,

        @Schema(description = "Estacion usada para la comparacion like-for-like", example = "invierno",
                nullable = true)
        String estacion
) {

    public static ResumenHistorialDTO sinPrevios(String estacion) {
        return new ResumenHistorialDTO(
                0, null, null, null, null, "sin_referencia",
                null, null, null, 0, null, null, estacion);
    }

    public boolean tienePrevios() {
        return analisisPrevios > 0;
    }
}
