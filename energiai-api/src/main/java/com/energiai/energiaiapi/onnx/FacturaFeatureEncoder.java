package com.energiai.energiaiapi.onnx;

import com.energiai.energiaiapi.domain.enums.TipoInmueble;
import com.energiai.energiaiapi.dto.FacturaDTO;
import com.energiai.energiaiapi.service.inference.ModeloParametros;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * Codifica una {@link FacturaDTO} al vector de 6 floats que espera {@code version2.0.onnx}:
 * <pre>
 * [consumoMensual, usoHorarioPico(0/1), cantidadEquipos, tipoInmuebleEnc,
 *  horasPromedioUso, estacionAnioEnc]
 * </pre>
 * Los 5 campos obligatorios de la Factura (incluido {@code tipoInmueble}) son
 * bloqueantes: sin ellos no se puede invocar el modelo. {@code estacionAnio}
 * es opcional y se imputa si falta.
 */
@Component
public class FacturaFeatureEncoder {

    private final ModeloParametros modelo;

    public FacturaFeatureEncoder(ModeloParametros modelo) {
        this.modelo = modelo;
    }

    public float[] encode(FacturaDTO factura) {
        FacturaDTO f = factura.canonicalizada();
        ModeloParametros.Parametros p = modelo.get();

        if (f.consumoMensual() == null || f.usoHorarioPico() == null
                || f.cantidadEquipos() == null || f.horasPromedioUso() == null) {
            throw new IllegalArgumentException(
                    "Faltan campos obligatorios para el modelo ONNX "
                            + "(consumoMensual, usoHorarioPico, cantidadEquipos, horasPromedioUso)");
        }

        float consumo = f.consumoMensual().floatValue();
        float pico = Boolean.TRUE.equals(f.usoHorarioPico()) ? 1f : 0f;
        float equipos = f.cantidadEquipos().floatValue();
        // tipoInmueble es obligatorio para el modelo: sin encoding valido no se clasifica.
        float tipo = codificarObligatorio(p.tipoInmuebleEncoding(), f.tipoInmueble(), "tipoInmueble");
        float horas = f.horasPromedioUso().floatValue();
        float estacion = codificarOpcional(p.estacionAnioEncoding(), f.estacionAnio(),
                p.scalerMean().length > 5 ? (float) p.scalerMean()[5] : 0f);

        return new float[]{consumo, pico, equipos, tipo, horas, estacion};
    }

    /**
     * Codifica un campo categorico obligatorio. Falla si falta o no esta en el mapa
     * (no hay fallback silencioso a un valor por defecto).
     */
    private float codificarObligatorio(Map<String, Integer> encoding, String clave, String nombreCampo) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException(nombreCampo + " es obligatorio para el modelo ONNX");
        }
        // Valida tambien contra el enum de dominio (monoambiente|departamento|casa).
        if (TipoInmueble.desde(clave).isEmpty()) {
            throw new IllegalArgumentException(
                    nombreCampo + " debe ser: monoambiente, departamento o casa");
        }
        Double code = buscar(encoding, clave);
        if (code == null) {
            throw new IllegalArgumentException(
                    nombreCampo + " no tiene encoding valido en el contrato del modelo: " + clave);
        }
        return code.floatValue();
    }

    private float codificarOpcional(Map<String, Integer> encoding, String clave, float imputacion) {
        if (clave == null || clave.isBlank()) {
            return imputacion;
        }
        Double code = buscar(encoding, clave);
        return code != null ? code.floatValue() : imputacion;
    }

    private Double buscar(Map<String, Integer> encoding, String clave) {
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
}
