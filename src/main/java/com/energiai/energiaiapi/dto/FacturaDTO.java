package com.energiai.energiaiapi.dto;

import com.energiai.energiaiapi.domain.enums.AntiguedadElectrodomesticos;
import com.energiai.energiaiapi.domain.enums.EstacionAnio;
import com.energiai.energiaiapi.domain.enums.TipoInmueble;
import com.energiai.energiaiapi.validation.ValorPermitido;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Datos de entrada del analisis. Los 5 primeros son obligatorios (tambien para
 * {@code version2.0.onnx}); los 7 restantes son opcionales (pueden venir null).
 * {@code tipoInmueble} es obligatorio tanto en la API como en el vector ONNX
 * (feature indice 3); no se imputa.
 *
 * <p>Valores categoricos validados:
 * <ul>
 *   <li>tipoInmueble (obligatorio): monoambiente | departamento | casa</li>
 *   <li>estacionAnio (opcional): primavera | verano | otoño | invierno</li>
 *   <li>antiguedadElectrodomesticos (opcional): menor a 3/5/10 años | mayor a 10 años</li>
 * </ul>
 */
public record FacturaDTO(

        // ---------- Obligatorios ----------
        @Schema(description = "Consumo mensual de energia en kWh", example = "320.5",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "consumoMensual es obligatorio")
        @Positive(message = "consumoMensual debe ser mayor a 0")
        Double consumoMensual,

        @Schema(description = "Consumo intensivo en horario pico (si/no)", example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "usoHorarioPico es obligatorio")
        Boolean usoHorarioPico,

        @Schema(description = "Cantidad de electrodomesticos", example = "8",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "cantidadEquipos es obligatorio")
        @Positive(message = "cantidadEquipos debe ser mayor a 0")
        Integer cantidadEquipos,

        @Schema(description = "Tipo de inmueble (obligatorio para el modelo ONNX)", example = "casa",
                allowableValues = {"monoambiente", "departamento", "casa"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "tipoInmueble es obligatorio")
        @ValorPermitido(valores = {"monoambiente", "departamento", "casa"},
                message = "tipoInmueble debe ser: monoambiente, departamento o casa")
        String tipoInmueble,

        @Schema(description = "Horas promedio de uso diario (1 a 24)", example = "4.5",
                minimum = "1", maximum = "24", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "horasPromedioUso es obligatorio")
        @DecimalMin(value = "1.0", message = "horasPromedioUso debe ser al menos 1")
        @DecimalMax(value = "24.0", message = "horasPromedioUso no puede superar 24")
        Double horasPromedioUso,

        // ---------- Opcionales ----------
        @Schema(description = "Estacion del año (reemplaza area del inmueble)", example = "verano",
                allowableValues = {"primavera", "verano", "otoño", "invierno"})
        @ValorPermitido(valores = {"primavera", "verano", "otoño", "invierno"},
                message = "estacionAnio debe ser: primavera, verano, otoño o invierno")
        String estacionAnio,

        @Schema(description = "Cantidad de personas en el hogar", example = "4")
        @Positive(message = "numeroPersonas debe ser mayor a 0")
        Integer numeroPersonas,

        @Schema(description = "Tiene aire acondicionado", example = "true")
        Boolean tieneAireAcondicionado,

        @Schema(description = "Tiene calentador electrico", example = "false")
        Boolean tieneCalentador,

        @Schema(description = "Tiene iluminacion LED", example = "true")
        Boolean tieneIluminacionLed,

        @Schema(description = "Antiguedad de electrodomesticos", example = "menor a 5 años",
                allowableValues = {"menor a 3 años", "menor a 5 años", "menor a 10 años", "mayor a 10 años"})
        @ValorPermitido(
                valores = {"menor a 3 años", "menor a 5 años", "menor a 10 años", "mayor a 10 años"},
                message = "antiguedadElectrodomesticos debe ser: menor a 3 años, menor a 5 años, menor a 10 años o mayor a 10 años")
        String antiguedadElectrodomesticos,

        @Schema(description = "Tarifa individual del kWh; si falta se usa la de referencia", example = "0.75")
        @PositiveOrZero(message = "tarifaElectrica no puede ser negativa")
        Double tarifaElectrica
) {

    /** Canonicaliza valores categoricos (minusculas / acentos oficiales) si vienen informados. */
    public FacturaDTO canonicalizada() {
        return new FacturaDTO(
                consumoMensual,
                usoHorarioPico,
                cantidadEquipos,
                TipoInmueble.desde(tipoInmueble).map(TipoInmueble::getValor).orElse(tipoInmueble),
                horasPromedioUso,
                EstacionAnio.desde(estacionAnio).map(EstacionAnio::getValor).orElse(estacionAnio),
                numeroPersonas,
                tieneAireAcondicionado,
                tieneCalentador,
                tieneIluminacionLed,
                AntiguedadElectrodomesticos.desde(antiguedadElectrodomesticos)
                        .map(AntiguedadElectrodomesticos::getValor)
                        .orElse(antiguedadElectrodomesticos),
                tarifaElectrica
        );
    }
}
