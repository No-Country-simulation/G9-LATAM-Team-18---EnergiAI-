package com.energiai.energiaiapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta de registro/login: token JWT y datos basicos del usuario.
 */
@Schema(name = "AuthResponse", description = "JWT emitido tras registro/login/OAuth")
public record AuthResponse(

        @Schema(description = "Access token JWT", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token,

        @Schema(description = "Tipo de token", example = "Bearer")
        String tipo,

        @Schema(example = "lucia@example.com")
        String email,

        @Schema(example = "Lucia", nullable = true)
        String nombre
) {
    public static AuthResponse bearer(String token, String email, String nombre) {
        return new AuthResponse(token, "Bearer", email, nombre);
    }
}
