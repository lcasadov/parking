package com.aleatica.parking.release.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.aleatica.parking.release.domain.Release;
import com.aleatica.parking.release.domain.ReleaseType;
import com.aleatica.parking.resource.ResourceType;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de {@link ReleaseMapper}: mapeo plano entidad&harr;dominio en ambos sentidos
 * (todos los campos escalares o identificadores). Verifica que el {@code id} viaja tal cual (para
 * distinguir alta de actualizacion) y que no se pierde ningun atributo en la traduccion. No toca
 * la base de datos.
 */
class ReleaseMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");
    private static final LocalDate DATE = LocalDate.of(2026, 7, 10);

    @Test
    void shouldMapEntityToDomain_preservingAllFields() {
        // Arrange
        ReleaseEntity entity = new ReleaseEntity(
                42L, 8L, ResourceType.PARKING, 15L, DATE, ReleaseType.ADMINISTRATIVE,
                "Ausencia", 1L, NOW);

        // Act
        Release domain = ReleaseMapper.toDomain(entity);

        // Assert
        assertThat(domain.getId()).isEqualTo(42L);
        assertThat(domain.getResourceId()).isEqualTo(8L);
        assertThat(domain.getResourceType()).isEqualTo(ResourceType.PARKING);
        assertThat(domain.getEmployeeId()).isEqualTo(15L);
        assertThat(domain.getReleaseDate()).isEqualTo(DATE);
        assertThat(domain.getType()).isEqualTo(ReleaseType.ADMINISTRATIVE);
        assertThat(domain.getReason()).isEqualTo("Ausencia");
        assertThat(domain.getReleasedById()).isEqualTo(1L);
        assertThat(domain.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldMapDomainToEntity_preservingAllFields() {
        // Arrange: alta voluntaria (id null -> insert), titular = ejecutor, reason nulo
        Release domain = Release.voluntary(8L, 15L, DATE, NOW);

        // Act
        ReleaseEntity entity = ReleaseMapper.toEntity(domain);

        // Assert
        assertThat(entity.getId()).isNull();
        assertThat(entity.getResourceId()).isEqualTo(8L);
        assertThat(entity.getResourceType()).isEqualTo(ResourceType.PARKING);
        assertThat(entity.getEmployeeId()).isEqualTo(15L);
        assertThat(entity.getReleaseDate()).isEqualTo(DATE);
        assertThat(entity.getType()).isEqualTo(ReleaseType.VOLUNTARY);
        assertThat(entity.getReason()).isNull();
        assertThat(entity.getReleasedById()).isEqualTo(15L);
        assertThat(entity.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldRoundTripThroughEntityAndBack_preservingIdentityAndFields() {
        // Arrange
        Release original = Release.restore(
                7L, 9L, ResourceType.DESK, 20L, DATE, ReleaseType.VOLUNTARY, null, 20L, NOW);

        // Act
        Release roundTripped = ReleaseMapper.toDomain(ReleaseMapper.toEntity(original));

        // Assert
        assertThat(roundTripped.getId()).isEqualTo(7L);
        assertThat(roundTripped.getResourceType()).isEqualTo(ResourceType.DESK);
        assertThat(roundTripped.getEmployeeId()).isEqualTo(20L);
        assertThat(roundTripped.getReleasedById()).isEqualTo(20L);
        assertThat(roundTripped.getReason()).isNull();
    }
}
