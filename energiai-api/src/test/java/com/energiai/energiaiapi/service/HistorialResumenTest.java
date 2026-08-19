package com.energiai.energiaiapi.service;

import com.energiai.energiaiapi.domain.Analisis;
import com.energiai.energiaiapi.domain.Factura;
import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.energiai.energiaiapi.domain.enums.EstacionAnio;
import com.energiai.energiaiapi.dto.ResumenHistorialDTO;
import com.energiai.energiaiapi.repository.AnalisisRepository;
import com.energiai.energiaiapi.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HistorialResumenTest {

    private static final double DELTA = 1e-6;

    @Test
    void sinAnalisisPreviosNoHayReferencia() {
        HistorialService svc = servicioCon(List.of());

        ResumenHistorialDTO r = svc.resumenPara(1L, 320.0, 240.0, EstacionAnio.INVIERNO);

        assertFalse(r.tienePrevios());
        assertEquals("sin_referencia", r.tendencia());
        assertNull(r.variacionConsumoPct());
        assertEquals("invierno", r.estacion());
    }

    @Test
    void promediaConsumoYCostoDeLosAnalisisPrevios() {
        HistorialService svc = servicioCon(List.of(
                analisis(300.0, 225.0, "7", CategoriaEficiencia.MODERADO),
                analisis(200.0, 150.0, "4", CategoriaEficiencia.EFICIENTE)));

        ResumenHistorialDTO r = svc.resumenPara(1L, 300.0, 225.0, EstacionAnio.INVIERNO);

        assertEquals(2, r.analisisPrevios());
        assertEquals(250.0, r.consumoPromedioKwh(), DELTA);
        assertEquals(187.5, r.costoPromedioMensual(), DELTA);
        assertEquals(0.2, r.variacionConsumoPct(), DELTA);
        assertEquals("al_alza", r.tendencia());
        assertEquals(CategoriaEficiencia.MODERADO, r.ultimaCategoria());
    }

    @Test
    void comparaContraLaMismaEstacionCuandoHayRegistros() {
        HistorialService svc = servicioCon(List.of(
                analisis(400.0, 300.0, "7", CategoriaEficiencia.INEFICIENTE),   // invierno
                analisis(300.0, 225.0, "8", CategoriaEficiencia.MODERADO),      // invierno
                analisis(200.0, 150.0, "4", CategoriaEficiencia.EFICIENTE)));   // otoño

        ResumenHistorialDTO r = svc.resumenPara(1L, 350.0, 262.5, EstacionAnio.INVIERNO);

        assertEquals(2, r.analisisMismaEstacion());
        assertEquals(350.0, r.consumoPromedioMismaEstacionKwh(), DELTA);
        assertEquals(0.0, r.variacionVsMismaEstacionPct(), DELTA);
    }

    @Test
    void variacionMenorAlCincoPorCientoSeInformaComoEstable() {
        HistorialService svc = servicioCon(List.of(analisis(300.0, 225.0, "7", CategoriaEficiencia.MODERADO)));

        ResumenHistorialDTO r = svc.resumenPara(1L, 306.0, 229.5, EstacionAnio.INVIERNO);

        assertEquals("estable", r.tendencia());
        assertTrue(r.variacionConsumoPct() < 0.05);
    }

    private static HistorialService servicioCon(List<Analisis> previos) {
        AnalisisRepository analisisRepository = mock(AnalisisRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        when(analisisRepository.findByUsuarioIdOrderByCreadoEnDesc(anyLong())).thenReturn(previos);
        return new HistorialService(analisisRepository, usuarioRepository);
    }

    private static Analisis analisis(double consumo, double costo, String mes, CategoriaEficiencia categoria) {
        Factura factura = new Factura();
        factura.setConsumoMensual(consumo);
        factura.setMes(mes);

        Analisis a = new Analisis();
        a.setFactura(factura);
        a.setCostoEstimadoMensual(costo);
        a.setCategoria(categoria);
        return a;
    }
}
