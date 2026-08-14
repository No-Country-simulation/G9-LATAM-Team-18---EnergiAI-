package com.energiai.energiaiapi.exception;

/**
 * Error de regla de negocio (por ejemplo, email ya registrado). Se mapea a 409/400.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String message) {
        super(message);
    }
}
