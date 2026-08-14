package com.energiai.energiaiapi.service;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.service.recomendacion.GeminiRecomendacionClient;
import com.energiai.energiaiapi.service.recomendacion.ReglasRecomendacion;
import com.energiai.energiaiapi.service.recomendacion.TemaRecomendacion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecomendacionServiceTest {

    @Test
    void modoReglasDevuelveTextosBaseSinLlamarGemini() {
        ReglasRecomendacion reglas = new ReglasRecomendacion();
        GeminiRecomendacionClient gemini = mock(GeminiRecomendacionClient.class);
        RecomendacionService svc = new RecomendacionService(reglas, gemini, "reglas", 3);

        FacturaDTO f = facturaIneficiente();
        List<String> out = svc.generar(f, CategoriaEficiencia.INEFICIENTE);

        assertEquals(1, out.size());
        assertTrue(out.getFirst().contains("consumo es alto"));
    }

    @Test
    void modoHibridoUsaGeminiCuandoResponde() {
        ReglasRecomendacion reglas = new ReglasRecomendacion();
        GeminiRecomendacionClient gemini = mock(GeminiRecomendacionClient.class);
        when(gemini.isEnabled()).thenReturn(true);
        when(gemini.reformular(any(), any(), any(), anyInt(), any()))
                .thenReturn(Optional.of(List.of("Tip dinamico 1", "Tip dinamico 2")));

        RecomendacionService svc = new RecomendacionService(reglas, gemini, "hibrido", 3);
        List<String> out = svc.generar(facturaIneficiente(), CategoriaEficiencia.INEFICIENTE);

        assertEquals(List.of("Tip dinamico 1", "Tip dinamico 2"), out);
    }

    @Test
    void modoHibridoHaceFallbackSiGeminiFalla() {
        ReglasRecomendacion reglas = new ReglasRecomendacion();
        GeminiRecomendacionClient gemini = mock(GeminiRecomendacionClient.class);
        when(gemini.isEnabled()).thenReturn(true);
        when(gemini.reformular(any(), any(), any(), anyInt(), any())).thenReturn(Optional.empty());

        RecomendacionService svc = new RecomendacionService(reglas, gemini, "hibrido", 3);
        List<String> out = svc.generar(facturaIneficiente(), CategoriaEficiencia.INEFICIENTE);

        assertEquals(1, out.size());
        assertTrue(out.getFirst().contains("consumo es alto"));
    }

    @Test
    void clienteSinApiKeyQuedaDeshabilitado() {
        GeminiRecomendacionClient client = new GeminiRecomendacionClient(
                new ObjectMapper(), "", "gemini-flash-lite-latest", 1000);
        assertEquals(false, client.isEnabled());
        assertEquals(Optional.empty(), client.reformular(
                facturaIneficiente(),
                CategoriaEficiencia.INEFICIENTE,
                List.of(new TemaRecomendacion("x", "base")),
                3));
    }

    private static FacturaDTO facturaIneficiente() {
        return new FacturaDTO(
                320, false, 5, "Monoambiente", 2.0,
                null, "12", null, null, null, null, null, null);
    }
}
