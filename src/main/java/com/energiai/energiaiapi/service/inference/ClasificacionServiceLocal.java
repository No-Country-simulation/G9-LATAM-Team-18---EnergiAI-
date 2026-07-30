package com.energiai.energiaiapi.service.inference;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.energiai.energiaiapi.dto.FacturaDTO;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fallback de inferencia en Java puro: reimplementa el softmax de la Regresion
 * Logistica multinomial a partir de modelo_energiai.json, reproduciendo el
 * predict_proba() de scikit-learn (contrato de datos, secciones 2 y 8).
 *
 * Pasos: encoding -> imputacion -> orden feature_order -> StandardScaler ->
 * combinacion lineal por clase -> softmax -> argmax.
 */
@Service
public class ClasificacionServiceLocal implements ClasificadorPort {

    private final ModeloParametros modelo;

    public ClasificacionServiceLocal(ModeloParametros modelo) {
        this.modelo = modelo;
    }

    @Override
    public Clasificacion clasificar(FacturaDTO f) {
        ModeloParametros.Parametros p = modelo.get();

        // 1-3. Construir el vector de 12 features en el orden del contrato, imputando faltantes.
        double[] x = construirVector(f, p);

        // 4. StandardScaler: z = (x - mean) / scale
        double[] z = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            double scale = p.scalerScale()[i] == 0 ? 1.0 : p.scalerScale()[i];
            z[i] = (x[i] - p.scalerMean()[i]) / scale;
        }

        // 5. Puntaje por clase: score_k = intercept_k + sum_i(coef_k_i * z_i)
        int numClases = p.classes().size();
        double[] scores = new double[numClases];
        for (int k = 0; k < numClases; k++) {
            double s = p.intercept()[k];
            for (int i = 0; i < z.length; i++) {
                s += p.coef()[k][i] * z[i];
            }
            scores[k] = s;
        }

        // 6. Softmax (estable numericamente restando el maximo)
        double[] probs = softmax(scores);

        // 7. Argmax + mapeo de probabilidades por etiqueta de clase
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
            // Imputacion de opcionales faltantes con la media del entrenamiento.
            x[i] = (valor != null) ? valor : p.scalerMean()[i];
        }
        return x;
    }

    /** Traduce cada feature del contrato a su valor numerico (o null si falta). */
    private Double valorCrudo(String feature, FacturaDTO f, ModeloParametros.Parametros p) {
        return switch (feature) {
            case "consumo_kwh" -> f.consumoKwh() == null ? null : f.consumoKwh().doubleValue();
            case "uso_horario_pico_int" -> boolAInt(f.usoHorarioPico());
            case "cantidad_equipos" -> f.cantidadEquipos() == null ? null : f.cantidadEquipos().doubleValue();
            case "tipo_inmueble_enc" -> codificar(p.tipoInmuebleEncoding(), f.tipoInmueble());
            case "horas_alto_consumo" -> f.horasAltoConsumo();
            case "area_inmueble" -> f.areaInmueble();
            case "numero_personas" -> f.numeroPersonas() == null ? null : f.numeroPersonas().doubleValue();
            case "tiene_aire_acondicionado_int" -> boolAInt(f.tieneAireAcondicionado());
            case "tiene_calentador_electrico_int" -> boolAInt(f.tieneCalentadorElectrico());
            case "tiene_iluminacion_led_int" -> boolAInt(f.tieneIluminacionLed());
            case "antiguedad_electrodomesticos_enc" -> codificar(p.antiguedadEncoding(), f.antiguedadElectrodomesticos());
            case "tarifa_electrica" -> f.tarifaElectrica();
            default -> null;
        };
    }

    private Double boolAInt(Boolean b) {
        return b == null ? null : (b ? 1.0 : 0.0);
    }

    private Double codificar(Map<String, Integer> encoding, String clave) {
        if (clave == null || encoding == null) {
            return null;
        }
        Integer code = encoding.get(clave);
        return code == null ? null : code.doubleValue();
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
