package com.energiai.energiaiapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Desglose de costos con estacionalidad y recargos, derivado de {@code Datasetup FINAL FINAL.xlsx}
 * (hoja Parametros, columnas Q..AJ; benchmark de metricas_final). Solo se calcula para el usuario
 * autenticado (modo historial): en modo invitado el costo sigue siendo consumo x tarifa y este
 * bloque viaja en null.
 */
@Schema(name = "CostosEstacionales", description = """
        Costos con estacionalidad y recargos (solo usuario autenticado).
        Trazabilidad con la planilla DS: costo_bruto_mensual = Q, monto_estacional = S,
        monto_horario_pico = U, monto_sin_led = W, monto_antiguedad = Y,
        costo_ajustado_mensual = AE, costo_anual_bruto = Z, costo_anual_estimado = AF,
        ahorro_potencial_mensual = AH, ahorro_potencial_anual = AJ.
        El benchmark usa umbrales de metricas_final (fuente_umbrales=metricas_final).
        Rollback a Parametros!B10:E28: APP_COSTOS_UMBRALES=parametros.
        """)
public record CostosEstacionalesDTO(

        @Schema(description = "Estacion inferida desde month (hemisferio sur)", example = "invierno",
                nullable = true)
        String estacion,

        @JsonProperty("tarifa_aplicada")
        @Schema(description = "Tarifa usada: la del request o la de referencia del modelo", example = "0.75")
        double tarifaAplicada,

        @JsonProperty("costo_bruto_mensual")
        @Schema(description = "consumo_mensual x tarifa (sin recargos)", example = "240.0")
        double costoBrutoMensual,

        @JsonProperty("pct_estacional")
        @Schema(description = "Recargo por estacion (0..1)", example = "0.1")
        double pctEstacional,

        @JsonProperty("monto_estacional")
        @Schema(example = "24.0")
        double montoEstacional,

        @JsonProperty("pct_horario_pico")
        @Schema(description = "15% si uso_horario_pico = si", example = "0.15")
        double pctHorarioPico,

        @JsonProperty("monto_horario_pico")
        @Schema(example = "36.0")
        double montoHorarioPico,

        @JsonProperty("pct_sin_led")
        @Schema(description = "15% si tiene_iluminacion_led = false", example = "0.0")
        double pctSinLed,

        @JsonProperty("monto_sin_led")
        @Schema(example = "0.0")
        double montoSinLed,

        @JsonProperty("pct_antiguedad")
        @Schema(description = "15% si los electrodomesticos superan los 5 años", example = "0.15")
        double pctAntiguedad,

        @JsonProperty("monto_antiguedad")
        @Schema(example = "36.0")
        double montoAntiguedad,

        @JsonProperty("pct_ajuste_total")
        @Schema(description = "Suma de estacional + accionables", example = "0.4")
        double pctAjusteTotal,

        @JsonProperty("costo_ajustado_mensual")
        @Schema(description = "costo_bruto x (1 + pct_ajuste_total)", example = "336.0")
        double costoAjustadoMensual,

        @JsonProperty("costo_anual_bruto")
        @Schema(description = "costo_bruto x 12", example = "2880.0")
        double costoAnualBruto,

        @JsonProperty("costo_anual_estimado")
        @Schema(description = "Fiel a la planilla: aplica el ajuste de la estacion actual a los 12 meses",
                example = "4032.0")
        double costoAnualEstimado,

        @JsonProperty("costo_anual_estacionalizado")
        @Schema(description = "Suma de la proyeccion por estacion (3 meses cada una); mas realista que "
                + "costo_anual_estimado porque cada trimestre usa su propio recargo",
                example = "3931.32")
        double costoAnualEstacionalizado,

        @JsonProperty("pct_ahorro_potencial")
        @Schema(description = "Suma de los recargos accionables (excluye estacionalidad)", example = "0.3")
        double pctAhorroPotencial,

        @JsonProperty("ahorro_potencial_mensual")
        @Schema(description = "costo_bruto x pct_ahorro_potencial", example = "72.0")
        double ahorroPotencialMensual,

        @JsonProperty("ahorro_potencial_anual")
        @Schema(description = "costo_anual_bruto x pct_ahorro_potencial", example = "864.0")
        double ahorroPotencialAnual,

        @JsonProperty("proyeccion_estacional")
        @ArraySchema(arraySchema = @Schema(description = "Costo mensual estimado en las cuatro estaciones"),
                schema = @Schema(implementation = ProyeccionEstacionalDTO.class))
        List<ProyeccionEstacionalDTO> proyeccionEstacional,

        @Schema(description = "Rango de consumo esperado (referencia informativa)", nullable = true)
        BenchmarkConsumoDTO benchmark,

        @JsonProperty("parametros_version")
        @Schema(description = "Version de parametros_costos.json usada", example = "datasetup-final-final-v1")
        String parametrosVersion,

        @JsonProperty("fuente_umbrales")
        @Schema(description = "Juego de umbrales del benchmark: metricas_final (default, hoja metricas_final) "
                + "o parametros (rollback, hoja Parametros). Switch: APP_COSTOS_UMBRALES.",
                example = "metricas_final",
                allowableValues = {"metricas_final", "parametros"})
        String fuenteUmbrales
) {

    /** Estacion con el costo mensual estimado mas alto de la proyeccion. */
    public ProyeccionEstacionalDTO estacionMasCara() {
        if (proyeccionEstacional == null || proyeccionEstacional.isEmpty()) {
            return null;
        }
        return proyeccionEstacional.stream()
                .max((a, b) -> Double.compare(a.costoMensualEstimado(), b.costoMensualEstimado()))
                .orElse(null);
    }
}
