package com.energiai.energiaiapi.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Cuerpo estandar de error para la API.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }

    public static ErrorResponse validation(int status, String message, String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, "Bad Request", message, path, fieldErrors);
    }
}
