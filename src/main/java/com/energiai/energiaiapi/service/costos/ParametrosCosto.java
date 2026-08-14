package com.energiai.energiaiapi.service.costos;

import com.energiai.energiaiapi.domain.enums.AntiguedadElectrodomesticos;
import com.energiai.energiaiapi.domain.enums.EstacionAnio;
import com.energiai.energiaiapi.domain.enums.TipoInmueble;
import com.energiai.energiaiapi.util.TextoNormalizado;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Carga los parametros de costos derivados de {@code Datasetup FINAL FINAL.xlsx}
 * (hoja Parametros para recargos Q..AJ; hoja metricas_final para el benchmark).
 * Se lee una sola vez al arrancar. La clave de estacion / tipo de inmueble es el nombre del
 * enum en minusculas y sin acentos (primavera, verano, otono, invierno / casa, departamento,
 * monoambiente).
 *
 * <p>El benchmark de consumo usa por default los umbrales de {@code metricas_final}
 * ({@code app.costos.umbrales=metricas_final}). Rollback a {@code Parametros!B10:E28}:
 * {@code APP_COSTOS_UMBRALES=parametros}. El alias {@code metricas} se acepta como
 * equivalente de {@code metricas_final}.
 */
@Component
public class ParametrosCosto {

    static final String FUENTE_METRICAS_FINAL = "metricas_final";
    static final String FUENTE_PARAMETROS = "parametros";

    private final Parametros parametros;
    private final String fuenteUmbrales;
    private final Map<String, Map<String, Umbral>> umbralesActivos;

    /** Constructor de tests: umbrales de la hoja metricas_final. */
    public ParametrosCosto(ObjectMapper mapper, Resource recurso) throws IOException {
        this(mapper, recurso, FUENTE_METRICAS_FINAL);
    }

    @Autowired
    public ParametrosCosto(ObjectMapper mapper,
                           @Value("${app.costos.ruta:classpath:model/parametros_costos.json}") Resource recurso,
                           @Value("${app.costos.umbrales:metricas_final}") String fuenteUmbrales)
            throws IOException {
        try (InputStream is = recurso.getInputStream()) {
            this.parametros = mapper.readValue(is, Parametros.class);
        }
        this.fuenteUmbrales = normalizarFuente(fuenteUmbrales);
        this.umbralesActivos = umbralesDe(this.fuenteUmbrales);
        validarInvariantes();
    }

    public String version() {
        return parametros.version();
    }

    /** {@code metricas_final} (default, confirmado) o {@code parametros} (rollback). */
    public String fuenteUmbrales() {
        return fuenteUmbrales;
    }

    /** Recargo por estacion (0..1). 0 si la estacion no se pudo resolver. */
    public double recargoEstacional(EstacionAnio estacion) {
        if (estacion == null) {
            return 0.0;
        }
        return parametros.recargoEstacional().getOrDefault(clave(estacion), 0.0);
    }

    /** Suma de los cuatro recargos estacionales (celda D36 de la planilla). */
    public double recargoEstacionalAnual() {
        return parametros.recargoEstacionalAnual();
    }

    public double recargoHorarioPico() {
        return parametros.recargosAccionables().horarioPico();
    }

    public double recargoSinIluminacionLed() {
        return parametros.recargosAccionables().sinIluminacionLed();
    }

    public double recargoElectrodomesticosAntiguos() {
        return parametros.recargosAccionables().electrodomesticosAntiguos();
    }

    /**
     * True si la antiguedad informada cae en un bucket con recargo. La planilla penaliza
     * "mayor a 5 años"; en el contrato de la API eso equivale a "menor a 10 años" y
     * "mayor a 10 años" (ambos superan los 5).
     */
    public boolean antiguedadConRecargo(String antiguedad) {
        List<String> conRecargo = parametros.antiguedadConRecargo();
        if (conRecargo == null || conRecargo.isEmpty()) {
            return false;
        }
        Optional<AntiguedadElectrodomesticos> resuelta = AntiguedadElectrodomesticos.desde(antiguedad);
        if (resuelta.isEmpty()) {
            return false;
        }
        String normalizada = TextoNormalizado.de(resuelta.get().getValor());
        return conRecargo.stream().anyMatch(v -> TextoNormalizado.de(v).equals(normalizada));
    }

