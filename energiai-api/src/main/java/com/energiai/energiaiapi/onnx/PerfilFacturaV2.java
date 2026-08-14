package com.energiai.energiaiapi.onnx;

import ai.onnxruntime.OnnxMap;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Perfil para {@code version2.0.onnx}:
 * <ul>
 *   <li>Input: {@code float_input} shape [1, 6]</li>
 *   <li>Outputs: {@code output_label} (string) + {@code output_probability} (ZipMap)</li>
 * </ul>
 */
public class PerfilFacturaV2 implements IPerfilDeConsumo {

    private final float[] features;

    public PerfilFacturaV2(float[] features) {
        if (features == null || features.length != 6) {
            throw new IllegalArgumentException("version2.0.onnx espera exactamente 6 features float");
        }
        this.features = features;
    }

    @Override
    public Map<String, OnnxTensor> preprocesamiento(OrtEnvironment env) throws OrtException {
        float[][] entrada2D = new float[1][features.length];
        System.arraycopy(features, 0, entrada2D[0], 0, features.length);

        Map<String, OnnxTensor> feeds = new HashMap<>();
        feeds.put("float_input", OnnxTensor.createTensor(env, entrada2D));
        return feeds;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResultadoPrediccion posprocesamiento(OrtSession.Result resultados) throws OrtException {
        String etiqueta = extraerEtiqueta(resultados.get("output_label").get().getValue());
        Map<String, Double> probs = extraerProbabilidades(resultados.get("output_probability").get().getValue());

        // Mantiene compatibilidad con ResultadoPrediccion numerico + expone etiqueta/mapa via campos extendidos.
        return ResultadoPrediccion.deClasificacion(etiqueta, probs);
    }

    private static String extraerEtiqueta(Object value) {
        if (value instanceof String[] arr) {
            return arr[0];
        }
        if (value instanceof Object[] arr && arr[0] != null) {
            return arr[0].toString();
        }
        if (value instanceof String s) {
            return s;
        }
        throw new IllegalStateException("Tipo inesperado de output_label: " + value.getClass());
    }

    private static Map<String, Double> extraerProbabilidades(Object value) throws OrtException {
        if (value instanceof List<?> lista && !lista.isEmpty()) {
            Object primero = lista.get(0);
            // ORT Java envuelve el ZipMap como List<OnnxMap>
            if (primero instanceof OnnxMap onnxMap) {
                return aDoubles(onnxMap.getValue());
            }
            if (primero instanceof Map<?, ?> map) {
                return aDoubles(map);
            }
        }
        if (value instanceof OnnxMap onnxMap) {
            return aDoubles(onnxMap.getValue());
        }
        if (value instanceof Map<?, ?> map) {
            return aDoubles(map);
        }
        throw new IllegalStateException("Tipo inesperado de output_probability: " + value.getClass());
    }

    private static Map<String, Double> aDoubles(Map<?, ?> map) {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            out.put(String.valueOf(e.getKey()), ((Number) e.getValue()).doubleValue());
        }
        return out;
    }
}
