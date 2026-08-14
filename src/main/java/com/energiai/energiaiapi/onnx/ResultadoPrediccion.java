package com.energiai.energiaiapi.onnx;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resultado de inferencia ONNX.
 * <ul>
 *   <li>Modelo RF legacy (10 features): prediccion numerica + arreglo de probs</li>
 *   <li>version2.0 (6 features): etiqueta string + mapa de probs por clase</li>
 * </ul>
 */
public class ResultadoPrediccion {

    private final long prediccion;
    private final double[] probabilidades;
    private final String etiqueta;
    private final Map<String, Double> probabilidadesPorClase;

    public ResultadoPrediccion(long prediccion, double[] probabilidades) {
        this.prediccion = prediccion;
        this.probabilidades = probabilidades;
        this.etiqueta = null;
        this.probabilidadesPorClase = Collections.emptyMap();
    }

    private ResultadoPrediccion(String etiqueta, Map<String, Double> probabilidadesPorClase) {
        this.etiqueta = etiqueta;
        this.probabilidadesPorClase = probabilidadesPorClase != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(probabilidadesPorClase))
                : Collections.emptyMap();
        this.prediccion = -1;
        this.probabilidades = this.probabilidadesPorClase.values().stream()
                .mapToDouble(Double::doubleValue)
                .toArray();
    }

    public static ResultadoPrediccion deClasificacion(String etiqueta, Map<String, Double> probs) {
        return new ResultadoPrediccion(etiqueta, probs);
    }

    public long getPrediccion() {
        return prediccion;
    }

    public double[] getProbabilidades() {
        return probabilidades;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public Map<String, Double> getProbabilidadesPorClase() {
        return probabilidadesPorClase;
    }

    @Override
    public String toString() {
        if (etiqueta != null) {
            return String.format("{ etiqueta: %s, probabilidades: %s }", etiqueta, probabilidadesPorClase);
        }
        if (probabilidades != null && probabilidades.length >= 2) {
            return String.format("{ prediccion: %d, probabilidades: [%.4f, %.4f] }",
                    prediccion, probabilidades[0], probabilidades[1]);
        }
        return String.format("{ prediccion: %d, probabilidades: %s }",
                prediccion, Arrays.toString(probabilidades));
    }
}
