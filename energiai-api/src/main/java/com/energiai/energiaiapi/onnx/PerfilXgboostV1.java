package com.energiai.energiaiapi.onnx;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;

import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Perfil para modelos XGBoost ONNX (`modelo_xgboost_v2.onnx` / v1):
 * <ul>
 *   <li>Input: {@code float_input} shape [1, 22]</li>
 *   <li>Outputs: {@code label} (int64) + {@code probabilities} (float [1, 3])</li>
 * </ul>
 * Indices de clase: 0=Eficiente, 1=Moderado, 2=Ineficiente.
 */
public class PerfilXgboostV1 implements IPerfilDeConsumo {

    private static final CategoriaEficiencia[] CLASES = {
            CategoriaEficiencia.EFICIENTE,
            CategoriaEficiencia.MODERADO,
            CategoriaEficiencia.INEFICIENTE
    };

    private final float[] features;

    public PerfilXgboostV1(float[] features) {
        if (features == null || features.length != 22) {
            throw new IllegalArgumentException("modelo XGBoost ONNX espera exactamente 22 features float");
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
    public ResultadoPrediccion posprocesamiento(OrtSession.Result resultados) throws OrtException {
        int labelIdx = extraerLabel(resultados.get("label").orElseThrow().getValue());
        Map<String, Double> probs = extraerProbabilidades(resultados.get("probabilities").orElseThrow().getValue());

        String etiqueta = CLASES[Math.max(0, Math.min(labelIdx, CLASES.length - 1))].getEtiqueta();
        return ResultadoPrediccion.deClasificacion(etiqueta, probs);
    }

    private static int extraerLabel(Object value) {
        if (value instanceof long[] arr) {
            return (int) arr[0];
        }
        if (value instanceof LongBuffer buf) {
            return (int) buf.get(0);
        }
        if (value instanceof int[] arr) {
            return arr[0];
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        throw new IllegalStateException("Tipo inesperado de label: " + value.getClass());
    }

    private static Map<String, Double> extraerProbabilidades(Object value) {
        float[] flat;
        if (value instanceof float[][] matrix) {
            flat = matrix[0];
        } else if (value instanceof float[] arr) {
            flat = arr;
        } else if (value instanceof double[][] matrix) {
            flat = new float[matrix[0].length];
            for (int i = 0; i < flat.length; i++) {
                flat[i] = (float) matrix[0][i];
            }
        } else {
            throw new IllegalStateException("Tipo inesperado de probabilities: " + value.getClass());
        }

        Map<String, Double> out = new LinkedHashMap<>();
        for (int i = 0; i < CLASES.length; i++) {
            double p = i < flat.length ? flat[i] : 0.0;
            out.put(CLASES[i].getEtiqueta(), p);
        }
        return out;
    }
}
