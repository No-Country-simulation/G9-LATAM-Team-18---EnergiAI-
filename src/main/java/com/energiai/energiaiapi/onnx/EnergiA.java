package com.energiai.energiaiapi.onnx;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.util.Map;

/** Carga e inferencia del modelo ONNX (Random Forest / skl2onnx). */
public class EnergiA implements AutoCloseable {

    private final String direccionIA;
    private final byte[] modeloBytes;
    private OrtEnvironment env;
    private OrtSession session;

    public EnergiA(String direccionIA) {
        this.direccionIA = direccionIA;
        this.modeloBytes = null;
    }

    public EnergiA(byte[] modeloBytes) {
        this.direccionIA = null;
        this.modeloBytes = modeloBytes;
    }

    public synchronized void cargarModelo() throws OrtException {
        if (this.session == null) {
            this.env = OrtEnvironment.getEnvironment();
            if (modeloBytes != null) {
                this.session = this.env.createSession(modeloBytes, new OrtSession.SessionOptions());
            } else {
                this.session = this.env.createSession(this.direccionIA, new OrtSession.SessionOptions());
            }
        }
    }

    public ResultadoPrediccion predecir(IPerfilDeConsumo perfilDeConsumo) {
        Map<String, OnnxTensor> feeds = null;
        try {
            if (this.session == null) {
                cargarModelo();
            }

            feeds = perfilDeConsumo.preprocesamiento(this.env);

            try (OrtSession.Result resultados = session.run(feeds)) {
                return perfilDeConsumo.posprocesamiento(resultados);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error al ejecutar el modelo ONNX: " + e.getMessage(), e);
        } finally {
            if (feeds != null) {
                for (OnnxTensor tensor : feeds.values()) {
                    tensor.close();
                }
            }
        }
    }

    @Override
    public void close() {
        if (session != null) {
            try {
                session.close();
            } catch (OrtException e) {
                throw new IllegalStateException("Error al cerrar la sesion ONNX", e);
            } finally {
                session = null;
            }
        }
    }
}
