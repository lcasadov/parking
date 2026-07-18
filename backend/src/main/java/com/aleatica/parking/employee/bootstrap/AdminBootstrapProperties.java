package com.aleatica.parking.employee.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Propiedades del bootstrap del primer administrador (namespace
 * {@code parking.bootstrap.admin}).
 *
 * <p>Mecanismo <strong>opt-in</strong> (desactivado por defecto) para crear el
 * administrador inicial en un despliegue limpio (perfiles {@code docker}/{@code pro}),
 * donde la tabla {@code employees} nace vacia y el seed de desarrollo esta excluido
 * a proposito (bug #11 / CWE-798). Los valores se inyectan por variables de entorno
 * gracias al <em>relaxed binding</em> de Spring Boot: {@code PARKING_BOOTSTRAP_ADMIN_ENABLED},
 * {@code PARKING_BOOTSTRAP_ADMIN_EMAIL}, {@code PARKING_BOOTSTRAP_ADMIN_PASSWORD}, etc.</p>
 *
 * <p>Nunca contiene credenciales por defecto: sin {@code email} y {@code password}
 * el bootstrap es un no-op.</p>
 *
 * @param enabled   si {@code true} intenta crear el admin al arrancar (default {@code false})
 * @param email     email del administrador inicial (obligatorio si {@code enabled})
 * @param password  contrasena en claro del administrador (obligatorio si {@code enabled});
 *                  se persiste hasheada con BCrypt, nunca en claro
 * @param firstName nombre del administrador (default {@code Admin})
 * @param lastName  apellidos del administrador (default {@code Parking})
 * @param phone     telefono movil opcional (columna anulable)
 * @param login     login opcional; si se omite se deriva de la parte local del email
 */
@ConfigurationProperties(prefix = "parking.bootstrap.admin")
public record AdminBootstrapProperties(
        @DefaultValue("false") boolean enabled,
        String email,
        String password,
        @DefaultValue("Admin") String firstName,
        @DefaultValue("Parking") String lastName,
        String phone,
        String login) {
}
