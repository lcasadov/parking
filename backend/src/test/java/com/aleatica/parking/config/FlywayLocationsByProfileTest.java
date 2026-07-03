package com.aleatica.parking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Verifica que el seed del admin de desarrollo ({@code V5__seed_dev_admin.sql})
 * solo entra en las localizaciones de Flyway del perfil de desarrollo.
 *
 * <p>Guard del bug #11 (CWE-798): el seed inserta una credencial documentada en el
 * repo ({@code admin} / contraseña conocida) y NUNCA debe ejecutarse en PRE/PRO.
 * Los perfiles {@code pre} y {@code pro} fijan explícitamente las localizaciones
 * de esquema para que un cambio futuro del default no pueda re-filtrar el seed.</p>
 *
 * <p>Es un test ligero de configuración: procesa los YAML reales con el pipeline de
 * config-data de Spring Boot (sin levantar datasource ni contexto completo).</p>
 */
class FlywayLocationsByProfileTest {

    private static final String FLYWAY_LOCATIONS_PROPERTY = "spring.flyway.locations";
    private static final String SCHEMA_LOCATION = "classpath:db/migration";
    private static final String DEV_SEED_LOCATION = "classpath:db/seed/dev";
    private static final String DEV_SEED_RESOURCE = "V5__seed_dev_admin.sql";

    @Test
    void shouldExcludeDevSeedLocation_whenProProfileActive() {
        // Arrange + Act
        List<String> locations = flywayLocationsFor("pro");

        // Assert
        assertThat(locations).containsExactly(SCHEMA_LOCATION);
    }

    @Test
    void shouldExcludeDevSeedLocation_whenPreProfileActive() {
        // Arrange + Act
        List<String> locations = flywayLocationsFor("pre");

        // Assert
        assertThat(locations).containsExactly(SCHEMA_LOCATION);
    }

    @Test
    void shouldIncludeDevSeedLocation_whenDesProfileActive() {
        // Arrange + Act
        List<String> locations = flywayLocationsFor("des");

        // Assert
        assertThat(locations).containsExactly(SCHEMA_LOCATION, DEV_SEED_LOCATION);
    }

    @Test
    void shouldIncludeDevSeedLocation_whenNoProfileActive() {
        // Arrange + Act — sin perfil explícito aplica el default (des), que es el
        // perfil con el que corren los tests de integración (AuthLocalIT depende
        // de que el seed exista).
        List<String> locations = flywayLocationsFor(null);

        // Assert
        assertThat(locations).containsExactly(SCHEMA_LOCATION, DEV_SEED_LOCATION);
    }

    @Test
    void shouldKeepDevSeedOutsideSchemaMigrations_whenInspectingClasspath() {
        // Arrange + Act — Flyway escanea las localizaciones de forma recursiva, por lo
        // que el seed no puede vivir bajo db/migration (ni en un subdirectorio suyo).
        var seedUnderSchemaPath = getClass().getResource("/db/migration/" + DEV_SEED_RESOURCE);
        var seedUnderDevPath = getClass().getResource("/db/seed/dev/" + DEV_SEED_RESOURCE);

        // Assert
        assertThat(seedUnderSchemaPath)
                .as("el seed de dev no debe estar en la localización de esquema común")
                .isNull();
        assertThat(seedUnderDevPath)
                .as("el seed de dev debe existir en la localización exclusiva de dev")
                .isNotNull();
    }

    /**
     * Procesa los application*.yml reales con el pipeline de config-data de Spring
     * Boot y devuelve las localizaciones efectivas de Flyway para el perfil dado.
     *
     * @param profile perfil a activar, o {@code null} para dejar actuar el default
     * @return lista efectiva de {@code spring.flyway.locations}
     */
    private List<String> flywayLocationsFor(String profile) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            ConfigurableEnvironment environment = context.getEnvironment();
            if (profile != null) {
                environment.setActiveProfiles(profile);
            }
            new ConfigDataApplicationContextInitializer().initialize(context);
            return Binder.get(environment)
                    .bind(FLYWAY_LOCATIONS_PROPERTY, Bindable.listOf(String.class))
                    .orElseThrow(() -> new IllegalStateException(
                            FLYWAY_LOCATIONS_PROPERTY + " no está definido para el perfil " + profile));
        }
    }
}
