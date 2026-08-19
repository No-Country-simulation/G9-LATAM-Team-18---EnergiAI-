package com.energiai.energiaiapi.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Solo se registra si OAuth2 browser NO esta habilitado.
 * Evita el 500 "No static resource oauth2/authorization/..." y explica como activarlo.
 */
@RestController
@ConditionalOnMissingBean(ClientRegistrationRepository.class)
public class OAuth2DisabledController {

    @GetMapping(value = "/oauth2/authorization/{provider}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> oauthDeshabilitado(@PathVariable String provider) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 503);
        body.put("error", "OAuth2 browser deshabilitado");
        body.put("provider", provider);
        body.put("message",
                "No hay ClientRegistration OAuth2. Para habilitar login con Google/Facebook: "
                        + "1) export APP_OAUTH2_ENABLED=true  "
                        + "2) export GOOGLE_CLIENT_ID y GOOGLE_CLIENT_SECRET "
                        + "(y/o FACEBOOK_CLIENT_ID / FACEBOOK_CLIENT_SECRET)  "
                        + "3) reiniciar la API  "
                        + "4) redirect URI en la consola del IdP: "
                        + "http://localhost:8080/login/oauth2/code/" + provider);
        body.put("loginPage", "http://localhost:8080/oauth-login.html");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
