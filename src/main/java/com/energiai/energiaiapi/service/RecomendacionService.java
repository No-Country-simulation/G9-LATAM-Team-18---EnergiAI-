package com.energiai.energiaiapi.service;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.service.recomendacion.ContextoRecomendacion;
import com.energiai.energiaiapi.service.recomendacion.GeminiRecomendacionClient;
import com.energiai.energiaiapi.service.recomendacion.ReglasRecomendacion;
import com.energiai.energiaiapi.service.recomendacion.TemaRecomendacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Recomendaciones hibridas:
 * <ul>
 *   <li>{@code reglas}: textos base deterministas</li>
 *   <li>{@code hibrido} / {@code gemini}: Gemini reformula el set cerrado; si falla → reglas</li>
 * </ul>
 */
@Service
public class RecomendacionService {

    private static final Logger log = LoggerFactory.getLogger(RecomendacionService.class);

    private final ReglasRecomendacion reglas;
    private final GeminiRecomendacionClient gemini;
    private final String modo;
    private final int maxItems;

    public RecomendacionService(
            ReglasRecomendacion reglas,
            GeminiRecomendacionClient gemini,
            @Value("${app.recomendaciones.modo:hibrido}") String modo,
            @Value("${app.recomendaciones.max-items:3}") int maxItems) {
        this.reglas = reglas;
        this.gemini = gemini;
        this.modo = modo == null ? "hibrido" : modo.trim().toLowerCase();
        this.maxItems = Math.max(1, maxItems);
    }

    public List<String> generar(FacturaDTO f, CategoriaEficiencia categoria) {
        return generar(f, categoria, ContextoRecomendacion.vacio());
    }

    /**
     * @param contexto costos con estacionalidad + comparativa de historial; vacio en modo invitado
     */
    public List<String> generar(FacturaDTO f, CategoriaEficiencia categoria, ContextoRecomendacion contexto) {
        ContextoRecomendacion ctx = contexto == null ? ContextoRecomendacion.vacio() : contexto;
        List<TemaRecomendacion> temas = reglas.seleccionar(f, categoria, ctx);
        List<String> base = textosBase(temas);

        if ("reglas".equals(modo)) {
            return limitar(base);
        }

        // hibrido | gemini
        if (!gemini.isEnabled()) {
            log.debug("Recomendaciones: Gemini deshabilitado (sin API key); usando reglas");
            return limitar(base);
        }

        return gemini.reformular(f, categoria, temas, maxItems, ctx)
                .map(this::limitar)
                .orElseGet(() -> {
                    log.debug("Recomendaciones: fallback a textos base de reglas");
                    return limitar(base);
                });
    }

    private List<String> textosBase(List<TemaRecomendacion> temas) {
        return temas.stream().map(TemaRecomendacion::textoBase).toList();
    }

    private List<String> limitar(List<String> items) {
        if (items.size() <= maxItems) {
            return List.copyOf(items);
        }
        return List.copyOf(items.subList(0, maxItems));
    }
}
