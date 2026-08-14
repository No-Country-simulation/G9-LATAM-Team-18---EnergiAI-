package com.energiai.energiaiapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(name = "ErrorResponse", description = "Error estandar de la API")
public record ErrorResponse(

        Instant timestamp,

        @Schema(example = "400")
        int status,

        @Schema(example = "Bad Request")
        String error,

        @Schema(example = "Error de validacion en 3 campos: factura.horas_alto_consumo, factura.month, factura.tipo_inmueble")
        String message,

        @Schema(example = "/api/analisis")
        String path,

        @Schema(description = "Un mensaje por campo invalido. Clave = ruta JSON snake_case; incluye el valor recibido. "
                + "Errores de dominio/rango se acumulan (caso QA #5). Errores de tipo/formato se reportan de a uno.",
                example = """
                        {"factura.horas_alto_consumo":"horas_alto_consumo no puede superar 24 (recibido: 30.0)",\
                        "factura.month":"month debe ser 1-12 o el nombre del mes (recibido: \\"13\\")",\
                        "factura.tipo_inmueble":"tipo_inmueble debe ser Casa, Departamento o Monoambiente (sin espacios) (recibido: \\"Oficina\\")"}
                        """,
                nullable = true)
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }

    public static ErrorResponse validation(int status, String message, String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, "Bad Request", message, path, fieldErrors);
    }
}
