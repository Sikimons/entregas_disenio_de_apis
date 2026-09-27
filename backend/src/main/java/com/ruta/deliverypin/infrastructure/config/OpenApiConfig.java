package com.ruta.deliverypin.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentacion OpenAPI generada a partir de los controladores y DTO ya existentes
 * (springdoc-openapi los lee via reflexion; no requiere anotar cada endpoint para
 * tener una documentacion base). Disponible en /v3/api-docs y /swagger-ui/index.html.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI rutaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ruta - API de verificacion de entregas")
                        .description("Verificacion de entregas con PIN, foto y ubicacion (Fase1_Vision_Producto_Modelo_Negocio_API_Final.md). "
                                + "Todas las rutas /api/v1/admin/** y /api/v1/driver/** requieren un token JWT Bearer obtenido en POST /api/v1/auth/login.")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
