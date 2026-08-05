package com.energiai.energiaiapi.onnx;

import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.service.inference.ModeloParametros;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FacturaFeatureEncoderTest {

    private FacturaFeatureEncoder encoder;

    @BeforeEach
    void setUp() throws Exception {
        ModeloParametros modelo = new ModeloParametros(
                new ObjectMapper(),
                new ClassPathResource("model/modelo_energiai.json"));
        encoder = new FacturaFeatureEncoder(modelo);
    }

    @Test
    void codificaSeisFeaturesEnOrdenEsperado() {
        FacturaDTO f = new FacturaDTO(
                350.0, true, 8, "departamento", 6.0,
                "verano", null, null, null, null, null, null);

        float[] v = encoder.encode(f);

        assertEquals(6, v.length);
        assertEquals(350f, v[0], 1e-5);
        assertEquals(1f, v[1], 1e-5);   // pico
        assertEquals(8f, v[2], 1e-5);
        assertEquals(1f, v[3], 1e-5);   // departamento
        assertEquals(6f, v[4], 1e-5);
        assertEquals(1f, v[5], 1e-5);   // verano
    }

    @Test
    void rechazaTipoInmuebleAusentePorqueEsObligatorioParaElModelo() {
        FacturaDTO f = new FacturaDTO(
                350.0, true, 8, null, 6.0,
                "verano", null, null, null, null, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> encoder.encode(f));
        assertEquals(true, ex.getMessage().contains("tipoInmueble"));
    }
}
