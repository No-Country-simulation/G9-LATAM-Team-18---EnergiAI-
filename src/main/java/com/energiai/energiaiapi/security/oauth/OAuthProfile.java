package com.energiai.energiaiapi.security.oauth;

/**
 * Perfil minimo obtenido tras verificar un token del proveedor OAuth.
 */
public record OAuthProfile(
        String providerId,
        String email,
        String nombre
) {
}
