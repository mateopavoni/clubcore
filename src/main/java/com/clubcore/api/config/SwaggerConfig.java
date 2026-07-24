package com.clubcore.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura Swagger UI / OpenAPI 3.
 *
 * <p>El botón "Authorize" en la UI acepta el Bearer JWT para probar
 * endpoints protegidos directamente desde el navegador.</p>
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ClubCore API")
                        .version("0.1.0")
                        .description("API REST para gestión de un instituto de salud / complejo deportivo. " +
                                "Soporta membresías, pagos, reservas de turnos, rutinas y control de accesos.")
                        .contact(new Contact()
                                .name("Mateo Pavoni")
                                .email("codetloncordoba@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
