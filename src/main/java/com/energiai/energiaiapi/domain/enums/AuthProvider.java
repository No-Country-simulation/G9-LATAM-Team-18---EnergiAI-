package com.energiai.energiaiapi.domain.enums;

/**
 * Origen de autenticacion del usuario.
 * LOCAL = registro con email + password. GOOGLE/FACEBOOK = OAuth2 (a activar mas adelante).
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE,
    FACEBOOK
}
