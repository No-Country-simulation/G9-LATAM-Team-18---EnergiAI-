package com.energiai.energiaiapi.service.inference;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.onnx.XgboostFeatureEncoder;
import com.energiai.energiaiapi.onnx.XgboostMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasificadorOnnxAdapterTest {

    private ClasificadorOnnxAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        XgboostMetadata metadata = new XgboostMetadata(
                new ObjectMapper(),
                new ClassPathResource("model/metadata_backend.json"));
        XgboostFeatureEncoder encoder = new XgboostFeatureEncoder(metadata);
        adapter = new ClasificadorOnnxAdapter(
                encoder,
                new ClassPathResource("model/modelo_xgboost_v2.onnx"),
                "xgboost-v2");
    }

    @AfterEach
    void tearDown() {
        if (adapter != null) {
            adapter.cerrar();
        }
    }

    @Test
    void clasificaFacturaEjemploDsConXgboostV2() {
        // Muestra documentada: Departamento, mes 3, pico si, 6.5h, 8 equipos
        // v2: Ineficiente (label 2) con alta probabilidad (~0.97)
        FacturaDTO f = new FacturaDTO(
                320, true, 8, "departamento", 6.5,
                null, "3", null, null, null, null, null, null);

        ClasificadorPort.Clasificacion c = adapter.clasificar(f);

        assertNotNull(c.categoria());
        assertEquals(3, c.probabilidades().size());
        assertTrue(c.probabilidades().containsKey("Eficiente"));
        assertTrue(c.probabilidades().containsKey("Moderado"));
        assertTrue(c.probabilidades().containsKey("Ineficiente"));

        double suma = c.probabilidades().values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, suma, 1e-3);

        String ganadora = c.probabilidades().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow()
                .getKey();
        assertEquals(CategoriaEficiencia.desdeEtiqueta(ganadora), c.categoria());
        assertEquals(CategoriaEficiencia.INEFICIENTE, c.categoria());
        assertTrue(c.probabilidades().get("Ineficiente") > 0.9);
        assertEquals("xgboost-v2", adapter.getVersion());
    }
}
