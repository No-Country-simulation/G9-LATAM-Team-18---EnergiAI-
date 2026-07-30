package com.energiai.energiaiapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Datos de entrada del analisis. Los 5 primeros son obligatorios (validados aca en
 * el backend, ademas del frontend); los 7 restantes son opcionales y pueden venir null
 * (el pipeline del modelo los imputa con la media del entrenamiento - scaler_mean).
 */
public record FacturaDTO(

        // ---------- Obligatorios ----------
        @Schema(description = "Consumo mensual de energia en kWh", example = "320")
        @NotNull(message = "consumo_kwh es obligatorio")
        @Positive(message = "consumo_kwh debe ser mayor a 0")
        Integer consumoKwh,

        @Schema(description = "Uso intensivo en horario pico (si/no)", example = "true")
        @NotNull(message = "uso_horario_pico es obligatorio")
        Boolean usoHorarioPico,

        @Schema(description = "Cantidad de aparatos electricos", example = "8")
        @NotNull(message = "cantidad_equipos es obligatorio")
        @Positive(message = "cantidad_equipos debe ser mayor a 0")
        Integer cantidadEquipos,

        @Schema(description = "Tipo de inmueble: Casa, Departamento o Local", example = "Casa")
        @NotBlank(message = "tipo_inmueble es obligatorio")
        String tipoInmueble,

        @Schema(description = "Horas diarias de uso intensivo", example = "4.5")
        @NotNull(message = "horas_alto_consumo es obligatorio")
        @PositiveOrZero(message = "horas_alto_consumo no puede ser negativo")
        Double horasAltoConsumo,

        // ---------- Opcionales ----------
        @Schema(description = "Area del inmueble en m2", example = "85.0")
        @PositiveOrZero
        Double areaInmueble,

        @Schema(description = "Cantidad de personas en el hogar", example = "4")
        @Positive
        Integer numeroPersonas,

        @Schema(description = "Tiene aire acondicionado", example = "true")
        Boolean tieneAireAcondicionado,

        @Schema(description = "Tiene calentador electrico", example = "false")
        Boolean tieneCalentadorElectrico,

        @Schema(description = "Tiene iluminacion LED", example = "true")
        Boolean tieneIluminacionLed,

        @Schema(description = "Antiguedad de electrodomesticos: Nueva, Regular o Antigua", example = "Regular")
        String antiguedadElectrodomesticos,

        @Schema(description = "Tarifa individual del kWh; si falta se usa la tarifa de referencia", example = "0.75")
        @PositiveOrZero
        Double tarifaElectrica
) {
}
