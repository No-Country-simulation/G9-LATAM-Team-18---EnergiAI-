package com.energiai.energiaiapi.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

/**
 * Extraccion del email del {@link Authentication} poblado por el filtro JWT
 * (o, en defensa, por el principal OAuth2 del handshake si todavia no se invalido).
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<String> emailDe(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return texto(userDetails.getUsername());
        }
        if (principal instanceof OAuth2User oauthUser) {
            Object email = oauthUser.getAttribute("email");
            if (email instanceof String s) {
                return texto(s);
            }
        }
        if (principal instanceof String email && !"anonymousUser".equals(email)) {
            return texto(email);
        }
        return Optional.empty();
    }

    private static Optional<String> texto(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }
}
