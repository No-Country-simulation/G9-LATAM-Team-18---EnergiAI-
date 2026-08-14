package com.energiai.energiaiapi.service.inference;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.onnx.EnergiA;
import com.energiai.energiaiapi.onnx.PerfilXgboostV1;
import com.energiai.energiaiapi.onnx.ResultadoPrediccion;
import com.energiai.energiaiapi.onnx.XgboostFeatureEncoder;
import com.energiai.energiaiapi.onnx.XgboostFeatures;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Clasificador principal: {@code modelo_xgboost_v2.onnx} (22 features, 3 clases).
 * Activo cuando {@code app.modelo.estrategia=onnx} (default).
 * {@code modelo_xgboost.onnx} (v1) y {@code version2.0.onnx} quedan como legacy / rollback.
 */
@Service
@ConditionalOnProperty(name = "app.modelo.estrategia", havingValue = "onnx", matchIfMissing = true)
public class ClasificadorOnnxAdapter implements ClasificadorPort {

    private static final Logger log = LoggerFactory.getLogger(ClasificadorOnnxAdapter.class);

    private final EnergiA energia;
    private final XgboostFeatureEncoder encoder;
    private final String version;

    public ClasificadorOnnxAdapter(
            XgboostFeatureEncoder encoder,
            @Value("${app.modelo.onnx-ruta:classpath:model/modelo_xgboost_v2.onnx}") Resource recursoOnnx,
            @Value("${app.modelo.onnx-version:xgboost-v2}") String version) throws Exception {
        this.encoder = encoder;
        this.version = version;
        this.energia = crearEnergiA(recursoOnnx);
        this.energia.cargarModelo();
        log.info("Clasificador ONNX {} cargado desde {}", version, recursoOnnx.getDescription());
    }

    @Override
    public Clasificacion clasificar(FacturaDTO factura) {
        XgboostFeatures features = encoder.encodeFull(factura);
        ResultadoPrediccion resultado = energia.predecir(new PerfilXgboostV1(features.vector()));

        String etiqueta = resultado.getEtiqueta();
        if (etiqueta == null || etiqueta.isBlank()) {
            throw new IllegalStateException("El modelo ONNX no devolvio etiqueta de clase");
        }

        CategoriaEficiencia categoria = CategoriaEficiencia.desdeEtiqueta(etiqueta);
        Map<String, Double> probs = normalizarProbabilidades(resultado.getProbabilidadesPorClase());
        return new Clasificacion(categoria, probs);
    }

    public XgboostFeatures featuresDe(FacturaDTO factura) {
        return encoder.encodeFull(factura);
    }

    public String getVersion() {
        return version;
    }

    private Map<String, Double> normalizarProbabilidades(Map<String, Double> crudas) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (crudas == null) {
            return out;
        }
        for (Map.Entry<String, Double> e : crudas.entrySet()) {
            CategoriaEficiencia cat = CategoriaEficiencia.desdeEtiqueta(e.getKey());
            out.put(cat.getEtiqueta(), e.getValue());
        }
        for (CategoriaEficiencia c : CategoriaEficiencia.values()) {
            out.putIfAbsent(c.getEtiqueta(), 0.0);
        }
        return out;
    }

    private EnergiA crearEnergiA(Resource recurso) throws IOException {
        if (recurso.isFile()) {
            Path path = recurso.getFile().toPath();
            if (Files.exists(path)) {
                return new EnergiA(path.toAbsolutePath().toString());
            }
        }
        try (InputStream is = recurso.getInputStream()) {
            return new EnergiA(is.readAllBytes());
        }
    }

    @PreDestroy
    public void cerrar() {
        energia.close();
        log.debug("Sesion ONNX cerrada");
    }
}
