package com.energiai.energiaiapi.dto;

import com.energiai.energiaiapi.domain.enums.AntiguedadElectrodomesticos;
import com.energiai.energiaiapi.domain.enums.EstacionAnio;
import com.energiai.energiaiapi.domain.enums.Mes;
import com.energiai.energiaiapi.domain.enums.TipoInmueble;
import com.energiai.energiaiapi.dto.jackson.SiNoBooleanDeserializer;
import com.energiai.energiaiapi.dto.jackson.SiNoBooleanSerializer;
import com.energiai.energiaiapi.dto.jackson.StrictDecimalDeserializer;
import com.energiai.energiaiapi.dto.jackson.StrictIntegerDeserializer;
import com.energiai.energiaiapi.dto.jackson.StrictJsonBooleanDeserializer;
import com.energiai.energiaiapi.dto.jackson.StrictMonthDeserializer;
import com.energiai.energiaiapi.validation.ValorPermitido;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Payload de factura alineado al contrato DS. Deserializacion estricta:
 * sin notacion cientifica, sin alias cortos ({@code n}), sin padding en strings clave.
 * Unica flexibilidad: case-insensitive en {@code tipo_inmueble}.
 */
@Schema(name = "Factura", description = """
        Datos de factura / consulta (snake_case DS).
        Obligatorios: consumo_mensual (int 80–1200), uso_horario_pico (si|no), cantidad_equipos,
        tipo_inmueble, horas_alto_consumo, month.
        Opcionales: numero_personas, tiene_* (boolean JSON), antiguedad_electrodomesticos (exacto),
        tarifa_electrica. estacion_anio es legado (la estacion se infiere desde month).
        """)
