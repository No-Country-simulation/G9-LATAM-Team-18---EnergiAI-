package com.energiai.energiaiapi.domain.enums;

/**
 * Origen de autenticacion del usuario.
 * LOCAL = email + password. GOOGLE/FACEBOOK = OAuth2 (browser o canje de token API).
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE,
    FACEBOOK
}
