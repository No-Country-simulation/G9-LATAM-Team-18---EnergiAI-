package com.energiai.energiaiapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Eco de la consulta al modelo en la sintaxis DS (snake_case / si-no).
 */
@Schema(description = "Parametros enviados al preproceso ONNX (contrato DS)")
public record ConsultaModeloDTO(
        @JsonProperty("tipo_inmueble")
        @Schema(example = "Departamento")
        String tipoInmueble,

        @JsonProperty("month")
        @Schema(example = "3")
        int month,

        @JsonProperty("uso_horario_pico")
        @Schema(example = "si", allowableValues = {"si", "no"})
        String usoHorarioPico,

        @JsonProperty("horas_alto_consumo")
        @Schema(example = "6.5")
        double horasAltoConsumo,

        @JsonProperty("cantidad_equipos")
        @Schema(example = "8")
        int cantidadEquipos
) {
}
