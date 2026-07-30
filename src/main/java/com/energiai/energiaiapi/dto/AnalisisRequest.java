package com.energiai.energiaiapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request de POST /api/analisis.
 * - factura: datos de entrada (obligatorio).
 * - resultado: clasificacion del frontend (ONNX); opcional -> si falta, clasifica el backend.
 * - guardar: si es true y el usuario esta autenticado, se persiste en el historial.
 *   Si el usuario es anonimo, la consulta se procesa igual pero NO se guarda.
 */
public record AnalisisRequest(

        @NotNull(message = "La factura es obligatoria")
        @Valid
        FacturaDTO factura,

        @Valid
        ResultadoModeloDTO resultado,

        @Schema(description = "Guardar en el historial (requiere sesion iniciada)", example = "false")
        boolean guardar
) {
}
