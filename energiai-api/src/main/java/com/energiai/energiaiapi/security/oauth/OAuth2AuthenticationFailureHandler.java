package com.energiai.energiaiapi.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    private final String redirectUrl;

    public OAuth2AuthenticationFailureHandler(
            @Value("${app.oauth2.success-redirect-url:http://localhost:8080/oauth-callback.html}")
            String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("OAuth2 login falló: {}", exception.getMessage());
        String message = exception.getLocalizedMessage() != null
                ? exception.getLocalizedMessage()
                : "oauth_failed";
        // encode() evita 500 si el mensaje trae '[' u otros caracteres reservados
        String target = UriComponentsBuilder.fromUriString(redirectUrl)
                .queryParam("error", message)
                .encode()
                .build()
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
