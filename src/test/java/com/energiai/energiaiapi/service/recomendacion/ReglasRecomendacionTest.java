package com.energiai.energiaiapi.service.recomendacion;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.energiai.energiaiapi.dto.FacturaDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReglasRecomendacionTest {

    private final ReglasRecomendacion reglas = new ReglasRecomendacion();

    @Test
    void ineficienteSinOpcionalesIncluyeTemaDeCategoria() {
        FacturaDTO f = new FacturaDTO(
                320, false, 5, "Monoambiente", 2.0,
                null, "12", null, null, null, null, null, null);

        List<TemaRecomendacion> temas = reglas.seleccionar(f, CategoriaEficiencia.INEFICIENTE);

        assertEquals(1, temas.size());
        assertEquals("categoria_ineficiente", temas.getFirst().codigo());
        assertTrue(temas.getFirst().textoBase().contains("consumo es alto"));
    }

    @Test
    void picoYLedAgreganTemas() {
        FacturaDTO f = new FacturaDTO(
                320, true, 8, "Departamento", 6.5,
                null, "3", null, null, null, false, null, null);

        List<TemaRecomendacion> temas = reglas.seleccionar(f, CategoriaEficiencia.MODERADO);

        assertTrue(temas.stream().anyMatch(t -> t.codigo().equals("horario_pico")));
        assertTrue(temas.stream().anyMatch(t -> t.codigo().equals("iluminacion_led")));
        assertTrue(temas.stream().anyMatch(t -> t.codigo().equals("categoria_moderado")));
    }
}
