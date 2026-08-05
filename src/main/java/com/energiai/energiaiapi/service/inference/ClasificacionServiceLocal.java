package com.energiai.energiaiapi.service.inference;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.energiai.energiaiapi.dto.FacturaDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fallback de inferencia en Java puro: softmax leyendo modelo_energiai.json.
 * Activo solo cuando {@code app.modelo.estrategia=local}.
 */
@Service
@ConditionalOnProperty(name = "app.modelo.estrategia", havingValue = "local")
public class ClasificacionServiceLocal implements ClasificadorPort {

    private final ModeloParametros modelo;

    public ClasificacionServiceLocal(ModeloParametros modelo) {
        this.modelo = modelo;
    }

    @Override
    public Clasificacion clasificar(FacturaDTO factura) {
        FacturaDTO f = factura.canonicalizada();
        ModeloParametros.Parametros p = modelo.get();

        double[] x = construirVector(f, p);

        double[] z = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            double scale = p.scalerScale()[i] == 0 ? 1.0 : p.scalerScale()[i];
            z[i] = (x[i] - p.scalerMean()[i]) / scale;
        }

        int numClases = p.classes().size();
        double[] scores = new double[numClases];
        for (int k = 0; k < numClases; k++) {
            double s = p.intercept()[k];
            for (int i = 0; i < z.length; i++) {
                s += p.coef()[k][i] * z[i];
            }
            scores[k] = s;
        }

        double[] probs = softmax(scores);

        int ganador = 0;
        Map<String, Double> probabilidades = new LinkedHashMap<>();
        for (int k = 0; k < numClases; k++) {
            probabilidades.put(p.classes().get(k), probs[k]);
            if (probs[k] > probs[ganador]) {
                ganador = k;
            }
        }

        CategoriaEficiencia categoria = CategoriaEficiencia.desdeEtiqueta(p.classes().get(ganador));
        return new Clasificacion(categoria, probabilidades);
    }

    private double[] construirVector(FacturaDTO f, ModeloParametros.Parametros p) {
        List<String> orden = p.featureOrder();
        double[] x = new double[orden.size()];
        for (int i = 0; i < orden.size(); i++) {
            Double valor = valorCrudo(orden.get(i), f, p);
            x[i] = (valor != null) ? valor : p.scalerMean()[i];
        }
        return x;
    }

    private Double valorCrudo(String feature, FacturaDTO f, ModeloParametros.Parametros p) {
        return switch (feature) {
            case "consumo_mensual", "consumo_kwh" -> f.consumoMensual();
            case "uso_horario_pico_int" -> boolAInt(f.usoHorarioPico());
            case "cantidad_equipos" -> f.cantidadEquipos() == null ? null : f.cantidadEquipos().doubleValue();
            case "tipo_inmueble_enc" -> {
                if (f.tipoInmueble() == null || f.tipoInmueble().isBlank()) {
                    throw new IllegalArgumentException(
                            "tipoInmueble es obligatorio para el modelo");
                }
                Double code = codificar(p.tipoInmuebleEncoding(), f.tipoInmueble());
                if (code == null) {
                    throw new IllegalArgumentException(
                            "tipoInmueble debe ser: monoambiente, departamento o casa");
                }
                yield code;
            }
            case "horas_promedio_uso", "horas_alto_consumo" -> f.horasPromedioUso();
            case "estacion_anio_enc" -> codificar(p.estacionAnioEncoding(), f.estacionAnio());
            case "numero_personas" -> f.numeroPersonas() == null ? null : f.numeroPersonas().doubleValue();
            case "tiene_aire_acondicionado_int" -> boolAInt(f.tieneAireAcondicionado());
            case "tiene_calentador_int", "tiene_calentador_electrico_int" -> boolAInt(f.tieneCalentador());
            case "tiene_iluminacion_led_int" -> boolAInt(f.tieneIluminacionLed());
            case "antiguedad_electrodomesticos_enc" -> codificar(p.antiguedadEncoding(), f.antiguedadElectrodomesticos());
            case "tarifa_electrica" -> f.tarifaElectrica();
            default -> null;
        };
    }

    private Double boolAInt(Boolean b) {
        return b == null ? null : (b ? 1.0 : 0.0);
    }

    /** Busca el codigo en el mapa de encoding de forma case/acento-insensitive. */
    private Double codificar(Map<String, Integer> encoding, String clave) {
        if (clave == null || encoding == null) {
            return null;
        }
        Integer directo = encoding.get(clave);
        if (directo != null) {
            return directo.doubleValue();
        }
        String n = normalizar(clave);
        for (Map.Entry<String, Integer> e : encoding.entrySet()) {
            if (normalizar(e.getKey()).equals(n)) {
                return e.getValue().doubleValue();
            }
        }
        return null;
    }

    private static String normalizar(String s) {
        return s.trim().toLowerCase(Locale.ROOT)
                .replace('á', 'a').replace('é', 'e').replace('í', 'i')
                .replace('ó', 'o').replace('ú', 'u').replace('ñ', 'n');
    }

    private double[] softmax(double[] scores) {
        double max = Double.NEGATIVE_INFINITY;
        for (double s : scores) {
            max = Math.max(max, s);
        }
        double suma = 0.0;
        double[] exp = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            exp[i] = Math.exp(scores[i] - max);
            suma += exp[i];
        }
        double[] probs = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            probs[i] = exp[i] / suma;
        }
        return probs;
    }
}
