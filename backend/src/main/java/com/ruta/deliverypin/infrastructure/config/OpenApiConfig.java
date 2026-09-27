package com.ruta.deliverypin.infrastructure.config;

import com.ruta.deliverypin.infrastructure.adapter.in.web.security.JwtAuthenticationFilter;
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

    private static final String COOKIE_SCHEME = "sessionCookie";

    @Bean
    public OpenAPI rutaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ruta - API de verificacion de entregas")
                        .description("Verificacion de entregas con PIN, foto y ubicacion (Fase1_Vision_Producto_Modelo_Negocio_API_Final.md). "
                                + "Todas las rutas /api/v1/admin/** y /api/v1/driver/** exigen la cookie HttpOnly de sesion "
                                + "(\"" + JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE + "\") que POST /api/v1/auth/login establece "
                                + "via Set-Cookie -- Swagger UI la envia solo si se probo el login desde el propio navegador con "
                                + "\"Try it out\" (comparte cookies con la pestana); un token pegado a mano no aplica aqui, a "
                                + "diferencia del antiguo esquema Bearer.")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_SCHEME))
                .components(new Components().addSecuritySchemes(COOKIE_SCHEME,
                        new SecurityScheme()
                                .name(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE)
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)));
    }
}
