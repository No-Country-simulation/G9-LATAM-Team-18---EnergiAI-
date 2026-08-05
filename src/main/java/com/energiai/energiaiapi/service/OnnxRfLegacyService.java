package com.energiai.energiaiapi.service;

import com.energiai.energiaiapi.dto.OnnxRfLegacyRequest;
import com.energiai.energiaiapi.dto.OnnxRfLegacyResponse;
import com.energiai.energiaiapi.onnx.EnergiA;
import com.energiai.energiaiapi.onnx.ResultadoPrediccion;
import com.energiai.energiaiapi.onnx.UnGranPerfil;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Servicio legacy para modelo_rf.onnx (10 features, clasificacion binaria 0/1).
 * Expone {@code POST /api/pruebas/onnx-rf}.
 */
@Service
public class OnnxRfLegacyService {

    private static final Logger log = LoggerFactory.getLogger(OnnxRfLegacyService.class);

    private final EnergiA energia;
    private final String modeloLabel;

    public OnnxRfLegacyService(
            @Value("${app.modelo.onnx-rf-ruta:classpath:model/modelo_rf.onnx}") Resource recursoOnnx)
            throws Exception {
        this.modeloLabel = recursoOnnx.getDescription();
        this.energia = crearEnergiA(recursoOnnx);
        this.energia.cargarModelo();
        log.info("Modelo ONNX RF legacy cargado desde {}", modeloLabel);
    }

    public OnnxRfLegacyResponse predecir(OnnxRfLegacyRequest request) {
        ResultadoPrediccion resultado = energia.predecir(new UnGranPerfil(request.aVector()));
        return new OnnxRfLegacyResponse(
                resultado.getPrediccion(),
                resultado.getProbabilidades(),
                modeloLabel);
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
    void cerrar() {
        energia.close();
    }
}
