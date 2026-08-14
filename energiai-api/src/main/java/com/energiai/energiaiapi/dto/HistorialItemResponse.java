package com.energiai.energiaiapi.dto;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Item del historial de analisis de un usuario autenticado.
 */
@Schema(name = "HistorialItem", description = "Analisis persistido del usuario")
public record HistorialItemResponse(

        @Schema(example = "42")
        Long id,

        Instant creadoEn,

        @Schema(example = "MODERADO")
        CategoriaEficiencia categoria,

        @Schema(description = "Probabilidad de la categoria ganadora", example = "0.61")
        double probabilidad,

        @Schema(example = "240.375")
        double costoEstimadoMensual,

        @Schema(nullable = true, example = "80.125")
        Double indiceEficiencia,

        @Schema(nullable = true, example = "320")
        Double consumoMensual,

        @Schema(nullable = true, example = "Departamento")
        String tipoInmueble,

        @Schema(nullable = true, example = "3")
        String mes,

        @ArraySchema(schema = @Schema(example = "Evita equipos de alto consumo en horario pico."))
        List<String> recomendaciones,

        @JsonProperty("costos")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "Desglose estacional congelado; null en analisis previos a la V5",
                nullable = true)
        CostosGuardadosDTO costos
) {
}
