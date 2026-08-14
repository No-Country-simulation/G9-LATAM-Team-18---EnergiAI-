package com.energiai.energiaiapi.service.recomendacion;

import com.energiai.energiaiapi.domain.enums.AntiguedadElectrodomesticos;
import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.energiai.energiaiapi.domain.enums.EstacionAnio;
import com.energiai.energiaiapi.domain.enums.Mes;
import com.energiai.energiaiapi.dto.CostosEstacionalesDTO;
import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.dto.ProyeccionEstacionalDTO;
import com.energiai.energiaiapi.dto.ResumenHistorialDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Selecciona un set cerrado de temas/textos base a partir de la factura y la categoria.
 *
 * <p>Para el usuario registrado se agregan temas contextuales con cifras concretas
 * (estacionalidad, ahorro potencial y variacion contra su historial). El primero de esos temas
 * encabeza la lista para que sobreviva al recorte por {@code max-items}; el resto va al final.
 */
@Component
public class ReglasRecomendacion {

    /** Variacion minima para que valga la pena mencionar la tendencia del historial. */
    private static final double UMBRAL_VARIACION = 0.05;

    public List<TemaRecomendacion> seleccionar(FacturaDTO f, CategoriaEficiencia categoria) {
        return seleccionar(f, categoria, ContextoRecomendacion.vacio());
    }

    public List<TemaRecomendacion> seleccionar(FacturaDTO f,
                                               CategoriaEficiencia categoria,
                                               ContextoRecomendacion contexto) {
        List<TemaRecomendacion> accionables = temasAccionables(f, categoria);
        List<TemaRecomendacion> contextuales = temasContextuales(contexto);

        if (contextuales.isEmpty()) {
            return List.copyOf(accionables);
        }

        List<TemaRecomendacion> temas = new ArrayList<>(accionables.size() + contextuales.size());
        temas.add(contextuales.getFirst());
        temas.addAll(accionables);
        temas.addAll(contextuales.subList(1, contextuales.size()));
        return List.copyOf(temas);
    }

    private List<TemaRecomendacion> temasAccionables(FacturaDTO f, CategoriaEficiencia categoria) {
        List<TemaRecomendacion> temas = new ArrayList<>();

        if (Boolean.FALSE.equals(f.tieneIluminacionLed())) {
            temas.add(new TemaRecomendacion(
                    "iluminacion_led",
                    "Cambia la iluminacion a tecnologia LED para reducir el consumo de iluminacion."));
        }
        if (Boolean.TRUE.equals(f.usoHorarioPico())) {
            temas.add(new TemaRecomendacion(
                    "horario_pico",
                    "Evita el uso de equipos de alto consumo en horario pico para reducir el costo."));
        }
        if (Boolean.TRUE.equals(f.tieneAireAcondicionado())) {
            // Prioridad: estacion inferida desde month (hemisferio sur); estacion_anio es legado.
            EstacionAnio estacion = EstacionAnio.desdeMes(Mes.numeroDesde(f.mes()).orElse(null))
                    .or(() -> EstacionAnio.desde(f.estacionAnio()))
                    .orElse(null);
            if (estacion == EstacionAnio.VERANO) {
                temas.add(new TemaRecomendacion(
                        "aire_verano",
                        "En verano, configura el aire acondicionado en 24 grados y mantén los filtros limpios."));
            } else {
                temas.add(new TemaRecomendacion(
                        "aire",
                        "Configura el aire acondicionado en 24 grados y realiza mantenimiento de los filtros."));
            }
        }
        if (Boolean.TRUE.equals(f.tieneCalentador())) {
            temas.add(new TemaRecomendacion(
                    "calentador",
                    "Reduce la temperatura del calentador electrico y aisla el tanque para ahorrar energia."));
        }
        AntiguedadElectrodomesticos.desde(f.antiguedadElectrodomesticos()).ifPresent(a -> {
            if (a.esAntigua()) {
                temas.add(new TemaRecomendacion(
                        "equipos_antiguos",
                        "Considera reemplazar electrodomesticos antiguos por modelos de mayor eficiencia energetica."));
            }
        });
        if (categoria == CategoriaEficiencia.INEFICIENTE) {
            temas.add(new TemaRecomendacion(
                    "categoria_ineficiente",
                    "Tu consumo es alto respecto a inmuebles similares: revisa los equipos de mayor consumo."));
        } else if (categoria == CategoriaEficiencia.MODERADO) {
            temas.add(new TemaRecomendacion(
                    "categoria_moderado",
                    "Tu perfil es moderado: pequeños ajustes en horarios y equipos pueden mejorar la eficiencia."));
        }

        if (temas.isEmpty()) {
            temas.add(new TemaRecomendacion(
                    "habitos_eficientes",
                    "Buen trabajo: tu consumo se encuentra dentro de un rango eficiente. Mantén estos hábitos."));
        }
        return temas;
    }

