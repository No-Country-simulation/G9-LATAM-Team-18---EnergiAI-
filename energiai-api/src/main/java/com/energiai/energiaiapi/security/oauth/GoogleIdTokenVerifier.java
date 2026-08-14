package com.energiai.energiaiapi.security.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Verifica un Google {@code id_token} contra el endpoint tokeninfo de Google
 * y valida el audience ({@code aud}) contra {@code GOOGLE_CLIENT_ID} si esta configurado.
 */
@Component
public class GoogleIdTokenVerifier {

    private final RestClient restClient;
    private final String expectedAudience;

    public GoogleIdTokenVerifier(@Value("${app.oauth2.google.client-id:}") String expectedAudience) {
        this.restClient = RestClient.create();
        this.expectedAudience = expectedAudience;
    }

    public OAuthProfile verificar(String idToken) {
        if (!StringUtils.hasText(idToken)) {
            throw new IllegalArgumentException("id_token de Google ausente");
        }
        JsonNode body;
        try {
            body = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token={token}", idToken)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("Token de Google invalido o expirado");
        }
        if (body == null || body.path("email").asText(null) == null) {
            throw new IllegalArgumentException("Token de Google sin email verificable");
        }
        if (StringUtils.hasText(expectedAudience)) {
            String aud = body.path("aud").asText("");
            if (!expectedAudience.equals(aud)) {
                throw new IllegalArgumentException("Token de Google con audience incorrecto");
            }
        }
        String emailVerified = body.path("email_verified").asText("true");
        if ("false".equalsIgnoreCase(emailVerified)) {
            throw new IllegalArgumentException("Email de Google no verificado");
        }
        return new OAuthProfile(
                body.path("sub").asText(null),
                body.path("email").asText(),
                body.path("name").asText(body.path("email").asText()));
    }
}