public record FacturaDTO(

        @Schema(description = "Consumo mensual kWh entero (negocio; 80 a 1200). No entra al tensor ONNX",
                example = "320", minimum = "80", maximum = "1200",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("consumo_mensual")
        @JsonAlias({"consumoMensual", "consumoKwh"})
        @JsonDeserialize(using = StrictIntegerDeserializer.class)
        @NotNull(message = "consumo_mensual es obligatorio")
        @Min(value = 80, message = "consumo_mensual debe ser al menos 80 kWh")
        @Max(value = 1200, message = "consumo_mensual no puede superar 1200 kWh")
        Integer consumoMensual,

        @Schema(description = "Uso en horario pico (exactamente si|no)", example = "si",
                allowableValues = {"si", "no"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("uso_horario_pico")
        @JsonAlias({"usoHorarioPico"})
        @JsonDeserialize(using = SiNoBooleanDeserializer.class)
        @JsonSerialize(using = SiNoBooleanSerializer.class)
        @NotNull(message = "uso_horario_pico es obligatorio")
        Boolean usoHorarioPico,

        @Schema(description = "Cantidad de equipos (0 a 50), entero JSON", example = "8",
                minimum = "0", maximum = "50", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("cantidad_equipos")
        @JsonAlias({"cantidadEquipos"})
        @JsonDeserialize(using = StrictIntegerDeserializer.class)
        @NotNull(message = "cantidad_equipos es obligatorio")
        @Min(value = 0, message = "cantidad_equipos debe ser al menos 0")
        @Max(value = 50, message = "cantidad_equipos no puede superar 50")
        Integer cantidadEquipos,

        @Schema(description = "Tipo de inmueble (case-insensitive; sin espacios extra)",
                example = "Departamento",
                allowableValues = {"Casa", "Departamento", "Monoambiente",
                        "casa", "departamento", "monoambiente"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("tipo_inmueble")
        @JsonAlias({"tipoInmueble"})
        @NotBlank(message = "tipo_inmueble es obligatorio")
        @Pattern(regexp = "(?i)^(casa|departamento|monoambiente)$",
                message = "tipo_inmueble debe ser Casa, Departamento o Monoambiente (sin espacios)")
        String tipoInmueble,

        @Schema(description = "Horas diarias de alto consumo (0 a 24); sin notacion cientifica",
                example = "6.5",
                minimum = "0", maximum = "24", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("horas_alto_consumo")
        @JsonAlias({"horasPromedioUso", "horasAltoConsumo"})
        @JsonDeserialize(using = StrictDecimalDeserializer.class)
        @NotNull(message = "horas_alto_consumo es obligatorio")
        @DecimalMin(value = "0.0", message = "horas_alto_consumo debe ser al menos 0")
        @DecimalMax(value = "24.0", message = "horas_alto_consumo no puede superar 24")
        Double horasPromedioUso,

        @Schema(description = "Opcional / legado. El negocio infiere la estacion desde month (hemisferio sur). "
                + "Si se envia, debe ser un valor exacto permitido.",
                example = "otoño",
                allowableValues = {"primavera", "verano", "otoño", "otono", "invierno"},
                deprecated = true)
        @JsonProperty("estacion_anio")
        @JsonAlias({"estacionAnio"})
        @ValorPermitido(valores = {"primavera", "verano", "otoño", "otono", "invierno"},
                message = "estacion_anio debe ser: primavera, verano, otoño o invierno")
        String estacionAnio,

        @Schema(description = "Mes del analisis: entero 1-12 o nombre (sin cientifica/decimal)",
                example = "3",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("month")
        @JsonAlias({"mes"})
        @JsonDeserialize(using = StrictMonthDeserializer.class)
        @NotBlank(message = "month es obligatorio")
        @ValorPermitido(
                valores = {
                        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12",
                        "enero", "febrero", "marzo", "abril", "mayo", "junio",
                        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
                },
                message = "month debe ser 1-12 o el nombre del mes")
        String mes,

        @Schema(description = "Personas en el hogar (opcional; null omitido)", example = "4", nullable = true)
        @JsonProperty("numero_personas")
        @JsonAlias({"numeroPersonas"})
        @JsonDeserialize(using = StrictIntegerDeserializer.class)
        @Positive(message = "numero_personas debe ser mayor a 0")
        Integer numeroPersonas,

        @Schema(description = "Booleano JSON true|false (opcional)", example = "true", nullable = true)
        @JsonProperty("tiene_aire_acondicionado")
        @JsonAlias({"tieneAireAcondicionado"})
        @JsonDeserialize(using = StrictJsonBooleanDeserializer.class)
        Boolean tieneAireAcondicionado,

        @Schema(description = "Booleano JSON true|false (opcional)", example = "false", nullable = true)
        @JsonProperty("tiene_calentador")
        @JsonAlias({"tieneCalentador"})
        @JsonDeserialize(using = StrictJsonBooleanDeserializer.class)
        Boolean tieneCalentador,

        @Schema(description = "Booleano JSON true|false (opcional)", example = "true", nullable = true)
        @JsonProperty("tiene_iluminacion_led")
        @JsonAlias({"tieneIluminacionLed"})
        @JsonDeserialize(using = StrictJsonBooleanDeserializer.class)
        Boolean tieneIluminacionLed,

        @Schema(description = "Antiguedad exacta (sin typos; conserva ñ)",
                example = "menor a 5 años",
                allowableValues = {"menor a 3 años", "menor a 5 años", "menor a 10 años", "mayor a 10 años"},
                nullable = true)
        @JsonProperty("antiguedad_electrodomesticos")
        @JsonAlias({"antiguedadElectrodomesticos"})
        @ValorPermitido(
                valores = {"menor a 3 años", "menor a 5 años", "menor a 10 años", "mayor a 10 años"},
                message = "antiguedad_electrodomesticos debe ser exactamente: "
                        + "\"menor a 3 años\", \"menor a 5 años\", \"menor a 10 años\" o \"mayor a 10 años\"")
        String antiguedadElectrodomesticos,

        @Schema(example = "0.75", nullable = true)
        @JsonProperty("tarifa_electrica")
        @JsonAlias({"tarifaElectrica"})
        @JsonDeserialize(using = StrictDecimalDeserializer.class)
        @PositiveOrZero(message = "tarifa_electrica no puede ser negativa")
        Double tarifaElectrica
) {

    public FacturaDTO canonicalizada() {
        String tipo = TipoInmueble.desde(tipoInmueble)
                .map(t -> Character.toUpperCase(t.getValor().charAt(0)) + t.getValor().substring(1))
                .orElse(tipoInmueble);
        String monthCanon = Mes.numeroDesde(mes).map(String::valueOf).orElse(mes);
        // Preferir estacion inferida desde month; el JSON estacion_anio es legado.
        String estacionCanon = EstacionAnio.desdeMes(Mes.numeroDesde(mes).orElse(null))
                .map(EstacionAnio::getValor)
                .or(() -> EstacionAnio.desde(estacionAnio).map(EstacionAnio::getValor))
                .orElse(estacionAnio);
        return new FacturaDTO(
                consumoMensual,
                usoHorarioPico,
                cantidadEquipos,
                tipo,
                horasPromedioUso,
                estacionCanon,
                monthCanon,
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
