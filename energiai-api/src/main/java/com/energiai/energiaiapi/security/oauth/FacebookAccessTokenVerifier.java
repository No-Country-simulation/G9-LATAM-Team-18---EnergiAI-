package com.energiai.energiaiapi.security.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Verifica un Facebook {@code access_token} via Graph API ({@code /me}).
 * Requiere que la app de Facebook solicite el permiso {@code email}.
 */
@Component
public class FacebookAccessTokenVerifier {

    private final RestClient restClient;
    private final String appId;
    private final String appSecret;

    public FacebookAccessTokenVerifier(
            @Value("${app.oauth2.facebook.client-id:}") String appId,
            @Value("${app.oauth2.facebook.client-secret:}") String appSecret) {
        this.restClient = RestClient.create();
        this.appId = appId;
        this.appSecret = appSecret;
    }

    public OAuthProfile verificar(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalArgumentException("access_token de Facebook ausente");
        }
        if (StringUtils.hasText(appId) && StringUtils.hasText(appSecret)) {
            validarApp(accessToken);
        }
        JsonNode body;
        try {
            body = restClient.get()
                    .uri("https://graph.facebook.com/me?fields=id,name,email&access_token={token}",
                            accessToken)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("Token de Facebook invalido o expirado");
        }
        if (body == null || !StringUtils.hasText(body.path("email").asText(null))) {
            throw new IllegalArgumentException(
                    "Token de Facebook sin email (verifica el permiso 'email' en la app)");
        }
        return new OAuthProfile(
                body.path("id").asText(null),
                body.path("email").asText(),
                body.path("name").asText(body.path("email").asText()));
    }

    private void validarApp(String accessToken) {
        try {
            String appAccessToken = appId + "|" + appSecret;
            JsonNode debug = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("graph.facebook.com")
                            .path("/debug_token")
                            .queryParam("input_token", accessToken)
                            .queryParam("access_token", appAccessToken)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode data = debug != null ? debug.path("data") : null;
            if (data == null || !data.path("is_valid").asBoolean(false)) {
                throw new IllegalArgumentException("Token de Facebook no valido para esta app");
            }
            String tokenAppId = data.path("app_id").asText("");
            if (!appId.equals(tokenAppId)) {
                throw new IllegalArgumentException("Token de Facebook emitido por otra app");
            }
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("No se pudo validar el token de Facebook");
        }
    }
}
