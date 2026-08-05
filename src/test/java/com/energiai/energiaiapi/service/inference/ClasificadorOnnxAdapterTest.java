package com.energiai.energiaiapi.service.inference;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.onnx.FacturaFeatureEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasificadorOnnxAdapterTest {

    private ClasificadorOnnxAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        ModeloParametros modelo = new ModeloParametros(
                new ObjectMapper(),
                new ClassPathResource("model/modelo_energiai.json"));
        FacturaFeatureEncoder encoder = new FacturaFeatureEncoder(modelo);
        adapter = new ClasificadorOnnxAdapter(
                encoder,
                new ClassPathResource("model/version2.0.onnx"),
                "2.0");
    }

    @AfterEach
    void tearDown() {
        if (adapter != null) {
            adapter.cerrar();
        }
    }

    @Test
    void clasificaFacturaMinimaConOnnxV2() {
        FacturaDTO f = new FacturaDTO(
                350.0, true, 8, "departamento", 6.0,
                "verano", null, null, null, null, null, null);

        ClasificadorPort.Clasificacion c = adapter.clasificar(f);

        assertNotNull(c.categoria());
        assertEquals(3, c.probabilidades().size());
        assertTrue(c.probabilidades().containsKey("Eficiente"));
        assertTrue(c.probabilidades().containsKey("Moderado"));
        assertTrue(c.probabilidades().containsKey("Ineficiente"));

        double suma = c.probabilidades().values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, suma, 1e-3);

        // Misma muestra que en Python: [350,1,8,1,6,1] -> moderado
        assertEquals(CategoriaEficiencia.MODERADO, c.categoria());
    }
}
