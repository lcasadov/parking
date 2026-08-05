package com.aleatica.parking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de la documentacion OpenAPI / Swagger UI.
 *
 * <p>La autenticacion de parking es por cookie de sesion ({@code parking_SESSION}),
 * por lo que se declara un esquema de seguridad de tipo {@code apiKey} en cookie.</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String COOKIE_SCHEME = "sessionCookie";
    private static final String SESSION_COOKIE = "parking_SESSION";

    /**
     * Define la metainformacion y el esquema de seguridad de la API.
     *
     * @return el modelo {@link OpenAPI} de la API parking
     */
    @Bean
    public OpenAPI parkingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nexo REST API")
                        .description("Gestion de plazas de parking y puestos de oficina — ALEATICA")
                        .version("1.0.0")
                        .contact(new Contact().name("Administrador").email("admin@aleatica.local")))
                .components(new Components()
                        .addSecuritySchemes(COOKIE_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name(SESSION_COOKIE)
                                .description("Sesion server-side (Spring Session JDBC). "
                                        + "Obtener via POST /api/v1/auth/login")));
    }
}
