package com.energiai.energiaiapi.service;

import com.energiai.energiaiapi.domain.Analisis;
import com.energiai.energiaiapi.domain.Factura;
import com.energiai.energiaiapi.domain.Usuario;
import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.energiai.energiaiapi.dto.AnalisisRequest;
import com.energiai.energiaiapi.dto.AnalisisResponse;
import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.dto.ResultadoModeloDTO;
import com.energiai.energiaiapi.repository.AnalisisRepository;
import com.energiai.energiaiapi.repository.UsuarioRepository;
import com.energiai.energiaiapi.service.inference.ClasificadorOnnxAdapter;
import com.energiai.energiaiapi.service.inference.ClasificadorPort;
import com.energiai.energiaiapi.service.inference.ModeloParametros;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Orquesta un analisis energetico:
 *  1. Toma la clasificacion del frontend (ONNX) o, si no vino, la calcula con el fallback en Java.
 *  2. Calcula negocio: costo mensual, indice de eficiencia y recomendaciones.
 *  3. Persiste opcionalmente en el historial (solo si guardar=true y hay usuario autenticado).
 */
@Service
public class AnalisisService {

    private static final String FUENTE_FRONTEND = "FRONTEND_ONNX";
    private static final String FUENTE_BACKEND_ONNX = "BACKEND_ONNX";
    private static final String FUENTE_FALLBACK = "BACKEND_FALLBACK";

    private final ClasificadorPort clasificador;
    private final CostoService costoService;
    private final RecomendacionService recomendacionService;
    private final AnalisisRepository analisisRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModeloParametros modelo;
    private final String estrategiaModelo;

    public AnalisisService(ClasificadorPort clasificador,
                           CostoService costoService,
                           RecomendacionService recomendacionService,
                           AnalisisRepository analisisRepository,
                           UsuarioRepository usuarioRepository,
                           ModeloParametros modelo,
                           @Value("${app.modelo.estrategia:onnx}") String estrategiaModelo) {
        this.clasificador = clasificador;
        this.costoService = costoService;
        this.recomendacionService = recomendacionService;
        this.analisisRepository = analisisRepository;
        this.usuarioRepository = usuarioRepository;
        this.modelo = modelo;
        this.estrategiaModelo = estrategiaModelo;
    }

    @Transactional
    public AnalisisResponse analizar(AnalisisRequest request, String emailUsuario) {
        FacturaDTO f = request.factura().canonicalizada();

        // 1. Clasificacion: preferimos el resultado del frontend; si no vino, fallback en backend.
        CategoriaEficiencia categoria;
        Map<String, Double> probabilidades;
        String fuente;

        ResultadoModeloDTO resultado = request.resultado();
        if (resultado != null && resultado.categoria() != null) {
            categoria = CategoriaEficiencia.desdeEtiqueta(resultado.categoria());
            probabilidades = resultado.probabilidades() != null
                    ? resultado.probabilidades()
                    : Collections.emptyMap();
            fuente = FUENTE_FRONTEND;
        } else {
            ClasificadorPort.Clasificacion c = clasificador.clasificar(f);
            categoria = c.categoria();
            probabilidades = c.probabilidades();
            fuente = "onnx".equalsIgnoreCase(estrategiaModelo) ? FUENTE_BACKEND_ONNX : FUENTE_FALLBACK;
        }

        // 2. Negocio
        double costo = costoService.calcularCostoMensual(f);
        Double iie = costoService.calcularIndiceEficiencia(f);
        List<String> recomendaciones = recomendacionService.generar(f, categoria);
        String modeloVersion = resolverVersionModelo();

        // 3. Persistencia opcional
        boolean guardado = false;
        Long analisisId = null;
        Instant creadoEn = Instant.now();

        if (request.guardar() && emailUsuario != null) {
            Usuario usuario = usuarioRepository.findByEmail(emailUsuario).orElse(null);
            if (usuario != null) {
                Analisis analisis = construirAnalisis(
                        f, usuario, categoria, probabilidades, costo, iie, modeloVersion, fuente, recomendaciones);
                analisis = analisisRepository.save(analisis);
                guardado = true;
                analisisId = analisis.getId();
                creadoEn = analisis.getCreadoEn();
            }
        }

        return new AnalisisResponse(
                categoria, probabilidades, costo, iie, recomendaciones,
                modeloVersion, fuente, guardado, analisisId, creadoEn);
    }

    private Analisis construirAnalisis(FacturaDTO f,
                                       Usuario usuario,
                                       CategoriaEficiencia categoria,
                                       Map<String, Double> probabilidades,
                                       double costo,
                                       Double iie,
                                       String modeloVersion,
                                       String fuente,
                                       List<String> recomendaciones) {
        Factura factura = mapearFactura(f);

        Analisis analisis = new Analisis();
        analisis.setUsuario(usuario);
        analisis.setFactura(factura);
        analisis.setCategoria(categoria);
        analisis.setProbabilidad(probabilidadDe(probabilidades, categoria));
        analisis.setCostoEstimadoMensual(costo);
        analisis.setIndiceEficiencia(iie);
        analisis.setModeloVersion(modeloVersion);
        analisis.setFuenteClasificacion(fuente);
        analisis.setRecomendaciones(recomendaciones);
        return analisis;
    }

    private double probabilidadDe(Map<String, Double> probabilidades, CategoriaEficiencia categoria) {
        if (probabilidades == null || probabilidades.isEmpty()) {
            return 0.0;
        }
        Double p = probabilidades.get(categoria.getEtiqueta());
        return p != null ? p : 0.0;
    }

    private String resolverVersionModelo() {
        if (clasificador instanceof ClasificadorOnnxAdapter onnx) {
            return onnx.getVersion();
        }
        return modelo.get().version();
    }

    private Factura mapearFactura(FacturaDTO f) {
        Factura factura = new Factura();
        factura.setConsumoMensual(f.consumoMensual());
        factura.setUsoHorarioPico(f.usoHorarioPico());
        factura.setCantidadEquipos(f.cantidadEquipos());
        factura.setTipoInmueble(f.tipoInmueble());
        factura.setHorasPromedioUso(f.horasPromedioUso());
        factura.setEstacionAnio(f.estacionAnio());
        factura.setNumeroPersonas(f.numeroPersonas());
        factura.setTieneAireAcondicionado(f.tieneAireAcondicionado());
        factura.setTieneCalentador(f.tieneCalentador());
        factura.setTieneIluminacionLed(f.tieneIluminacionLed());
        factura.setAntiguedadElectrodomesticos(f.antiguedadElectrodomesticos());
        factura.setTarifaElectrica(f.tarifaElectrica());
        return factura;
    }
}
