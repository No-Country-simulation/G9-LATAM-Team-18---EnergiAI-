package com.energiai.energiaiapi.service.costos;

import com.energiai.energiaiapi.domain.enums.EstacionAnio;
import com.energiai.energiaiapi.domain.enums.Mes;
import com.energiai.energiaiapi.domain.enums.TipoInmueble;
import com.energiai.energiaiapi.dto.BenchmarkConsumoDTO;
import com.energiai.energiaiapi.dto.CostosEstacionalesDTO;
import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.dto.ProyeccionEstacionalDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Traduce las columnas Q..AJ de la hoja Parametros de {@code Datasetup FINAL FINAL.xlsx} a un desglose
 * de costos: recargo por estacion + los tres recargos accionables (horario pico, ausencia de LED,
 * electrodomesticos de mas de 5 años).
 *
 * <p>Es un calculo de negocio puro: no toca el pipeline JSON -&gt; ONNX ni la categoria devuelta
 * por el modelo. El benchmark de consumo se expone como referencia informativa.
 */
@Component
public class CalculadoraCostosEstacionales {

    private static final int MESES_ANIO = 12;
    private static final int MESES_POR_ESTACION = 3;

    private final ParametrosCosto parametros;

    public CalculadoraCostosEstacionales(ParametrosCosto parametros) {
        this.parametros = parametros;
    }

    /**
     * @param factura           factura ya canonicalizada
     * @param costoBrutoMensual consumo x tarifa (columna Q de la planilla)
     * @param tarifaAplicada    tarifa efectivamente usada para el costo bruto
     */
    public CostosEstacionalesDTO calcular(FacturaDTO factura, double costoBrutoMensual, double tarifaAplicada) {
        EstacionAnio estacion = estacionDe(factura);

        double pctEstacional = parametros.recargoEstacional(estacion);
        double pctHorarioPico = Boolean.TRUE.equals(factura.usoHorarioPico())
                ? parametros.recargoHorarioPico()
                : 0.0;
        double pctSinLed = Boolean.FALSE.equals(factura.tieneIluminacionLed())
                ? parametros.recargoSinIluminacionLed()
                : 0.0;
        double pctAntiguedad = parametros.antiguedadConRecargo(factura.antiguedadElectrodomesticos())
                ? parametros.recargoElectrodomesticosAntiguos()
                : 0.0;

        double pctAccionables = pctHorarioPico + pctSinLed + pctAntiguedad;
        double pctAjusteTotal = pctEstacional + pctAccionables;

        double costoAnualBruto = costoBrutoMensual * MESES_ANIO;
        List<ProyeccionEstacionalDTO> proyeccion =
                proyeccionEstacional(costoBrutoMensual, pctAccionables, estacion);
        double costoAnualEstacionalizado = proyeccion.stream()
                .mapToDouble(p -> p.costoMensualEstimado() * MESES_POR_ESTACION)
                .sum();

        return new CostosEstacionalesDTO(
                estacion != null ? estacion.getValor() : null,
                tarifaAplicada,
                costoBrutoMensual,
                pctEstacional,
                costoBrutoMensual * pctEstacional,
                pctHorarioPico,
                costoBrutoMensual * pctHorarioPico,
                pctSinLed,
                costoBrutoMensual * pctSinLed,
                pctAntiguedad,
                costoBrutoMensual * pctAntiguedad,
                pctAjusteTotal,
                costoBrutoMensual * (1 + pctAjusteTotal),
                costoAnualBruto,
                costoAnualBruto * (1 + pctAjusteTotal),
                costoAnualEstacionalizado,
                pctAccionables,
                costoBrutoMensual * pctAccionables,
                costoAnualBruto * pctAccionables,
                proyeccion,
                benchmark(factura, estacion).orElse(null),
                parametros.version(),
                parametros.fuenteUmbrales());
    }

    /** Estacion de negocio: se prefiere la inferida desde month; estacion_anio es legado. */
    public EstacionAnio estacionDe(FacturaDTO factura) {
        return EstacionAnio.desdeMes(Mes.numeroDesde(factura.mes()).orElse(null))
                .or(() -> EstacionAnio.desde(factura.estacionAnio()))
                .orElse(null);
    }

    private List<ProyeccionEstacionalDTO> proyeccionEstacional(double costoBrutoMensual,
                                                               double pctAccionables,
                                                               EstacionAnio estacionActual) {
        List<ProyeccionEstacionalDTO> proyeccion = new ArrayList<>(EstacionAnio.values().length);
        for (EstacionAnio e : EstacionAnio.values()) {
            double pct = parametros.recargoEstacional(e);
            proyeccion.add(new ProyeccionEstacionalDTO(
                    e.getValor(),
                    pct,
                    costoBrutoMensual * (1 + pct + pctAccionables),
                    e == estacionActual));
        }
        return List.copyOf(proyeccion);
    }

    private Optional<BenchmarkConsumoDTO> benchmark(FacturaDTO factura, EstacionAnio estacion) {
        if (factura.consumoMensual() == null) {
            return Optional.empty();
        }
        Optional<TipoInmueble> tipo = TipoInmueble.desde(factura.tipoInmueble());
        if (tipo.isEmpty() || estacion == null) {
            return Optional.empty();
        }
        return parametros.umbral(tipo.get(), estacion).map(u -> {
            double consumo = factura.consumoMensual().doubleValue();
            String posicion;
            if (consumo <= u.eficienteHasta()) {
                posicion = "dentro_eficiente";
            } else if (consumo <= u.moderadoHasta()) {
                posicion = "moderado";
            } else {
                posicion = "sobre_moderado";
            }
            return new BenchmarkConsumoDTO(
                    factura.tipoInmueble(),
                    estacion.getValor(),
                    u.eficienteHasta(),
                    u.moderadoHasta(),
                    consumo,
                    consumo - u.eficienteHasta(),
                    posicion);
        });
    }
}
