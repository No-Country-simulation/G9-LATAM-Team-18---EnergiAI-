package com.energiai.energiaiapi.onnx;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.util.Map;

public interface IPerfilDeConsumo {

    Map<String, OnnxTensor> preprocesamiento(OrtEnvironment env) throws OrtException;

    ResultadoPrediccion posprocesamiento(OrtSession.Result resultados) throws OrtException;
}
