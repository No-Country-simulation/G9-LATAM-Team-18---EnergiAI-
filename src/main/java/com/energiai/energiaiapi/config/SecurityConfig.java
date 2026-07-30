package com.energiai.energiaiapi.config;

import com.energiai.energiaiapi.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Seguridad LISTA PERO ABIERTA (fase de pruebas).
 *
 * Estado actual:
 *  - Todos los endpoints en permitAll: se puede probar la API sin autenticacion.
 *  - El filtro JWT ya esta en la cadena: si llega un token valido, identifica al usuario
 *    (necesario para guardar historial); si no, la request es anonima.
 *  - Sesion stateless + CSRF deshabilitado (API REST con JWT).
 *
 * Para ENDURECER mas adelante (tras las pruebas), reemplazar el authorizeHttpRequests por algo como:
 *   .authorizeHttpRequests(auth -> auth
 *       .requestMatchers("/api/auth/**", "/api/analisis", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
 *       .requestMatchers("/api/historial/**").authenticated()
 *       .anyRequest().authenticated())
 *
 * Para OAuth2 (Google/Facebook), una vez definido el clientRegistration en application.yml, agregar:
 *   .oauth2Login(Customizer.withDefaults())
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
