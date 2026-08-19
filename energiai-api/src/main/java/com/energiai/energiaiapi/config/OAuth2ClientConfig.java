package com.energiai.energiaiapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Registra clientes OAuth2 (Google / Facebook) solo cuando {@code app.oauth2.enabled=true}
 * y hay client-id/secret. Asi la API arranca sin OAuth en entornos de prueba.
 */
@Configuration
@ConditionalOnProperty(name = "app.oauth2.enabled", havingValue = "true")
public class OAuth2ClientConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${app.oauth2.google.client-id:}") String googleClientId,
            @Value("${app.oauth2.google.client-secret:}") String googleClientSecret,
            @Value("${app.oauth2.facebook.client-id:}") String facebookClientId,
            @Value("${app.oauth2.facebook.client-secret:}") String facebookClientSecret) {

        List<ClientRegistration> registrations = new ArrayList<>();
        if (StringUtils.hasText(googleClientId) && StringUtils.hasText(googleClientSecret)) {
            registrations.add(google(googleClientId, googleClientSecret));
        }
        if (StringUtils.hasText(facebookClientId) && StringUtils.hasText(facebookClientSecret)) {
            registrations.add(facebook(facebookClientId, facebookClientSecret));
        }
        if (registrations.isEmpty()) {
            throw new IllegalStateException(
                    "app.oauth2.enabled=true pero no hay client-id/secret de Google ni Facebook");
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }

    private static ClientRegistration google(String clientId, String clientSecret) {
        return ClientRegistration.withRegistrationId("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .issuerUri("https://accounts.google.com")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .clientName("Google")
                .build();
    }

    private static ClientRegistration facebook(String clientId, String clientSecret) {
        return ClientRegistration.withRegistrationId("facebook")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("email", "public_profile")
                .authorizationUri("https://www.facebook.com/v18.0/dialog/oauth")
                .tokenUri("https://graph.facebook.com/v18.0/oauth/access_token")
                .userInfoUri("https://graph.facebook.com/me?fields=id,name,email")
                .userNameAttributeName("id")
                .clientName("Facebook")
                .build();
    }
}
