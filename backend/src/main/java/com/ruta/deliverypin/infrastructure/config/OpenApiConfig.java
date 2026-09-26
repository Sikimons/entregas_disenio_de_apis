package com.ruta.deliverypin.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI deliveryOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Ruta - API de entregas").version("1.0.0")
                        .description("Gestion de facturas, productos, PIN, conductores y evidencias. "
                                + "Ejecuta POST /api/auth/login y pega el token en Authorize, sin el prefijo Bearer. "
                                + "Las rutas /api/admin requieren ADMIN; /api/driver permiten ADMIN o CONDUCTOR."))
                .addServersItem(new Server().url("/").description("Servidor actual"))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
