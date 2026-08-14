package com.energiai.energiaiapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(name = "AnalisisRequest", description = "Body de POST /api/analisis")
public record AnalisisRequest(

        @Schema(description = "Datos de la factura / consulta al modelo", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La factura es obligatoria")
        @Valid
        FacturaDTO factura,

        @Schema(description = "Clasificacion previa del FE (opcional). Si falta, clasifica el backend ONNX")
        @Valid
        ResultadoModeloDTO resultado,

        @Schema(description = "true = persistir en historial (requiere JWT). Invitado: false",
                example = "false", defaultValue = "false")
        boolean guardar
) {
}
