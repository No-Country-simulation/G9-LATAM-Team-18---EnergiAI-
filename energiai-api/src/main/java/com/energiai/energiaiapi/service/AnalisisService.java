package com.energiai.energiaiapi.service;

import com.energiai.energiaiapi.domain.Analisis;
import com.energiai.energiaiapi.domain.Factura;
import com.energiai.energiaiapi.domain.Usuario;
import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.energiai.energiaiapi.dto.AnalisisRequest;
import com.energiai.energiaiapi.dto.AnalisisResponse;
import com.energiai.energiaiapi.dto.ConsultaModeloDTO;
import com.energiai.energiaiapi.dto.CostosEstacionalesDTO;
import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.dto.FeaturesSinteticasDTO;
import com.energiai.energiaiapi.dto.ResultadoModeloDTO;
import com.energiai.energiaiapi.dto.ResumenHistorialDTO;
import com.energiai.energiaiapi.repository.AnalisisRepository;
import com.energiai.energiaiapi.repository.UsuarioRepository;
import com.energiai.energiaiapi.onnx.XgboostFeatureEncoder;
import com.energiai.energiaiapi.onnx.XgboostFeatures;
import com.energiai.energiaiapi.service.costos.CalculadoraCostosEstacionales;
import com.energiai.energiaiapi.service.inference.ClasificadorOnnxAdapter;
import com.energiai.energiaiapi.service.inference.ClasificadorPort;
import com.energiai.energiaiapi.service.inference.ModeloParametros;
import com.energiai.energiaiapi.service.recomendacion.ContextoRecomendacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Orquesta un analisis energetico:
 *  1. Toma la clasificacion del frontend (ONNX) o, si no vino, la calcula con el fallback en Java.
 *  2. Calcula negocio: costo mensual, score de huella (kWh × 0.22 × f edad, o null), IIE y recomendaciones.
 *  3. Persiste opcionalmente en el historial (solo si guardar=true y hay usuario autenticado).
 */
@Service
public class AnalisisService {

    private static final Logger log = LoggerFactory.getLogger(AnalisisService.class);

    private static final String FUENTE_FRONTEND = "FRONTEND_ONNX";
    private static final String FUENTE_BACKEND_ONNX = "BACKEND_ONNX";
    private static final String FUENTE_FALLBACK = "BACKEND_FALLBACK";

    private final ClasificadorPort clasificador;
    private final CostoService costoService;
    private final RecomendacionService recomendacionService;
    private final AnalisisRepository analisisRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModeloParametros modelo;
    private final XgboostFeatureEncoder xgboostEncoder;
    private final CalculadoraCostosEstacionales calculadoraCostos;
    private final HistorialService historialService;
    private final String estrategiaModelo;

    public AnalisisService(ClasificadorPort clasificador,
                           CostoService costoService,
                           RecomendacionService recomendacionService,
                           AnalisisRepository analisisRepository,
                           UsuarioRepository usuarioRepository,
                           ModeloParametros modelo,
                           XgboostFeatureEncoder xgboostEncoder,
                           CalculadoraCostosEstacionales calculadoraCostos,
                           HistorialService historialService,
                           @Value("${app.modelo.estrategia:onnx}") String estrategiaModelo) {
        this.clasificador = clasificador;
        this.costoService = costoService;
        this.recomendacionService = recomendacionService;
        this.analisisRepository = analisisRepository;
        this.usuarioRepository = usuarioRepository;
        this.modelo = modelo;
        this.xgboostEncoder = xgboostEncoder;
        this.calculadoraCostos = calculadoraCostos;
        this.historialService = historialService;
        this.estrategiaModelo = estrategiaModelo;
    }

    @Transactional
    public AnalisisResponse analizar(AnalisisRequest request, String emailUsuario) {
        long t0 = System.nanoTime();
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

        // 2. Negocio base: identico para invitado y registrado (consumo x tarifa y huella).
        double costo = costoService.calcularCostoMensual(f);
        Double huellaCarbono = costoService.calculoHuellaCarbono(f);
        Double iie = costoService.calcularIndiceEficiencia(f);
        String modeloVersion = resolverVersionModelo();
        XgboostFeatures features = xgboostEncoder.encodeFull(f);

        // 3. Modo historial: el usuario del token habilita costos estacionales y comparativa.
        Usuario usuario = (emailUsuario != null)
                ? usuarioRepository.findByEmailIgnoreCase(emailUsuario)
                    .or(() -> usuarioRepository.findByEmail(emailUsuario))
                    .orElse(null)
                : null;
        CostosEstacionalesDTO costos = (usuario != null)
                ? calculadoraCostos.calcular(f, costo, costoService.tarifaAplicada(f))
                : null;
        ResumenHistorialDTO resumenHistorial = (usuario != null)
                ? historialService.resumenPara(usuario.getId(), f.consumoMensual().doubleValue(), costo,
                        calculadoraCostos.estacionDe(f))
                : null;

        // 4. Recomendaciones: matizadas con estacionalidad, costos e historial si hay usuario.
        List<String> recomendaciones = recomendacionService.generar(f, categoria,
                new ContextoRecomendacion(costos, resumenHistorial));

        // 5. Persistencia opcional (requiere usuario autenticado registrado en BD)
        boolean guardado = false;
        Long analisisId = null;
        Instant creadoEn = Instant.now();

        if (request.guardar()) {
            if (emailUsuario == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Para guardar el analisis debes iniciar sesion (JWT)");
            }
            if (usuario == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuario del token no encontrado en la base de datos");
            }
            // Validar que no exista ya un análisis para este mes+año
            String mesNormalizado = f.mes();
            Integer anio = f.anio() != null ? f.anio() : java.time.Year.now().getValue();
            if (analisisRepository.existsByUsuarioIdAndFacturaMesAndFacturaAnio(usuario.getId(), mesNormalizado, anio)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        String.format("Ya existe un analisis para %s/%d. Elimina el existente o elige otro periodo.",
                                mesNormalizado, anio));
            }
            Analisis analisis = construirAnalisis(
                    f, usuario, categoria, probabilidades, costo, huellaCarbono, iie, modeloVersion, fuente,
                    recomendaciones, features, costos);
            analisis = analisisRepository.save(analisis);
            guardado = true;
            analisisId = analisis.getId();
            creadoEn = analisis.getCreadoEn();
        }

