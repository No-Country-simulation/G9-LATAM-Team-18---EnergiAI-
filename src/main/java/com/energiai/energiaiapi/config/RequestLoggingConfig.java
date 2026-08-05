package com.energiai.energiaiapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Loguea cada request HTTP en consola (metodo, URI, query, headers y body).
 * El nivel DEBUG se activa en application-dev.yml.
 */
@Configuration
public class RequestLoggingConfig {

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setIncludeHeaders(false);
        filter.setMaxPayloadLength(10_000);
        filter.setIncludeClientInfo(true);
        filter.setAfterMessagePrefix("HTTP REQUEST => ");
        return filter;
    }
}
