package com.energiai.energiaiapi.config;

import com.energiai.energiaiapi.security.JwtAuthenticationFilter;
import com.energiai.energiaiapi.security.RestAuthenticationEntryPoint;
import com.energiai.energiaiapi.security.oauth.OAuth2AuthenticationFailureHandler;
import com.energiai.energiaiapi.security.oauth.OAuth2AuthenticationSuccessHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Seguridad JWT + OAuth2 browser (opcional).
 *
 * Con OAuth habilitado se usa sesion solo para el handshake del IdP
 * ({@code IF_REQUIRED}); el resto de la API sigue autenticandose con JWT.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository;
    private final ObjectProvider<OAuth2AuthenticationSuccessHandler> oauthSuccessHandler;
    private final ObjectProvider<OAuth2AuthenticationFailureHandler> oauthFailureHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
                          ObjectProvider<OAuth2AuthenticationSuccessHandler> oauthSuccessHandler,
                          ObjectProvider<OAuth2AuthenticationFailureHandler> oauthFailureHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.oauthSuccessHandler = oauthSuccessHandler;
        this.oauthFailureHandler = oauthFailureHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean oauthEnabled = clientRegistrationRepository.getIfAvailable() != null;

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(
                        oauthEnabled ? SessionCreationPolicy.IF_REQUIRED : SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/oauth-login.html",
                                "/oauth-callback.html",
                                "/branding/**",
                                "/error",
                                "/favicon.ico")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/analisis").permitAll()
                        .requestMatchers("/api/pruebas/**").permitAll()
                        .requestMatchers("/api/historial/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        OAuth2AuthenticationSuccessHandler success = oauthSuccessHandler.getIfAvailable();
        OAuth2AuthenticationFailureHandler failure = oauthFailureHandler.getIfAvailable();
        if (oauthEnabled && success != null && failure != null) {
            // Repositorio en sesion HTTP (default): mas fiable que cookie serializada.
            http.oauth2Login(oauth -> oauth
                    .successHandler(success)
                    .failureHandler(failure));
        }

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }
}