    /** Umbrales de consumo (kWh) del benchmark informativo; empty si falta el tipo o la estacion. */
    public Optional<Umbral> umbral(TipoInmueble tipo, EstacionAnio estacion) {
        if (tipo == null || estacion == null) {
            return Optional.empty();
        }
        Map<String, Umbral> porEstacion = umbralesActivos.get(clave(tipo));
        return porEstacion == null ? Optional.empty() : Optional.ofNullable(porEstacion.get(clave(estacion)));
    }

    private Map<String, Map<String, Umbral>> umbralesDe(String fuente) {
        if (FUENTE_PARAMETROS.equals(fuente)) {
            if (parametros.umbralesConsumoKwhParametros() == null) {
                throw new IllegalStateException(
                        "parametros_costos.json: falta umbrales_consumo_kwh_parametros (rollback)");
            }
            return parametros.umbralesConsumoKwhParametros();
        }
        if (parametros.umbralesConsumoKwh() == null) {
            throw new IllegalStateException("parametros_costos.json: falta umbrales_consumo_kwh");
        }
        return parametros.umbralesConsumoKwh();
    }

    private static String normalizarFuente(String fuente) {
        String n = fuente == null ? FUENTE_METRICAS_FINAL : fuente.trim().toLowerCase(Locale.ROOT);
        if ("metricas".equals(n)) {
            n = FUENTE_METRICAS_FINAL;
        }
        if (!FUENTE_METRICAS_FINAL.equals(n) && !FUENTE_PARAMETROS.equals(n)) {
            throw new IllegalStateException(
                    "app.costos.umbrales debe ser 'metricas_final' o 'parametros', no: " + fuente);
        }
        return n;
    }

    /** Falla el arranque si el JSON no cubre las 4 estaciones y los 3 tipos de inmueble. */
    private void validarInvariantes() {
        if (parametros.recargoEstacional() == null) {
            throw new IllegalStateException("parametros_costos.json: falta recargo_estacional");
        }
        for (EstacionAnio e : EstacionAnio.values()) {
            if (!parametros.recargoEstacional().containsKey(clave(e))) {
                throw new IllegalStateException(
                        "parametros_costos.json: recargo_estacional sin la estacion " + clave(e));
            }
        }
        if (parametros.recargosAccionables() == null) {
            throw new IllegalStateException("parametros_costos.json: falta recargos_accionables");
        }
        validarUmbrales("umbrales_consumo_kwh", parametros.umbralesConsumoKwh());
        validarUmbrales("umbrales_consumo_kwh_parametros", parametros.umbralesConsumoKwhParametros());
    }

    private static void validarUmbrales(String nombre, Map<String, Map<String, Umbral>> umbrales) {
        if (umbrales == null) {
            throw new IllegalStateException("parametros_costos.json: falta " + nombre);
        }
        for (TipoInmueble t : TipoInmueble.values()) {
            Map<String, Umbral> porEstacion = umbrales.get(clave(t));
            if (porEstacion == null) {
                throw new IllegalStateException(
                        "parametros_costos.json: " + nombre + " sin el tipo " + clave(t));
            }
            for (EstacionAnio e : EstacionAnio.values()) {
                if (!porEstacion.containsKey(clave(e))) {
                    throw new IllegalStateException("parametros_costos.json: " + nombre + "."
                            + clave(t) + " sin la estacion " + clave(e));
                }
            }
        }
    }

    private static String clave(Enum<?> valor) {
        return valor.name().toLowerCase(Locale.ROOT);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Parametros(
            String version,
            String origen,
            @JsonProperty("base_aumento") double baseAumento,
            @JsonProperty("consumo_referencia_kwh") Map<String, Double> consumoReferenciaKwh,
            @JsonProperty("recargo_estacional") Map<String, Double> recargoEstacional,
            @JsonProperty("recargo_estacional_anual") double recargoEstacionalAnual,
            @JsonProperty("recargos_accionables") RecargosAccionables recargosAccionables,
            @JsonProperty("antiguedad_con_recargo") List<String> antiguedadConRecargo,
            @JsonProperty("umbrales_consumo_kwh") Map<String, Map<String, Umbral>> umbralesConsumoKwh,
            @JsonProperty("umbrales_consumo_kwh_parametros") Map<String, Map<String, Umbral>> umbralesConsumoKwhParametros
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecargosAccionables(
            @JsonProperty("horario_pico") double horarioPico,
            @JsonProperty("sin_iluminacion_led") double sinIluminacionLed,
            @JsonProperty("electrodomesticos_antiguos") double electrodomesticosAntiguos
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Umbral(
            @JsonProperty("eficiente_hasta") double eficienteHasta,
            @JsonProperty("moderado_hasta") double moderadoHasta
    ) {
    }
}
