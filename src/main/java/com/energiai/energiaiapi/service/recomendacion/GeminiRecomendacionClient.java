package com.energiai.energiaiapi.service.recomendacion;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.energiai.energiaiapi.dto.CostosEstacionalesDTO;
import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.dto.ProyeccionEstacionalDTO;
import com.energiai.energiaiapi.dto.ResumenHistorialDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reformula recomendaciones con Gemini a partir de un set cerrado de textos base.
 * Si falla (sin key, timeout, JSON invalido), el caller usa el fallback de reglas.
 */
@Component
public class GeminiRecomendacionClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiRecomendacionClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final boolean enabled;

    public GeminiRecomendacionClient(
            ObjectMapper objectMapper,
            @Value("${app.recomendaciones.gemini.api-key:}") String apiKey,
            @Value("${app.recomendaciones.gemini.modelo:gemini-2.5-flash-lite}") String model,
            @Value("${app.recomendaciones.gemini.timeout-ms:8000}") long timeoutMs) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.enabled = !this.apiKey.isBlank();

        ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(Duration.ofMillis(Math.min(timeoutMs, 2000)))
                        .withReadTimeout(Duration.ofMillis(timeoutMs)));
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .requestFactory(factory)
                .build();

        if (this.enabled) {
            log.info("Gemini recomendaciones habilitado: modelo={} timeoutMs={} (modo hibrido reformula la frase patron)",
                    model, timeoutMs);
        } else {
            log.warn("Gemini recomendaciones deshabilitado: falta GEMINI_API_KEY; se usaran solo reglas");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return lista reformulada, o empty si no se pudo obtener una respuesta usable
     */
    public Optional<List<String>> reformular(
            FacturaDTO factura,
            CategoriaEficiencia categoria,
            List<TemaRecomendacion> temas,
            int maxItems) {
        return reformular(factura, categoria, temas, maxItems, ContextoRecomendacion.vacio());
    }

    /**
     * Variante para el usuario registrado: agrega al prompt el desglose de costos y la
     * comparativa con su historial para que la redaccion sea especifica y con cifras reales.
     *
     * @return lista reformulada, o empty si no se pudo obtener una respuesta usable
     */
    public Optional<List<String>> reformular(
            FacturaDTO factura,
            CategoriaEficiencia categoria,
            List<TemaRecomendacion> temas,
            int maxItems,
            ContextoRecomendacion contexto) {
        if (!enabled || temas == null || temas.isEmpty()) {
            return Optional.empty();
        }

        try {
            String prompt = construirPrompt(factura, categoria, temas, maxItems,
                    contexto == null ? ContextoRecomendacion.vacio() : contexto);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("contents", List.of(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", prompt)))));
            body.put("generationConfig", Map.of(
                    "temperature", 0.35,
                    // 512 alcanza para 3 frases; valores altos en modelos con "thinking" se comen el presupuesto y el timeout.
                    "maxOutputTokens", 512,
                    "responseMimeType", "application/json"));

            String raw = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE + ",*/*")
                    .body(body)
                    .exchange((request, response) -> {
                        byte[] bytes = response.getBody().readAllBytes();
                        String payload = new String(bytes, StandardCharsets.UTF_8);
                        if (response.getStatusCode().isError()) {
                            throw new IllegalStateException(
                                    "Gemini HTTP " + response.getStatusCode().value() + ": "
                                            + payload.substring(0, Math.min(payload.length(), 300)));
                        }
                        return payload;
                    });

            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }
            return parseRespuesta(raw, maxItems);
        } catch (Exception e) {
            log.warn("Gemini recomendaciones no disponible (fallback a reglas): {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String construirPrompt(
            FacturaDTO f,
            CategoriaEficiencia categoria,
            List<TemaRecomendacion> temas,
            int maxItems,
            ContextoRecomendacion contexto) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                Eres un asesor de eficiencia energetica para el hogar en Latinoamerica.
                Reformula en espanol latinoamericano neutro (claro, breve, tono cercano) las recomendaciones base.
                Reglas de idioma (obligatorias):
                - Usa tuteo estandar: "evita", "configura", "mantén", "reduce", "considera", "pon".
                - PROHIBIDO el voseo: no uses "evitá", "poné", "mantené", "hacé", "tené", "fijate", "anda".
                - Evita regionalismos de un solo pais; usa vocabulario panlatino
                  (factura, aire acondicionado, consumo, horario pico).
                Reglas de contenido:
                - NO inventes temas nuevos ni datos que no esten en el contexto.
                - Conserva el sentido de cada texto base.
                - Devuelve SOLO un JSON array de strings (sin markdown), maximo %d items.
                - Una frase por item, maximo ~140 caracteres.
                """.formatted(maxItems));
        sb.append("\nContexto factual:\n");
        sb.append("- categoria_eficiencia: ").append(categoria.getEtiqueta()).append('\n');
        sb.append("- tipo_inmueble: ").append(f.tipoInmueble()).append('\n');
        sb.append("- month: ").append(f.mes()).append('\n');
        sb.append("- uso_horario_pico: ").append(Boolean.TRUE.equals(f.usoHorarioPico()) ? "si" : "no").append('\n');
        sb.append("- horas_alto_consumo: ").append(f.horasPromedioUso()).append('\n');
        sb.append("- cantidad_equipos: ").append(f.cantidadEquipos()).append('\n');
        sb.append("- consumo_mensual_kwh: ").append(f.consumoMensual()).append('\n');

        if (contexto != null && !contexto.vacia()) {
            agregarContextoRegistrado(sb, contexto);
        }

        sb.append("\nTextos base (temas cerrados):\n");
        for (TemaRecomendacion t : temas) {
            sb.append("- [").append(t.codigo()).append("] ").append(t.textoBase()).append('\n');
        }
        sb.append("\nJSON de salida esperado: [\"...\", \"...\"]\n");
        return sb.toString();
    }

    /**
     * Bloque exclusivo del usuario registrado: costos con estacionalidad y comparativa contra su
     * historial. Las cifras van pre-formateadas para que el modelo solo tenga que citarlas.
     */
    private void agregarContextoRegistrado(StringBuilder sb, ContextoRecomendacion contexto) {
        if (contexto.tieneCostos()) {
            CostosEstacionalesDTO c = contexto.costos();
            sb.append("\nContexto de costos (estacionalidad y recargos):\n");
            sb.append("- estacion_actual: ").append(c.estacion()).append('\n');
            sb.append("- recargo_estacional: ").append(Cifras.pct(c.pctEstacional())).append('\n');
            sb.append("- costo_base_mensual: ").append(Cifras.usd(c.costoBrutoMensual())).append('\n');
            sb.append("- costo_ajustado_mensual: ").append(Cifras.usd(c.costoAjustadoMensual())).append('\n');
            sb.append("- ahorro_potencial_mensual: ").append(Cifras.usd(c.ahorroPotencialMensual())).append('\n');
            sb.append("- ahorro_potencial_anual: ").append(Cifras.usd(c.ahorroPotencialAnual())).append('\n');
            ProyeccionEstacionalDTO masCara = c.estacionMasCara();
            if (masCara != null) {
                sb.append("- estacion_mas_cara: ").append(masCara.estacion())
                        .append(" (").append(Cifras.usd(masCara.costoMensualEstimado()))
                        .append(" por mes)\n");
            }
        }

        if (contexto.tieneHistorial()) {
            ResumenHistorialDTO h = contexto.historial();
            sb.append("\nContexto de historial del usuario:\n");
            sb.append("- analisis_previos: ").append(h.analisisPrevios()).append('\n');
            if (h.consumoPromedioKwh() != null) {
                sb.append("- consumo_promedio_previo: ").append(Cifras.kwh(h.consumoPromedioKwh())).append('\n');
            }
            if (h.variacionConsumoPct() != null) {
                sb.append("- variacion_consumo_vs_promedio: ")
                        .append(Cifras.pctVariacion(h.variacionConsumoPct())).append('\n');
            }
            if (h.variacionCostoPct() != null) {
                sb.append("- variacion_costo_vs_promedio: ")
                        .append(Cifras.pctVariacion(h.variacionCostoPct())).append('\n');
            }
            sb.append("- tendencia: ").append(h.tendencia()).append('\n');
            if (h.analisisMismaEstacion() > 0 && h.consumoPromedioMismaEstacionKwh() != null) {
                sb.append("- consumo_promedio_misma_estacion: ")
                        .append(Cifras.kwh(h.consumoPromedioMismaEstacionKwh()))
                        .append(" (").append(h.analisisMismaEstacion()).append(" registros en ")
                        .append(h.estacion()).append(")\n");
            }
            if (h.variacionVsMismaEstacionPct() != null) {
                sb.append("- variacion_vs_misma_estacion: ")
                        .append(Cifras.pctVariacion(h.variacionVsMismaEstacionPct())).append('\n');
            }
        }

        sb.append("""

                Reglas adicionales para este usuario (tiene historial):
                - Puedes citar la estacion, el ahorro potencial y la variacion del historial,
                  SOLO con las cifras exactas listadas arriba (no calcules ni estimes otras).
                - Personaliza al menos un item con la comparativa del historial o la estacionalidad.
                - No repitas la misma cifra en dos items distintos.
                - Habla de la factura en dolares (USD) tal como aparece en el contexto.
                """);
    }

    private Optional<List<String>> parseRespuesta(String raw, int maxItems) throws Exception {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        JsonNode root = objectMapper.readTree(raw);
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        StringBuilder joined = new StringBuilder();
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                if (part.hasNonNull("text")) {
                    joined.append(part.get("text").asText());
                }
            }
        }
        String text = joined.toString().trim();
        if (text.isEmpty()) {
            return Optional.empty();
        }
        if (text.startsWith("```")) {
            int start = text.indexOf('\n');
            int end = text.lastIndexOf("```");
            if (start >= 0 && end > start) {
                text = text.substring(start + 1, end).trim();
            }
        }
        // Por si viene texto extra alrededor del array.
        int i = text.indexOf('[');
        int j = text.lastIndexOf(']');
        if (i >= 0 && j > i) {
            text = text.substring(i, j + 1);
        }
        List<String> items = objectMapper.readValue(text, new TypeReference<>() {
        });
        List<String> limpios = new ArrayList<>();
        for (String item : items) {
            if (item == null) {
                continue;
            }
            String s = item.trim();
            if (!s.isEmpty()) {
                limpios.add(s);
            }
            if (limpios.size() >= maxItems) {
                break;
            }
        }
        return limpios.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(limpios));
    }
}
