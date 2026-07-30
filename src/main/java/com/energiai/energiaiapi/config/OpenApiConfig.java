package com.energiai.energiaiapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI energiaiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EnergiAI API")
                        .description("Backend de analisis de eficiencia energetica. "
                                + "El frontend clasifica con ONNX y este servicio valida, calcula negocio "
                                + "(costo, indice, recomendaciones) y persiste el historial de forma opcional.")
                        .version("v0.0.1")
                        .contact(new Contact().name("Equipo Backend - ONE G9"))
                        .license(new License().name("MIT")));
    }
}
