package com.energiai.energiaiapi.onnx;

import com.energiai.energiaiapi.domain.enums.Mes;
import com.energiai.energiaiapi.domain.enums.TipoInmueble;
import com.energiai.energiaiapi.dto.FacturaDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Codifica {@link FacturaDTO} al tensor float[22] segun {@code metadata_backend.json}.
 * Orden critico (contrato DS / modelo):
 * <ul>
 *   <li>{@code uso_horario_pico} one-hot {@code [no, si]}: no→[1,0], si→[0,1]</li>
 *   <li>numericos: {@code horas_alto_consumo} luego {@code cantidad_equipos}</li>
 *   <li>luego sinteticas A/B/C</li>
 * </ul>
 */
@Component
public class XgboostFeatureEncoder {

    private static final double EPS_EQUIPOS = 0.00001;

    private final XgboostMetadata metadata;

    public XgboostFeatureEncoder(XgboostMetadata metadata) {
        this.metadata = metadata;
    }

    public float[] encode(FacturaDTO factura) {
        return encodeFull(factura).vector();
    }

    public XgboostFeatures encodeFull(FacturaDTO factura) {
        FacturaDTO f = factura.canonicalizada();
        validarObligatorios(f);

        String tipoLabel = TipoInmueble.desde(f.tipoInmueble())
                .map(t -> Character.toUpperCase(t.getValor().charAt(0)) + t.getValor().substring(1))
                .orElseThrow(() -> new IllegalArgumentException("tipo_inmueble invalido: " + f.tipoInmueble()));

        int month = Mes.numeroDesde(f.mes())
                .orElseThrow(() -> new IllegalArgumentException(
                        "month es obligatorio (1-12): " + f.mes()));

        double horas = f.horasPromedioUso();
        double equipos = f.cantidadEquipos().doubleValue();
        boolean pico = Boolean.TRUE.equals(f.usoHorarioPico());
        String picoLabel = pico ? "si" : "no";

        // A. intensidad_por_equipo
        double intensidad = horas / (equipos + EPS_EQUIPOS);
        // B. horas_pico_interaccion
        double horasPico = pico ? horas : 0.0;
        // C. desviacion_equipos_tipo (medias desde metadata, no hardcode)
        Double media = metadata.get().mediasEquiposPorTipo().get(tipoLabel.toLowerCase(Locale.ROOT));
        if (media == null) {
            throw new IllegalArgumentException(
                    "No hay media_segun_tipo_inmueble para: " + tipoLabel
                            + " (revisar metadata_backend.json)");
        }
        double desviacion = equipos - media;

        List<String> orden = metadata.get().ordenColumnas();
        float[] vector = new float[orden.size()];
        for (int i = 0; i < orden.size(); i++) {
            vector[i] = evaluarFeature(
                    orden.get(i), tipoLabel, month, picoLabel, horas, equipos, intensidad, horasPico, desviacion);
        }
        return new XgboostFeatures(
                vector, intensidad, horasPico, desviacion,
                tipoLabel, month, picoLabel, horas, f.cantidadEquipos());
    }

    private static void validarObligatorios(FacturaDTO f) {
        if (f.usoHorarioPico() == null
                || f.cantidadEquipos() == null
                || f.horasPromedioUso() == null
                || f.tipoInmueble() == null || f.tipoInmueble().isBlank()
                || f.mes() == null || f.mes().isBlank()) {
            throw new IllegalArgumentException(
                    "El modelo requiere: uso_horario_pico, cantidad_equipos, tipo_inmueble, "
                            + "horas_alto_consumo y month");
        }
    }

    private static float evaluarFeature(
            String name,
            String tipoLabel,
            int month,
            String picoLabel,
            double horas,
            double equipos,
            double intensidad,
            double horasPico,
            double desviacion) {

        if (name.contains("cantidad_equipos")) {
            return (float) equipos;
        }
        if (name.contains("horas_alto_consumo")) {
            return (float) horas;
        }
        if (name.contains("intensidad_por_equipo")) {
            return (float) intensidad;
        }
        if (name.contains("horas_pico_interaccion")) {
            return (float) horasPico;
        }
        if (name.contains("desviacion_equipos_tipo")) {
            return (float) desviacion;
        }

        if (name.startsWith("cat__tipo_inmueble_")) {
            String opcion = name.substring("cat__tipo_inmueble_".length());
            return opcion.equalsIgnoreCase(tipoLabel) ? 1.0f : 0.0f;
        }
        if (name.startsWith("cat__month_")) {
            String opcion = name.substring("cat__month_".length());
            return opcion.equals(String.valueOf(month)) ? 1.0f : 0.0f;
        }
        if (name.startsWith("cat__uso_horario_pico_")) {
            String opcion = name.substring("cat__uso_horario_pico_".length());
            return opcion.equalsIgnoreCase(picoLabel) ? 1.0f : 0.0f;
        }
        return 0.0f;
    }
}
