package com.energiai.energiaiapi.security.oauth;

import com.energiai.energiaiapi.domain.Usuario;
import com.energiai.energiaiapi.domain.enums.AuthProvider;
import com.energiai.energiaiapi.security.JwtService;
import com.energiai.energiaiapi.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Locale;

/**
 * Tras el redirect del proveedor OAuth2: upsert del usuario en BD, emite JWT
 * y redirige a {@code oauth-callback.html?token=...}.
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final String redirectUrl;

    public OAuth2AuthenticationSuccessHandler(
            UsuarioService usuarioService,
            JwtService jwtService,
            @Value("${app.oauth2.success-redirect-url:http://localhost:8080/oauth-callback.html}")
            String redirectUrl) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
        this.redirectUrl = redirectUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            getRedirectStrategy().sendRedirect(request, response, redirectUrl + "?error=oauth");
            return;
        }

        OAuth2User oauthUser = oauthToken.getPrincipal();
        AuthProvider provider = AuthProvider.valueOf(
                oauthToken.getAuthorizedClientRegistrationId().toUpperCase(Locale.ROOT));
        String email = oauthUser.getAttribute("email");
        String nombre = oauthUser.getAttribute("name");
        String providerId = oauthUser.getName();

        if (email == null || email.isBlank()) {
            log.warn("OAuth {} sin email en el perfil (scopes/permisos)", provider);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl + "?error=email");
            return;
        }

        try {
            Usuario usuario = usuarioService.registrarOActualizarOAuth(
                    email, nombre, provider, providerId);
            String jwt = jwtService.generarToken(usuario.getEmail());

            String target = UriComponentsBuilder.fromUriString(redirectUrl)
                    .queryParam("token", jwt)
                    .queryParam("tipo", "Bearer")
                    .queryParam("email", usuario.getEmail())
                    .encode()
                    .build()
                    .toUriString();

            clearAuthenticationAttributes(request);
            getRedirectStrategy().sendRedirect(request, response, target);
        } catch (Exception ex) {
            log.error("Error persistiendo usuario OAuth {}: {}", email, ex.getMessage());
            String target = UriComponentsBuilder.fromUriString(redirectUrl)
                    .queryParam("error", ex.getMessage() != null ? ex.getMessage() : "persist_failed")
                    .encode()
                    .build()
                    .toUriString();
            getRedirectStrategy().sendRedirect(request, response, target);
        }
    }
}
