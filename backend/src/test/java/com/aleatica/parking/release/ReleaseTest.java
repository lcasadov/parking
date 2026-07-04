package com.aleatica.parking.release;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de la entidad {@link Release}: fabricas de liberacion voluntaria y
 * administrativa (dueno/ejecutor y {@code reason}) y contrato {@code equals}/
 * {@code hashCode} basado en el identificador. No toca la base de datos.
 */
class ReleaseTest {

    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");
    private static final LocalDate DATE = LocalDate.of(2026, 7, 10);

    @Test
    void shouldCreateVoluntaryRelease_whenFactoryUsed() {
        // Act: el titular libera su propio recurso
        Release release = Release.voluntary(8L, 15L, DATE, NOW);

        // Assert: titular = ejecutor, reason nulo
        assertThat(release.getType()).isEqualTo(ReleaseType.VOLUNTARY);
        assertThat(release.getParkingSpaceId()).isEqualTo(8L);
        assertThat(release.getEmployeeId()).isEqualTo(15L);
        assertThat(release.getReleasedById()).isEqualTo(15L);
        assertThat(release.getReleaseDate()).isEqualTo(DATE);
        assertThat(release.getReason()).isNull();
        assertThat(release.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldCreateAdministrativeRelease_whenFactoryUsed() {
        // Act: un admin (1) libera el recurso del empleado (15)
        Release release = Release.administrative(8L, 15L, DATE, "Ausencia", 1L, NOW);

        // Assert: titular != ejecutor, reason presente
        assertThat(release.getType()).isEqualTo(ReleaseType.ADMINISTRATIVE);
        assertThat(release.getEmployeeId()).isEqualTo(15L);
        assertThat(release.getReleasedById()).isEqualTo(1L);
        assertThat(release.getReason()).isEqualTo("Ausencia");
    }

    @Test
    void shouldBeEqualById_whenSameIdentifier() throws Exception {
        // Arrange
        Release a = withId(Release.voluntary(8L, 15L, DATE, NOW), 42L);
        Release b = withId(Release.administrative(9L, 16L, DATE, "x", 1L, NOW), 42L);
        Release other = withId(Release.voluntary(8L, 15L, DATE, NOW), 43L);

        // Assert
        assertThat(a)
                .isEqualTo(a)
                .isEqualTo(b)
                .isNotEqualTo(other)
                .isNotEqualTo(null)
                .isNotEqualTo("no-es-una-liberacion");
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    void shouldNotBeEqual_whenIdIsNull() {
        // Arrange: dos entidades transitorias (id null) nunca son iguales entre si
        Release a = Release.voluntary(8L, 15L, DATE, NOW);
        Release b = Release.voluntary(8L, 15L, DATE, NOW);

        // Assert
        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isZero();
    }

    private static Release withId(Release release, long id) throws Exception {
        Field field = Release.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(release, id);
        return release;
    }
}
