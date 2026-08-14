package com.energiai.energiaiapi.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * Extraccion del email del {@link Authentication} poblado por el filtro JWT
 * (o por el success handler OAuth2).
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
            return Optional.ofNullable(userDetails.getUsername());
        }
        if (principal instanceof String email && !"anonymousUser".equals(email)) {
            return Optional.of(email);
        }
        return Optional.empty();
    }
}
