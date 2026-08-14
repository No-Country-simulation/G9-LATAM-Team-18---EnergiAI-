package com.energiai.energiaiapi.service.inference;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import com.energiai.energiaiapi.dto.FacturaDTO;

import java.util.Map;

/**
 * Puerto de inferencia. Desacopla el resto de la app del "como" se clasifica.
 * Implementaciones posibles:
 *  - ClasificacionServiceLocal: softmax en Java leyendo modelo_energiai.json (fallback / validacion).
 *  - (futuro) ClasificadorRemotoAdapter: microservicio Python via REST.
 *  - (futuro) ClasificadorOnnxAdapter: ONNX Runtime for Java.
 *
 * En el flujo por defecto el frontend clasifica con ONNX y envia el resultado;
 * este puerto solo se usa cuando ese resultado no viene.
 */
public interface ClasificadorPort {

    Clasificacion clasificar(FacturaDTO factura);

    record Clasificacion(CategoriaEficiencia categoria, Map<String, Double> probabilidades) {
    }
}
