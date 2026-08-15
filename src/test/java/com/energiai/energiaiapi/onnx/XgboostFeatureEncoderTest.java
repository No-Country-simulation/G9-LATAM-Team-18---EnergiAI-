package com.energiai.energiaiapi.onnx;

import com.energiai.energiaiapi.dto.FacturaDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XgboostFeatureEncoderTest {

    /**
     * Vector DS alineado a metadata_backend.json:
     * pico one-hot [no, si]; numericos horas luego equipos.
     */
    private static final float[] VECTOR_REF_SI = {
            0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f,
            6.5f, 8.0f, 0.81249f, 6.5f, -2.83751f
    };

    private static final float[] VECTOR_REF_NO = {
            0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            1.0f, 0.0f,
            6.5f, 8.0f, 0.81249f, 0.0f, -2.83751f
    };

    private XgboostFeatureEncoder encoder;

    @BeforeEach
    void setUp() throws Exception {
        XgboostMetadata metadata = new XgboostMetadata(
                new ObjectMapper(),
                new ClassPathResource("model/metadata_backend.json"));
        encoder = new XgboostFeatureEncoder(metadata);
    }

    @Test
    void codificaVectorPicoSiHorasAntesQueEquipos() {
        FacturaDTO f = new FacturaDTO(
                320, true, 8, "Departamento", 6.5,
                null, "3", null, null, null, null, null, null);

        XgboostFeatures features = encoder.encodeFull(f);
        float[] v = features.vector();

        assertEquals(22, v.length);
        assertEquals("si", features.usoHorarioPicoDs());
        assertEquals(6.5, features.horasAltoConsumo(), 1e-9);
        assertEquals(8, features.cantidadEquipos());
        // pos 15-16: [no, si] → si ⇒ [0, 1]
        assertEquals(0.0f, v[15], 1e-6f);
        assertEquals(1.0f, v[16], 1e-6f);
        // pos 17-18: horas, equipos
        assertEquals(6.5f, v[17], 1e-6f);
        assertEquals(8.0f, v[18], 1e-6f);
        assertArrayEquals(VECTOR_REF_SI, v, 1e-4f);
    }

    @Test
    void codificaVectorPicoNoComoUnoCero() {
        FacturaDTO f = new FacturaDTO(
                320, false, 8, "Departamento", 6.5,
                null, "3", null, null, null, null, null, null);

        float[] v = encoder.encode(f);
        // pos 15-16: [no, si] → no ⇒ [1, 0]
        assertEquals(1.0f, v[15], 1e-6f);
        assertEquals(0.0f, v[16], 1e-6f);
        assertEquals(6.5f, v[17], 1e-6f);
        assertEquals(8.0f, v[18], 1e-6f);
        assertEquals(0.0f, v[20], 1e-6f); // horas_pico_interaccion
        assertArrayEquals(VECTOR_REF_NO, v, 1e-4f);
    }

    @Test
    void rechazaSinMonth() {
        FacturaDTO f = new FacturaDTO(
                320, true, 8, "Casa", 6.5,
                null, null, null, null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> encoder.encode(f));
    }
}
