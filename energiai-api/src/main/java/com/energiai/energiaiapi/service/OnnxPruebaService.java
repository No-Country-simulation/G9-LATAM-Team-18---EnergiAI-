package com.energiai.energiaiapi.service;

import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.dto.OnnxPruebaResponse;
import com.energiai.energiaiapi.onnx.XgboostFeatureEncoder;
import com.energiai.energiaiapi.service.inference.ClasificadorOnnxAdapter;
import com.energiai.energiaiapi.service.inference.ClasificadorPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

/**
 * Prueba aislada de inferencia ONNX xgboost (v2 por defecto) con el mismo body de factura que /api/analisis,
 * sin persistir ni calcular costo/recomendaciones.
 */
@Service
@ConditionalOnBean(ClasificadorOnnxAdapter.class)
public class OnnxPruebaService {

    private final ClasificadorPort clasificador;
    private final XgboostFeatureEncoder encoder;
    private final ClasificadorOnnxAdapter onnxAdapter;

    public OnnxPruebaService(ClasificadorPort clasificador,
                             XgboostFeatureEncoder encoder,
                             ClasificadorOnnxAdapter onnxAdapter) {
        this.clasificador = clasificador;
        this.encoder = encoder;
        this.onnxAdapter = onnxAdapter;
    }

    public OnnxPruebaResponse predecir(FacturaDTO factura) {
        FacturaDTO f = factura.canonicalizada();
        float[] vector = encoder.encode(f);
        ClasificadorPort.Clasificacion c = clasificador.clasificar(f);
        return new OnnxPruebaResponse(
                c.categoria(),
                c.probabilidades(),
                onnxAdapter.getVersion(),
                "BACKEND_ONNX",
                vector);
    }
}
