package com.energiai.energiaiapi.service;

import com.energiai.energiaiapi.domain.Usuario;
import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.energiai.energiaiapi.domain.enums.EstacionAnio;
import com.energiai.energiaiapi.dto.AnalisisRequest;
import com.energiai.energiaiapi.dto.AnalisisResponse;
import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.dto.ResumenHistorialDTO;
import com.energiai.energiaiapi.onnx.XgboostFeatureEncoder;
import com.energiai.energiaiapi.onnx.XgboostFeatures;
import com.energiai.energiaiapi.repository.AnalisisRepository;
import com.energiai.energiaiapi.repository.UsuarioRepository;
import com.energiai.energiaiapi.service.costos.CalculadoraCostosEstacionales;
import com.energiai.energiaiapi.service.costos.ParametrosCosto;
import com.energiai.energiaiapi.service.inference.ClasificadorPort;
import com.energiai.energiaiapi.service.inference.ModeloParametros;
import com.energiai.energiaiapi.service.recomendacion.ContextoRecomendacion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El desglose estacional y la comparativa de historial son exclusivos del usuario autenticado.
 * El invitado conserva el contrato anterior: costo = consumo x tarifa y nada mas.
 */
class AnalisisServiceGatingTest {

    private static final double DELTA = 1e-6;

    private final RecomendacionService recomendacionService = mock(RecomendacionService.class);
    private final HistorialService historialService = mock(HistorialService.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final AnalisisRepository analisisRepository = mock(AnalisisRepository.class);

    @Test
    void invitadoNoRecibeCostosNiHistorial() {
        AnalisisService svc = servicio();

        AnalisisResponse r = svc.analizar(new AnalisisRequest(factura(), null, false), null);

        assertEquals(240.0, r.costoEstimadoMensual(), DELTA);
        assertNull(r.costos());
        assertNull(r.historialResumen());
        verify(historialService, never()).resumenPara(anyLong(), anyDouble(), anyDouble(), any());
    }

    @Test
    void usuarioAutenticadoRecibeDesgloseEstacional() {
        Optional<Usuario> registrado = Optional.of(usuario());
        when(usuarioRepository.findByEmail("dani@test.com")).thenReturn(registrado);
        when(historialService.resumenPara(anyLong(), anyDouble(), anyDouble(), any()))
                .thenReturn(ResumenHistorialDTO.sinPrevios("invierno"));
        AnalisisService svc = servicio();

        AnalisisResponse r = svc.analizar(new AnalisisRequest(factura(), null, false), "dani@test.com");

        assertEquals(240.0, r.costoEstimadoMensual(), DELTA);
        assertNotNull(r.costos());
        assertEquals("invierno", r.costos().estacion());
        assertEquals(0.1, r.costos().pctEstacional(), DELTA);
        assertEquals(0.15, r.costos().pctHorarioPico(), DELTA);
        assertEquals(300.0, r.costos().costoAjustadoMensual(), DELTA);
        assertNotNull(r.historialResumen());
    }

    @Test
    void laEstacionSeInfiereDesdeMonthParaElResumenDeHistorial() {
        Optional<Usuario> registrado = Optional.of(usuario());
        when(usuarioRepository.findByEmail("dani@test.com")).thenReturn(registrado);
        AnalisisService svc = servicio();

        svc.analizar(new AnalisisRequest(factura(), null, false), "dani@test.com");

        verify(historialService).resumenPara(1L, 320.0, 240.0, EstacionAnio.INVIERNO);
    }

    @Test
    void elContextoQueRecibeGeminiIncluyeCostosEHistorial() {
        Optional<Usuario> registrado = Optional.of(usuario());
        when(usuarioRepository.findByEmail("dani@test.com")).thenReturn(registrado);
        when(historialService.resumenPara(anyLong(), anyDouble(), anyDouble(), any()))
                .thenReturn(ResumenHistorialDTO.sinPrevios("invierno"));
        AnalisisService svc = servicio();

        svc.analizar(new AnalisisRequest(factura(), null, false), "dani@test.com");

        ArgumentCaptor<ContextoRecomendacion> captor = ArgumentCaptor.forClass(ContextoRecomendacion.class);
        verify(recomendacionService).generar(any(), any(), captor.capture());
        assertTrue(captor.getValue().tieneCostos());
    }

    private AnalisisService servicio() {
        ClasificadorPort clasificador = mock(ClasificadorPort.class);
        when(clasificador.clasificar(any())).thenReturn(new ClasificadorPort.Clasificacion(
                CategoriaEficiencia.MODERADO, Map.of("Moderado", 0.6)));

        CostoService costoService = mock(CostoService.class);
        when(costoService.calcularCostoMensual(any())).thenReturn(240.0);
        when(costoService.tarifaAplicada(any())).thenReturn(0.75);
        when(costoService.calcularIndiceEficiencia(any())).thenReturn(null);

        when(recomendacionService.generar(any(), any(), any())).thenReturn(List.of("tip"));

        ModeloParametros modelo = mock(ModeloParametros.class);
        when(modelo.get()).thenReturn(new ModeloParametros.Parametros(
                "energiai", "local-v1", null, null, null, null, null, null, null, null, null, 0.75));

        XgboostFeatureEncoder encoder = mock(XgboostFeatureEncoder.class);
        when(encoder.encodeFull(any())).thenReturn(new XgboostFeatures(
                new float[]{1.0f}, 40.0, 6.5, 0.0, "Departamento", 7, "si", 6.5, 8));

        return new AnalisisService(
                clasificador, costoService, recomendacionService, analisisRepository,
                usuarioRepository, modelo, encoder,
                new CalculadoraCostosEstacionales(parametros()), historialService, "local");
    }

    /** 320 kWh, tarifa 0.75, julio (invierno), horario pico, con LED, equipos nuevos. */
    private static FacturaDTO factura() {
        return new FacturaDTO(
                320, true, 8, "Departamento", 6.5,
                null, "7", null, null, null, true, "menor a 5 años", 0.75);
    }

    /** Mock creado fuera de cualquier stubbing en curso (Mockito no admite anidarlos). */
    private static Usuario usuario() {
        Usuario u = mock(Usuario.class);
        when(u.getId()).thenReturn(1L);
        return u;
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
