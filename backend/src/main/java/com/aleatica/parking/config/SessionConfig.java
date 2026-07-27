package com.aleatica.parking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Configuracion de la sesion HTTP server-side (Spring Session JDBC sobre SQL Server).
 *
 * <p>La sesion vive en las tablas {@code SPRING_SESSION} / {@code SPRING_SESSION_ATTRIBUTES}
 * (creadas por Flyway), lo que permite invalidacion inmediata desde el servidor
 * (logout, bloqueo) y, en Fase 2, el Single Logout. TTL de inactividad: 30 dias
 * (decision de producto: sesion de larga duracion; el contador se reinicia con
 * cada peticion, asi que solo expira tras ~1 mes sin actividad).</p>
 *
 * <p>Cookie {@code parking_SESSION}: {@code HttpOnly} (inaccesible a XSS),
 * {@code SameSite=Lax} (anti-CSRF permitiendo el retorno del SSO),
 * {@code Path=/parking-api}. El flag {@code Secure} se activa solo en produccion
 * (perfil {@code pro}), no en desarrollo sobre HTTP.</p>
 */
@Configuration
@EnableSpringHttpSession
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = SessionConfig.SESSION_TTL_SECONDS)
public class SessionConfig {

    /** TTL de inactividad de la sesion: 30 dias (se reinicia con cada peticion). */
    public static final int SESSION_TTL_SECONDS = 30 * 24 * 60 * 60;

    private static final String COOKIE_NAME = "parking_SESSION";
    private static final String COOKIE_PATH = "/parking-api";
    private static final String SAME_SITE_LAX = "Lax";

    /**
     * Serializador de la cookie de sesion con los flags de seguridad del proyecto.
     *
     * @param secureCookie {@code true} para marcar la cookie como {@code Secure}
     *                     (inyectado desde {@code parking.session.cookie.secure},
     *                     {@code true} solo en el perfil {@code pro})
     * @return el {@link CookieSerializer} configurado para {@code parking_SESSION}
     */
    @Bean
    public CookieSerializer cookieSerializer(
            @org.springframework.beans.factory.annotation.Value(
                    "${parking.session.cookie.secure:false}") boolean secureCookie) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(COOKIE_NAME);
        serializer.setCookiePath(COOKIE_PATH);
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite(SAME_SITE_LAX);
        serializer.setUseSecureCookie(secureCookie);
        serializer.setCookieMaxAge(SESSION_TTL_SECONDS);
        return serializer;
    }
}
