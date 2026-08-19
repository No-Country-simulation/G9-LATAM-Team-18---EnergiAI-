package com.energiai.energiaiapi.service;

import com.energiai.energiaiapi.domain.Analisis;
import com.energiai.energiaiapi.domain.Factura;
import com.energiai.energiaiapi.domain.Usuario;
import com.energiai.energiaiapi.domain.enums.EstacionAnio;
import com.energiai.energiaiapi.domain.enums.Mes;
import com.energiai.energiaiapi.dto.CostosGuardadosDTO;
import com.energiai.energiaiapi.dto.HistorialItemResponse;
import com.energiai.energiaiapi.dto.ResumenHistorialDTO;
import com.energiai.energiaiapi.exception.RecursoNoEncontradoException;
import com.energiai.energiaiapi.repository.AnalisisRepository;
import com.energiai.energiaiapi.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.DoubleStream;

/**
 * Historial de analisis del usuario autenticado. El usuario se resuelve a partir
 * del email del token JWT (nunca se recibe como parametro editable por el cliente).
 */
@Service
public class HistorialService {

    private final AnalisisRepository analisisRepository;
    private final UsuarioRepository usuarioRepository;

    public HistorialService(AnalisisRepository analisisRepository, UsuarioRepository usuarioRepository) {
        this.analisisRepository = analisisRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<HistorialItemResponse> historialDe(String emailUsuario) {
        Usuario usuario = usuarioDe(emailUsuario);
        return analisisRepository.findByUsuarioIdOrderByCreadoEnDesc(usuario.getId())
                .stream()
                .map(this::aItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public HistorialItemResponse detalleDe(String emailUsuario, Long analisisId) {
        Usuario usuario = usuarioDe(emailUsuario);
        Analisis analisis = analisisRepository.findByIdAndUsuarioId(analisisId, usuario.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Analisis no encontrado o no pertenece al usuario: " + analisisId));
        return aItem(analisis);
    }

    /**
     * Compara el analisis en curso contra los ya persistidos del usuario. El costo se compara
     * bruto contra bruto (consumo x tarifa) porque es el unico valor presente en todos los
     * registros historicos, incluidos los anteriores al desglose estacional.
     *
     * @param consumoActualKwh consumo del analisis en curso
     * @param costoBrutoActual costo bruto del analisis en curso
     * @param estacionActual   estacion inferida del analisis en curso (puede ser null)
     */
    @Transactional(readOnly = true)
    public ResumenHistorialDTO resumenPara(Long usuarioId,
                                           double consumoActualKwh,
                                           double costoBrutoActual,
                                           EstacionAnio estacionActual) {
        String etiquetaEstacion = estacionActual != null ? estacionActual.getValor() : null;
        List<Analisis> previos = analisisRepository.findByUsuarioIdOrderByCreadoEnDesc(usuarioId);
        if (previos.isEmpty()) {
            return ResumenHistorialDTO.sinPrevios(etiquetaEstacion);
        }

        Double consumoPromedio = promedio(previos.stream()
                .map(HistorialService::consumoDe)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue));
        Double costoPromedio = promedio(previos.stream()
                .mapToDouble(Analisis::getCostoEstimadoMensual));

        Double variacionConsumo = variacion(consumoActualKwh, consumoPromedio);
        Double variacionCosto = variacion(costoBrutoActual, costoPromedio);

        List<Analisis> mismaEstacion = estacionActual == null
                ? List.of()
                : previos.stream().filter(a -> estacionDe(a) == estacionActual).toList();
        Double consumoMismaEstacion = mismaEstacion.isEmpty() ? null : promedio(mismaEstacion.stream()
                .map(HistorialService::consumoDe)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue));

        Analisis ultimo = previos.getFirst();

        return new ResumenHistorialDTO(
                previos.size(),
                consumoPromedio,
                costoPromedio,
                variacionConsumo,
                variacionCosto,
                tendencia(variacionConsumo),
                ultimo.getCreadoEn(),
                ultimo.getCategoria(),
                consumoDe(ultimo),
                mismaEstacion.size(),
                consumoMismaEstacion,
                variacion(consumoActualKwh, consumoMismaEstacion),
                etiquetaEstacion);
    }

    private static Double promedio(DoubleStream valores) {
        OptionalDouble media = valores.average();
        return media.isPresent() ? media.getAsDouble() : null;
    }

    private static Double variacion(double actual, Double referencia) {
        if (referencia == null || referencia == 0.0) {
            return null;
        }
        return (actual - referencia) / referencia;
    }

    /** Umbral de 5%: por debajo se considera ruido y el consumo se informa como estable. */
    private static String tendencia(Double variacionConsumo) {
        if (variacionConsumo == null) {
            return "sin_referencia";
        }
        if (variacionConsumo > 0.05) {
            return "al_alza";
        }
        if (variacionConsumo < -0.05) {
            return "a_la_baja";
        }
        return "estable";
    }

    private static Double consumoDe(Analisis a) {
        return a.getFactura() != null ? a.getFactura().getConsumoMensual() : null;
    }

    private static EstacionAnio estacionDe(Analisis a) {
        Factura f = a.getFactura();
        if (f == null) {
            return null;
        }
        return EstacionAnio.desdeMes(Mes.numeroDesde(f.getMes()).orElse(null))
                .or(() -> EstacionAnio.desde(f.getEstacionAnio()))
                .orElse(null);
    }

    private Usuario usuarioDe(String emailUsuario) {
        return usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + emailUsuario));
    }

    private HistorialItemResponse aItem(Analisis a) {
        return new HistorialItemResponse(
                a.getId(),
                a.getCreadoEn(),
                a.getCategoria(),
                a.getProbabilidad(),
                a.getCostoEstimadoMensual(),
                a.getIndiceEficiencia(),
                a.getFactura() != null ? a.getFactura().getConsumoMensual() : null,
                a.getFactura() != null ? a.getFactura().getTipoInmueble() : null,
                a.getFactura() != null ? a.getFactura().getMes() : null,
                List.copyOf(a.getRecomendaciones()),
                costosDe(a)
        );
    }

    /** Null para analisis anteriores a la V5, que no tienen desglose estacional guardado. */
    private static CostosGuardadosDTO costosDe(Analisis a) {
        if (a.getCostoAjustadoMensual() == null) {
            return null;
        }
        return new CostosGuardadosDTO(
                a.getEstacionCalculo(),
                a.getCostoBrutoMensual(),
                a.getPctEstacional(),
                a.getPctAjusteTotal(),
                a.getCostoAjustadoMensual(),
                a.getPctAhorroPotencial(),
                a.getAhorroPotencialMensual(),
                a.getAhorroPotencialAnual(),
                a.getCostoAnualEstimado(),
                a.getCostoAnualEstacionalizado(),
                a.getParametrosCostosVersion());
    }
}