    /**
     * Temas con cifras del historial y del desglose de costos, ordenados por relevancia:
     * primero la comparativa personal, despues el dinero y por ultimo la estacionalidad.
     */
    private List<TemaRecomendacion> temasContextuales(ContextoRecomendacion contexto) {
        List<TemaRecomendacion> temas = new ArrayList<>();
        if (contexto == null || contexto.vacia()) {
            return temas;
        }

        if (contexto.tieneHistorial()) {
            ResumenHistorialDTO h = contexto.historial();
            Double variacionEstacion = h.variacionVsMismaEstacionPct();
            if (variacionEstacion != null && Math.abs(variacionEstacion) >= UMBRAL_VARIACION) {
                temas.add(new TemaRecomendacion(
                        "comparativa_misma_estacion",
                        "Comparado con tus registros de " + h.estacion() + ", tu consumo vario "
                                + Cifras.pctVariacion(variacionEstacion) + ": "
                                + (variacionEstacion > 0 ? "revisa que cambio en tus habitos."
                                        : "mantén lo que estas haciendo bien.")));
            } else if (h.variacionConsumoPct() != null && h.consumoPromedioKwh() != null
                    && Math.abs(h.variacionConsumoPct()) >= UMBRAL_VARIACION) {
                temas.add(new TemaRecomendacion(
                        "tendencia_historial",
                        "Tu consumo vario " + Cifras.pctVariacion(h.variacionConsumoPct())
                                + " respecto al promedio de tus " + h.analisisPrevios()
                                + " analisis previos ("
                                + Cifras.kwh(h.consumoPromedioKwh()) + ")."));
            }
        }

        if (contexto.tieneCostos()) {
            CostosEstacionalesDTO c = contexto.costos();
            if (c.pctAhorroPotencial() > 0) {
                temas.add(new TemaRecomendacion(
                        "ahorro_potencial",
                        "Corrigiendo los factores penalizados puedes ahorrar hasta "
                                + Cifras.usd(c.ahorroPotencialMensual()) + " por mes ("
                                + Cifras.usd(c.ahorroPotencialAnual()) + " al año)."));
            }
            ProyeccionEstacionalDTO masCara = c.estacionMasCara();
            if (masCara != null && c.estacion() != null) {
                if (masCara.estacion().equals(c.estacion())) {
                    temas.add(new TemaRecomendacion(
                            "estacion_pico",
                            "Estas en " + c.estacion() + ", la estacion mas cara del año: tu factura sube "
                                    + Cifras.pct(c.pctEstacional()) + " frente al costo base."));
                } else {
                    temas.add(new TemaRecomendacion(
                            "anticipo_estacional",
                            "Con este consumo, en " + masCara.estacion() + " pagarias cerca de "
                                    + Cifras.usd(masCara.costoMensualEstimado())
                                    + " por mes: anticipa ese aumento."));
                }
            }
        }

        return temas;
    }
}
