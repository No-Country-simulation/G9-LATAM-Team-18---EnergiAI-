package com.energiai.energiaiapi.onnx;

/**
 * Vector ONNX + sinteticas + eco de la consulta en sintaxis DS.
 */
public record XgboostFeatures(
        float[] vector,
        double intensidadPorEquipo,
        double horasPicoInteraccion,
        double desviacionEquiposTipo,
        String tipoInmuebleDs,
        int month,
        String usoHorarioPicoDs,
        double horasAltoConsumo,
        int cantidadEquipos
) {
}
