package com.energiai.energiaiapi.service.inference;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.onnx.EnergiA;
import com.energiai.energiaiapi.onnx.FacturaFeatureEncoder;
import com.energiai.energiaiapi.onnx.PerfilFacturaV2;
import com.energiai.energiaiapi.onnx.ResultadoPrediccion;
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
 * Clasificador principal: version2.0.onnx (Random Forest, 6 features, 3 clases).
 * Activo cuando {@code app.modelo.estrategia=onnx} (default).
 */
@Service
@ConditionalOnProperty(name = "app.modelo.estrategia", havingValue = "onnx", matchIfMissing = true)
public class ClasificadorOnnxAdapter implements ClasificadorPort {

    private static final Logger log = LoggerFactory.getLogger(ClasificadorOnnxAdapter.class);

    private final EnergiA energia;
    private final FacturaFeatureEncoder encoder;
    private final String version;

    public ClasificadorOnnxAdapter(
            FacturaFeatureEncoder encoder,
            @Value("${app.modelo.onnx-ruta:classpath:model/version2.0.onnx}") Resource recursoOnnx,
            @Value("${app.modelo.onnx-version:2.0}") String version) throws Exception {
        this.encoder = encoder;
        this.version = version;
        this.energia = crearEnergiA(recursoOnnx);
        this.energia.cargarModelo();
        log.info("Clasificador ONNX v{} cargado desde {}", version, recursoOnnx.getDescription());
    }

    @Override
    public Clasificacion clasificar(FacturaDTO factura) {
        float[] vector = encoder.encode(factura);
        ResultadoPrediccion resultado = energia.predecir(new PerfilFacturaV2(vector));

        String etiqueta = resultado.getEtiqueta();
        if (etiqueta == null || etiqueta.isBlank()) {
            throw new IllegalStateException("El modelo ONNX no devolvio etiqueta de clase");
        }

        CategoriaEficiencia categoria = CategoriaEficiencia.desdeEtiqueta(etiqueta);
        Map<String, Double> probs = normalizarProbabilidades(resultado.getProbabilidadesPorClase());
        return new Clasificacion(categoria, probs);
    }

    public String getVersion() {
        return version;
    }

    /**
     * Normaliza claves del ZipMap (eficiente/ineficiente/moderado) a las etiquetas
     * de negocio (Eficiente/Ineficiente/Moderado).
     */
    private Map<String, Double> normalizarProbabilidades(Map<String, Double> crudas) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (crudas == null) {
            return out;
        }
        for (Map.Entry<String, Double> e : crudas.entrySet()) {
            CategoriaEficiencia cat = CategoriaEficiencia.desdeEtiqueta(e.getKey());
            out.put(cat.getEtiqueta(), e.getValue());
        }
        // Garantiza las 3 claves aunque el modelo omita alguna.
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
