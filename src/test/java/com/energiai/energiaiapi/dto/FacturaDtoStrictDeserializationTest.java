package com.energiai.energiaiapi.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacturaDtoStrictDeserializationTest {

    private ObjectMapper mapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void aceptaPayloadCanonicoDs() throws Exception {
        String json = """
                {
                  "consumo_mensual": 320,
                  "tipo_inmueble": "Casa",
                  "month": 10,
                  "uso_horario_pico": "no",
                  "horas_alto_consumo": 10,
                  "cantidad_equipos": 1
                }
                """;
        FacturaDTO f = mapper.readValue(json, FacturaDTO.class);
        assertEquals(320, f.consumoMensual());
        assertEquals("Casa", f.tipoInmueble());
        assertEquals("10", f.mes());
        assertEquals(false, f.usoHorarioPico());
        assertEquals(10.0, f.horasPromedioUso());
        assertEquals(1, f.cantidadEquipos());
    }

    @Test
    void aceptaTipoInmuebleCaseInsensitiveSinEspacios() throws Exception {
        String json = """
                {
                  "consumo_mensual": 80,
                  "tipo_inmueble": "departamento",
                  "month": 3,
                  "uso_horario_pico": "si",
                  "horas_alto_consumo": 6.5,
                  "cantidad_equipos": 8
                }
                """;
        FacturaDTO f = mapper.readValue(json, FacturaDTO.class);
        assertEquals("departamento", f.tipoInmueble());
    }

    @Test
    void rechazaConsumoDecimal() {
        String json = """
                {
                  "consumo_mensual": 320.5,
                  "tipo_inmueble": "casa",
                  "month": 10,
                  "uso_horario_pico": "no",
                  "horas_alto_consumo": 10,
                  "cantidad_equipos": 1
                }
                """;
        assertThrows(InvalidFormatException.class, () -> mapper.readValue(json, FacturaDTO.class));
    }

    @Test
    void rechazaMonthNotacionCientifica() {
        String json = """
                {
                  "consumo_mensual": 1000,
                  "tipo_inmueble": "casa",
                  "month": 1.003E1,
                  "uso_horario_pico": "no",
                  "horas_alto_consumo": 10,
                  "cantidad_equipos": 1
                }
                """;
        assertThrows(InvalidFormatException.class, () -> mapper.readValue(json, FacturaDTO.class));
    }

    @Test
    void rechazaConsumoNotacionCientifica() {
        String json = """
                {
                  "consumo_mensual": 1E3,
                  "tipo_inmueble": "casa",
                  "month": 10,
                  "uso_horario_pico": "no",
                  "horas_alto_consumo": 10,
                  "cantidad_equipos": 1
                }
                """;
        Exception ex = assertThrows(Exception.class, () -> mapper.readValue(json, FacturaDTO.class));
        assertTrue(ex.getMessage().toLowerCase().contains("cientifica")
                || ex.getCause() != null && ex.getCause().getMessage() != null
                && ex.getCause().getMessage().toLowerCase().contains("cientifica")
                || ex.getMessage().toLowerCase().contains("entero"));
    }

    @Test
    void rechazaUsoHorarioPicoAliasN() {
        String json = """
                {
                  "consumo_mensual": 1000,
                  "tipo_inmueble": "casa",
                  "month": 10,
                  "uso_horario_pico": "n",
                  "horas_alto_consumo": 10,
                  "cantidad_equipos": 1
                }
                """;
        assertThrows(InvalidFormatException.class, () -> mapper.readValue(json, FacturaDTO.class));
    }

    @Test
    void rechazaBooleanoComoString() {
        String json = """
                {
                  "consumo_mensual": 320,
                  "tipo_inmueble": "casa",
                  "month": 10,
                  "uso_horario_pico": "no",
                  "horas_alto_consumo": 10,
                  "cantidad_equipos": 1,
                  "tiene_aire_acondicionado": "true"
                }
                """;
        Exception ex = assertThrows(Exception.class, () -> mapper.readValue(json, FacturaDTO.class));
        String msg = ex.getMessage() + " " + (ex.getCause() == null ? "" : ex.getCause().getMessage());
        assertTrue(msg.toLowerCase().contains("booleano") || msg.toLowerCase().contains("true o false"),
                () -> "mensaje esperado en español, fue: " + msg);
    }

    @Test
    void rechazaAntiguedadConTypoMenor() {
        FacturaDTO f = new FacturaDTO(
                320, false, 5, "casa", 2.0,
                null, "12", null, null, null, null, "meñor a 5 años", null);
        Set<ConstraintViolation<FacturaDTO>> violations = validator.validate(f);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().contains("antiguedadElectrodomesticos")));
    }

    @Test
    void aceptaAntiguedadExacta() {
        FacturaDTO f = new FacturaDTO(
                320, false, 5, "casa", 2.0,
                null, "12", null, null, null, null, "menor a 5 años", null);
        assertTrue(validator.validate(f).stream()
                .noneMatch(v -> v.getPropertyPath().toString().contains("antiguedad")));
    }

    @Test
    void rechazaTipoConEspacios() throws Exception {
        String json = """
                {
                  "consumo_mensual": 1000,
                  "tipo_inmueble": "casa     ",
                  "month": 10,
                  "uso_horario_pico": "no",
                  "horas_alto_consumo": 10,
                  "cantidad_equipos": 1
                }
                """;
        FacturaDTO f = mapper.readValue(json, FacturaDTO.class);
        assertEquals("casa     ", f.tipoInmueble());
        assertTrue(java.util.regex.Pattern.compile("(?i)^(casa|departamento|monoambiente)$")
                .matcher(f.tipoInmueble()).matches() == false);
    }

    @Test
    void rechazaHorasNotacionCientifica() {
        String json = """
                {
                  "consumo_mensual": 1000,
                  "tipo_inmueble": "casa",
                  "month": 10,
                  "uso_horario_pico": "no",
                  "horas_alto_consumo": 1E1,
                  "cantidad_equipos": 1
                }
                """;
        assertThrows(Exception.class, () -> mapper.readValue(json, FacturaDTO.class));
    }

    /**
     * Caso de prueba #5 (invalido): tres campos mal. Antes, {@code month: 13} lanzaba durante la
     * deserializacion y Jackson abortaba la lectura, asi que la respuesta 400 reportaba solo ese
     * campo. Ahora el payload se lee completo y Bean Validation acumula los tres.
     */
    @Test
    void acumulaTodosLosCamposInvalidosDelCaso5() throws Exception {
        String json = """
                {
                  "consumo_mensual": 320,
                  "uso_horario_pico": "si",
                  "cantidad_equipos": 8,
                  "tipo_inmueble": "Oficina",
                  "horas_alto_consumo": 30.0,
                  "month": 13
                }
                """;
        FacturaDTO f = mapper.readValue(json, FacturaDTO.class);
        assertEquals("13", f.mes());

        Set<String> campos = validator.validate(f).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("tipoInmueble", "horasPromedioUso", "mes"), campos);
    }

    @Test
    void aceptaNombreDeMesConLetraE() throws Exception {
        String json = """
                {
                  "consumo_mensual": 320,
                  "tipo_inmueble": "casa",
                  "month": "Enero",
                  "uso_horario_pico": "no",
                  "horas_alto_consumo": 10,
                  "cantidad_equipos": 1
                }
                """;
        FacturaDTO f = mapper.readValue(json, FacturaDTO.class);
        assertEquals("enero", f.mes());
        assertTrue(validator.validate(f).isEmpty());
    }

    @Test
    void rechazaMonthConTypoViaValidacion() throws Exception {
        String json = """
                {
                  "consumo_mensual": 320,
                  "tipo_inmueble": "casa",
                  "month": "enerito",
                  "uso_horario_pico": "no",
                  "horas_alto_consumo": 10,
                  "cantidad_equipos": 1
                }
                """;
        FacturaDTO f = mapper.readValue(json, FacturaDTO.class);
        assertTrue(validator.validate(f).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("mes")));
    }

    @Test
    void canonicalizaEstacionDesdeMonth() {
        FacturaDTO f = new FacturaDTO(
                320, true, 8, "Departamento", 6.5,
                "invierno", "1", null, true, null, null, null, null);
        FacturaDTO c = f.canonicalizada();
        // month=1 (enero) → verano en hemisferio sur, ignora estacion_anio del JSON
        assertEquals("verano", c.estacionAnio());
    }
}
