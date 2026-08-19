package com.energiai.energiaiapi.dto;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El contrato del modo invitado no debe cambiar: sin JWT, los bloques de costos e historial
 * se omiten del JSON en lugar de viajar como null.
 */
class AnalisisResponseSerializacionTest {

    /** Equivalente al mapper autoconfigurado de Spring Boot en lo que importa aqui. */
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void modoInvitadoOmiteCostosEHistorial() throws Exception {
        JsonNode json = mapper.readTree(mapper.writeValueAsString(respuesta(null, null)));

        assertFalse(json.has("costos"));
        assertFalse(json.has("historial_resumen"));
        // Los campos nullable preexistentes siguen presentes para no romper al frontend.
        assertTrue(json.has("indiceEficiencia"));
        assertTrue(json.has("analisisId"));
    }

    @Test
    void modoHistorialIncluyeAmbosBloques() throws Exception {
        CostosEstacionalesDTO costos = new CostosEstacionalesDTO(
                "invierno", 0.75, 240.0, 0.1, 24.0, 0.15, 36.0, 0.0, 0.0, 0.0, 0.0,
                0.25, 300.0, 2880.0, 3600.0, 3565.8, 0.15, 36.0, 432.0,
                List.of(new ProyeccionEstacionalDTO("invierno", 0.1, 300.0, true)),
                null, "datasetup-final-final-v1", "metricas_final");

        JsonNode json = mapper.readTree(mapper.writeValueAsString(
                respuesta(costos, ResumenHistorialDTO.sinPrevios("invierno"))));

        assertTrue(json.has("costos"));
        assertTrue(json.get("costos").has("costo_ajustado_mensual"));
        assertTrue(json.get("costos").has("proyeccion_estacional"));
        assertTrue(json.has("historial_resumen"));
        assertTrue(json.get("historial_resumen").has("analisis_previos"));
    }

    private static AnalisisResponse respuesta(CostosEstacionalesDTO costos, ResumenHistorialDTO historial) {
        return new AnalisisResponse(
                CategoriaEficiencia.MODERADO,
                Map.of("Moderado", 0.6),
                240.0,
                null,
                List.of("Evita equipos de alto consumo en horario pico."),
                "xgboost-v2",
                "BACKEND_ONNX",
                false,
                null,
                Instant.parse("2026-08-13T18:00:00Z"),
                new ConsultaModeloDTO("Departamento", 7, "si", 6.5, 8),
                new FeaturesSinteticasDTO(40.0, 6.5, 0.0),
                new float[]{1.0f},
                costos,
                historial);
    }
}
