package com.aleatica.parking.fixedassignment.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.aleatica.parking.fixedassignment.domain.FixedAssignment;
import com.aleatica.parking.resource.ResourceType;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios del mapper entidad&harr;dominio {@link FixedAssignmentMapper}: preserva todos
 * los campos en ambos sentidos, incluidos el estado {@code active} y las marcas de revocacion,
 * y respeta la semantica del {@code id} (nulo en el alta). No toca la base de datos.
 */
class FixedAssignmentMapperTest {

    private static final Instant CREATED = Instant.parse("2026-06-20T10:15:30Z");
    private static final Instant REVOKED = Instant.parse("2026-07-01T09:00:00Z");

    @Test
    void shouldMapEntityToDomain_preservingAllFields() {
        // Arrange
        FixedAssignmentEntity entity = new FixedAssignmentEntity(
                42L, 8L, ResourceType.DESK, 15L, 3, false, 1L, CREATED, 2L, REVOKED);

        // Act
        FixedAssignment domain = FixedAssignmentMapper.toDomain(entity);

        // Assert
        assertThat(domain.getId()).isEqualTo(42L);
        assertThat(domain.getResourceId()).isEqualTo(8L);
        assertThat(domain.getResourceType()).isEqualTo(ResourceType.DESK);
        assertThat(domain.getEmployeeId()).isEqualTo(15L);
        assertThat(domain.getDayOfWeek()).isEqualTo(3);
        assertThat(domain.isActive()).isFalse();
        assertThat(domain.getCreatedById()).isEqualTo(1L);
        assertThat(domain.getCreatedAt()).isEqualTo(CREATED);
        assertThat(domain.getRevokedById()).isEqualTo(2L);
        assertThat(domain.getRevokedAt()).isEqualTo(REVOKED);
    }

    @Test
    void shouldMapDomainToEntity_preservingAllFields() {
        // Arrange
        FixedAssignment domain = FixedAssignment.restore(
                42L, 8L, ResourceType.PARKING, 15L, 4, true, 1L, CREATED, null, null);

        // Act
        FixedAssignmentEntity entity = FixedAssignmentMapper.toEntity(domain);

        // Assert
        assertThat(entity.getId()).isEqualTo(42L);
        assertThat(entity.getResourceId()).isEqualTo(8L);
        assertThat(entity.getResourceType()).isEqualTo(ResourceType.PARKING);
        assertThat(entity.getEmployeeId()).isEqualTo(15L);
        assertThat(entity.getDayOfWeek()).isEqualTo(4);
        assertThat(entity.isActive()).isTrue();
        assertThat(entity.getCreatedById()).isEqualTo(1L);
        assertThat(entity.getCreatedAt()).isEqualTo(CREATED);
        assertThat(entity.getRevokedById()).isNull();
        assertThat(entity.getRevokedAt()).isNull();
    }

    @Test
    void shouldMapNewAssignmentToEntity_withNullId() {
        // Arrange: alta creada por la factoria de dominio (id nulo -> insert)
        FixedAssignment domain = FixedAssignment.create(8L, 15L, 2, 1L, CREATED);

        // Act
        FixedAssignmentEntity entity = FixedAssignmentMapper.toEntity(domain);

        // Assert
        assertThat(entity.getId()).isNull();
        assertThat(entity.isActive()).isTrue();
        assertThat(entity.getResourceType()).isEqualTo(ResourceType.PARKING);
    }
}