        AnalisisResponse response = new AnalisisResponse(
                categoria, probabilidades, costo, huellaCarbono, iie, recomendaciones,
                modeloVersion, fuente, guardado, analisisId, creadoEn,
                new ConsultaModeloDTO(
                        features.tipoInmuebleDs(),
                        features.month(),
                        features.usoHorarioPicoDs(),
                        features.horasAltoConsumo(),
                        features.cantidadEquipos()),
                new FeaturesSinteticasDTO(
                        features.intensidadPorEquipo(),
                        features.horasPicoInteraccion(),
                        features.desviacionEquiposTipo()),
                features.vector(),
                costos,
                resumenHistorial);
        long totalMs = (System.nanoTime() - t0) / 1_000_000L;
        log.info("[Analisis] response armado en {} ms (categoria={}, recs={}, fuente={}, guardar={}) antes de devolver al cliente",
                totalMs, categoria, recomendaciones.size(), fuente, guardado);
        return response;
    }

    private Analisis construirAnalisis(FacturaDTO f,
                                       Usuario usuario,
                                       CategoriaEficiencia categoria,
                                       Map<String, Double> probabilidades,
                                       double costo,
                                       Double huellaCarbono,
                                       Double iie,
                                       String modeloVersion,
                                       String fuente,
                                       List<String> recomendaciones,
                                       XgboostFeatures features,
                                       CostosEstacionalesDTO costos) {
        Factura factura = mapearFactura(f, features);

        Analisis analisis = new Analisis();
        analisis.setUsuario(usuario);
        analisis.setFactura(factura);
        analisis.setCategoria(categoria);
        analisis.setProbabilidad(probabilidadDe(probabilidades, categoria));
        analisis.setCostoEstimadoMensual(costo);
        analisis.setHuellaCarbonoKgCo2eMes(huellaCarbono);
        analisis.setIndiceEficiencia(iie);
        analisis.setModeloVersion(modeloVersion);
        analisis.setFuenteClasificacion(fuente);
        analisis.setRecomendaciones(recomendaciones);
        aplicarCostos(analisis, costos);
        return analisis;
    }

    /** Congela el desglose estacional junto con la version de parametros que lo produjo. */
    private void aplicarCostos(Analisis analisis, CostosEstacionalesDTO costos) {
        if (costos == null) {
            return;
        }
        analisis.setEstacionCalculo(costos.estacion());
        analisis.setCostoBrutoMensual(costos.costoBrutoMensual());
        analisis.setPctEstacional(costos.pctEstacional());
        analisis.setPctAjusteTotal(costos.pctAjusteTotal());
        analisis.setPctAhorroPotencial(costos.pctAhorroPotencial());
        analisis.setCostoAjustadoMensual(costos.costoAjustadoMensual());
        analisis.setAhorroPotencialMensual(costos.ahorroPotencialMensual());
        analisis.setAhorroPotencialAnual(costos.ahorroPotencialAnual());
        analisis.setCostoAnualEstimado(costos.costoAnualEstimado());
        analisis.setCostoAnualEstacionalizado(costos.costoAnualEstacionalizado());
        analisis.setParametrosCostosVersion(costos.parametrosVersion());
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

    private Factura mapearFactura(FacturaDTO f, XgboostFeatures sx) {
        Factura factura = new Factura();
        factura.setConsumoMensual(f.consumoMensual() == null ? null : f.consumoMensual().doubleValue());
        factura.setUsoHorarioPico(f.usoHorarioPico());
        factura.setCantidadEquipos(f.cantidadEquipos());
        factura.setTipoInmueble(f.tipoInmueble());
        factura.setHorasPromedioUso(f.horasPromedioUso());
        factura.setEstacionAnio(f.estacionAnio());
        factura.setMes(f.mes());
        factura.setAnio(f.anio() != null ? f.anio() : java.time.Year.now().getValue());
        factura.setNumeroPersonas(f.numeroPersonas());
        factura.setTieneAireAcondicionado(f.tieneAireAcondicionado());
        factura.setTieneCalentador(f.tieneCalentador());
        factura.setTieneIluminacionLed(f.tieneIluminacionLed());
        factura.setAntiguedadElectrodomesticos(f.antiguedadElectrodomesticos());
        factura.setTarifaElectrica(f.tarifaElectrica());
        factura.setIntensidadPorEquipo(sx.intensidadPorEquipo());
        factura.setHorasPicoInteraccion(sx.horasPicoInteraccion());
        factura.setDesviacionEquiposTipo(sx.desviacionEquiposTipo());
        return factura;
    }
}
