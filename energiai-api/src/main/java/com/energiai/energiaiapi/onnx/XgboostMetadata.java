package com.energiai.energiaiapi.onnx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Carga {@code metadata_backend.json}: orden de las 22 columnas ONNX y medias por tipo
 * para las features sinteticas.
 */
@Component
public class XgboostMetadata {

    private final Metadata datos;

    public XgboostMetadata(
            ObjectMapper mapper,
            @Value("${app.modelo.xgboost-metadata-ruta:classpath:model/metadata_backend.json}") Resource recurso)
            throws IOException {
        try (InputStream is = recurso.getInputStream()) {
            JsonNode root = mapper.readTree(is);
            Map<String, Double> medias = new LinkedHashMap<>();
            JsonNode mediasNode = root.path("diccionario_medias_equipos");
            Iterator<String> names = mediasNode.fieldNames();
            while (names.hasNext()) {
                String key = names.next();
                medias.put(key.toLowerCase(Locale.ROOT), mediasNode.get(key).asDouble());
            }

            List<String> orden = new ArrayList<>();
            for (JsonNode col : root.path("orden_columnas_transformadas")) {
                orden.add(col.asText());
            }
            if (orden.size() != 22) {
                throw new IllegalStateException(
                        "metadata_backend.json debe declarar exactamente 22 columnas; hay " + orden.size());
            }

            this.datos = new Metadata(Collections.unmodifiableMap(medias), List.copyOf(orden));
        }
    }

    public Metadata get() {
        return datos;
    }

    public record Metadata(Map<String, Double> mediasEquiposPorTipo, List<String> ordenColumnas) {
    }
}
