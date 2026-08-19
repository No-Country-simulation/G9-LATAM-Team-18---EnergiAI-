package com.energiai.energiaiapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * El PasswordEncoder vive aparte de SecurityConfig para evitar un ciclo:
 * SecurityConfig -> JwtAuthenticationFilter -> UsuarioService -> PasswordEncoder.
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
