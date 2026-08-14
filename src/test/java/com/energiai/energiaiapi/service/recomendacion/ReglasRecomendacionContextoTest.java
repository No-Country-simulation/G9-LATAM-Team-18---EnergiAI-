package com.energiai.energiaiapi.service.recomendacion;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.energiai.energiaiapi.dto.CostosEstacionalesDTO;
import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.dto.ResumenHistorialDTO;
import com.energiai.energiaiapi.service.costos.CalculadoraCostosEstacionales;
import com.energiai.energiaiapi.service.costos.ParametrosCosto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los temas contextuales solo aparecen para el usuario registrado y el primero encabeza la lista
 * para que sobreviva al recorte por max-items.
 */
class ReglasRecomendacionContextoTest {

    private final ReglasRecomendacion reglas = new ReglasRecomendacion();
    private final CalculadoraCostosEstacionales calculadora = new CalculadoraCostosEstacionales(parametros());

    @Test
    void modoInvitadoNoAgregaTemasContextuales() {
        List<TemaRecomendacion> temas = reglas.seleccionar(
                factura(), CategoriaEficiencia.MODERADO, ContextoRecomendacion.vacio());

        assertTrue(temas.stream().noneMatch(t -> t.codigo().equals("ahorro_potencial")));
        assertTrue(temas.stream().noneMatch(t -> t.codigo().equals("tendencia_historial")));
        assertEquals("horario_pico", temas.getFirst().codigo());
    }

    @Test
    void conCostosAgregaAhorroPotencialConCifras() {
        CostosEstacionalesDTO costos = calculadora.calcular(factura(), 240.0, 0.75);

        List<TemaRecomendacion> temas = reglas.seleccionar(
                factura(), CategoriaEficiencia.MODERADO, new ContextoRecomendacion(costos, null));

        TemaRecomendacion ahorro = temas.stream()
                .filter(t -> t.codigo().equals("ahorro_potencial"))
                .findFirst()
                .orElseThrow();
        assertTrue(ahorro.textoBase().contains("USD 36.00"));
        assertTrue(ahorro.textoBase().contains("USD 432.00"));
    }

    @Test
    void laVariacionDelHistorialEncabezaLaLista() {
        ResumenHistorialDTO historial = new ResumenHistorialDTO(
                4, 300.0, 225.0, 0.0833, 0.0833, "al_alza",
                null, CategoriaEficiencia.MODERADO, 310.0,
                0, null, null, "invierno");

        List<TemaRecomendacion> temas = reglas.seleccionar(
                factura(), CategoriaEficiencia.MODERADO, new ContextoRecomendacion(null, historial));

        assertEquals("tendencia_historial", temas.getFirst().codigo());
        assertTrue(temas.getFirst().textoBase().contains("+8.3%"));
        assertTrue(temas.getFirst().textoBase().contains("300 kWh"));
    }

    @Test
    void variacionDespreciableNoGeneraTemaDeHistorial() {
        ResumenHistorialDTO historial = new ResumenHistorialDTO(
                3, 300.0, 225.0, 0.01, 0.01, "estable",
                null, CategoriaEficiencia.MODERADO, 303.0,
                2, 300.0, 0.01, "invierno");

        List<TemaRecomendacion> temas = reglas.seleccionar(
                factura(), CategoriaEficiencia.MODERADO, new ContextoRecomendacion(null, historial));

        assertFalse(temas.stream().anyMatch(t -> t.codigo().equals("tendencia_historial")));
        assertFalse(temas.stream().anyMatch(t -> t.codigo().equals("comparativa_misma_estacion")));
    }

    @Test
    void enOtonoAnticipaElCostoDeInvierno() {
        FacturaDTO otono = new FacturaDTO(
                320, true, 8, "Departamento", 6.5,
                null, "4", null, null, null, true, "menor a 5 años", 0.75);
        CostosEstacionalesDTO costos = calculadora.calcular(otono, 240.0, 0.75);

        List<TemaRecomendacion> temas = reglas.seleccionar(
                otono, CategoriaEficiencia.MODERADO, new ContextoRecomendacion(costos, null));

        assertTrue(temas.stream().anyMatch(t -> t.codigo().equals("anticipo_estacional")
                && t.textoBase().contains("invierno")));
    }

    private static FacturaDTO factura() {
        return new FacturaDTO(
                320, true, 8, "Departamento", 6.5,
                null, "7", null, null, null, true, "menor a 5 años", 0.75);
    }

    private static ParametrosCosto parametros() {
        try {
            return new ParametrosCosto(new ObjectMapper(),
                    new ClassPathResource("model/parametros_costos.json"));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar parametros_costos.json", e);
        }
    }
}
