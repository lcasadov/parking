package com.aleatica.parking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;

/**
 * Configuracion de seguridad minima del arranque.
 *
 * <p>Endpoints publicos: {@code /api/v1/health}, {@code POST /api/v1/auth/login},
 * la documentacion OpenAPI/Swagger y los endpoints de Actuator {@code health}/
 * {@code info}. El resto requiere autenticacion (fail closed), incluidos
 * {@code /api/v1/auth/me}, {@code /logout} y {@code /change-password}. La sesion
 * la establece {@code AuthController} tras verificar las credenciales.</p>
 *
 * <p>La sesion se gestiona con Spring Session JDBC (cookie {@code parking_SESSION});
 * Spring Security la usa cuando exista, pero no la fuerza ({@code IF_REQUIRED}).</p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final int BCRYPT_STRENGTH = 12;

    private static final String PATH_HEALTH = "/api/v1/health";
    private static final String PATH_AUTH_LOGIN = "/api/v1/auth/login";
    private static final String[] PATH_OPENAPI = {
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
    };

    /**
     * Cadena de filtros de seguridad de la API.
     *
     * <p>CSRF se desactiva: la API es JSON sin formularios y la cookie de sesion usa
     * {@code SameSite=Lax} (anti-CSRF para peticiones cross-site de terceros). Un
     * acceso no autenticado a una ruta protegida responde {@code 401} en vez de
     * redirigir a un formulario de login.</p>
     *
     * @param http builder de seguridad HTTP
     * @return la cadena de filtros configurada
     * @throws Exception si la configuracion de seguridad falla
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PATH_HEALTH).permitAll()
                        .requestMatchers(PATH_AUTH_LOGIN).permitAll()
                        .requestMatchers(PATH_OPENAPI).permitAll()
                        .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }

    /**
     * Codificador de contrasenas BCrypt con coste 12 (politica de seguridad).
     *
     * @return el {@link PasswordEncoder} BCrypt de coste 12
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }
}
