package com.energiai.energiaiapi.onnx;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.util.HashMap;
import java.util.Map;

/** Perfil de consumo: vector de exactamente 10 features float para el modelo RF ONNX. */
public class UnGranPerfil implements IPerfilDeConsumo {

    private final float[] valor;

    public UnGranPerfil(float[] valor) {
        if (valor == null || valor.length != 10) {
            throw new IllegalArgumentException("El perfil debe tener exactamente 10 features (float)");
        }
        this.valor = valor;
    }

    @Override
    public Map<String, OnnxTensor> preprocesamiento(OrtEnvironment env) throws OrtException {
        float[][] entrada2D = new float[1][this.valor.length];
        System.arraycopy(this.valor, 0, entrada2D[0], 0, this.valor.length);

        OnnxTensor tensorEntrada = OnnxTensor.createTensor(env, entrada2D);

        Map<String, OnnxTensor> feeds = new HashMap<>();
        feeds.put("float_input", tensorEntrada);
        return feeds;
    }

    @Override
    public ResultadoPrediccion posprocesamiento(OrtSession.Result resultados) throws OrtException {
        long[] labelData = (long[]) resultados.get("label").get().getValue();
        long prediccion = labelData[0];

        float[][] probMatrix = (float[][]) resultados.get("probabilities").get().getValue();
        float[] fila = probMatrix[0];
        double[] probs = new double[fila.length];
        for (int i = 0; i < fila.length; i++) {
            probs[i] = fila[i];
        }
        return new ResultadoPrediccion(prediccion, probs);
    }
}
