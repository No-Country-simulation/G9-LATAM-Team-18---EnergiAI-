package com.energiai.energiaiapi.service.inference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Carga y expone los parametros del contrato modelo_energiai.json (seccion 2 del
 * contrato de datos). Se lee una sola vez al arrancar la app.
 */
@Component
public class ModeloParametros {

    private final Parametros parametros;

    public ModeloParametros(ObjectMapper mapper,
                            @Value("${app.modelo.ruta:classpath:model/modelo_energiai.json}") Resource recurso)
            throws IOException {
        try (InputStream is = recurso.getInputStream()) {
            this.parametros = mapper.readValue(is, Parametros.class);
        }
        validarInvariantes();
    }

    /** Invariantes de la seccion 5 del contrato. Falla el arranque si el JSON esta mal formado. */
    private void validarInvariantes() {
        int n = parametros.featureOrder().size();
        if (n != 12) {
            throw new IllegalStateException("feature_order debe tener 12 elementos, tiene " + n);
        }
        if (parametros.classes().size() != parametros.coef().length
                || parametros.coef().length != parametros.intercept().length) {
            throw new IllegalStateException("classes, coef e intercept deben tener la misma cantidad de clases");
        }
        for (double[] fila : parametros.coef()) {
            if (fila.length != n) {
                throw new IllegalStateException("Cada fila de coef debe tener 12 columnas");
            }
        }
        if (parametros.scalerMean().length != n || parametros.scalerScale().length != n) {
            throw new IllegalStateException("scaler_mean y scaler_scale deben tener 12 elementos");
        }
    }

    public Parametros get() {
        return parametros;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Parametros(
            String modelo,
            String version,
            @JsonProperty("feature_order") List<String> featureOrder,
            List<String> classes,
            double[][] coef,
            double[] intercept,
            @JsonProperty("scaler_mean") double[] scalerMean,
            @JsonProperty("scaler_scale") double[] scalerScale,
            @JsonProperty("tipo_inmueble_encoding") Map<String, Integer> tipoInmuebleEncoding,
            @JsonProperty("estacion_anio_encoding") Map<String, Integer> estacionAnioEncoding,
            @JsonProperty("antiguedad_electrodomesticos_encoding") Map<String, Integer> antiguedadEncoding,
            @JsonProperty("tarifa_referencia_kwh") double tarifaReferenciaKwh
    ) {
    }
}
