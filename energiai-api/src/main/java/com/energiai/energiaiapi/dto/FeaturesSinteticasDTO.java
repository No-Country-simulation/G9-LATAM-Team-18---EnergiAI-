package com.energiai.energiaiapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Variables sinteticas calculadas en backend (formulas A/B/C del contrato DS).
 */
@Schema(description = "Features sinteticas calculadas antes de ONNX")
public record FeaturesSinteticasDTO(
        @JsonProperty("intensidad_por_equipo")
        double intensidadPorEquipo,

        @JsonProperty("horas_pico_interaccion")
        double horasPicoInteraccion,

        @JsonProperty("desviacion_equipos_tipo")
        double desviacionEquiposTipo
) {
}
