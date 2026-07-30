package com.energiai.energiaiapi.dto;

/**
 * Respuesta de registro/login: token JWT y datos basicos del usuario.
 */
public record AuthResponse(
        String token,
        String tipo,
        String email,
        String nombre
) {
    public static AuthResponse bearer(String token, String email, String nombre) {
        return new AuthResponse(token, "Bearer", email, nombre);
    }
}
