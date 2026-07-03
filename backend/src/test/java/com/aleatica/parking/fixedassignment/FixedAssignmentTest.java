package com.aleatica.parking.fixedassignment;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de la entidad {@link FixedAssignment}: fabrica de alta activa,
 * revocacion logica (sin borrar) y contrato {@code equals}/{@code hashCode} basado en
 * el identificador. No toca la base de datos.
 */
class FixedAssignmentTest {

    private static final Instant NOW = Instant.parse("2026-06-20T10:15:30Z");
    private static final Instant LATER = Instant.parse("2026-07-01T09:00:00Z");

    @Test
    void shouldCreateActiveAssignment_whenFactoryUsed() {
        // Act
        FixedAssignment assignment = FixedAssignment.create(8L, 15L, 3, 1L, NOW);

        // Assert
        assertThat(assignment.isActive()).isTrue();
        assertThat(assignment.getParkingSpaceId()).isEqualTo(8L);
        assertThat(assignment.getEmployeeId()).isEqualTo(15L);
        assertThat(assignment.getDayOfWeek()).isEqualTo(3);
        assertThat(assignment.getCreatedById()).isEqualTo(1L);
        assertThat(assignment.getCreatedAt()).isEqualTo(NOW);
        assertThat(assignment.getRevokedById()).isNull();
        assertThat(assignment.getRevokedAt()).isNull();
    }

    @Test
    void shouldMarkRevoked_whenRevokeInvoked() {
        // Arrange
        FixedAssignment assignment = FixedAssignment.create(8L, 15L, 3, 1L, NOW);

        // Act
        assignment.revoke(2L, LATER);

        // Assert
        assertThat(assignment.isActive()).isFalse();
        assertThat(assignment.getRevokedById()).isEqualTo(2L);
        assertThat(assignment.getRevokedAt()).isEqualTo(LATER);
    }

    @Test
    void shouldBeEqualById_whenSameIdentifier() throws Exception {
        // Arrange
        FixedAssignment a = withId(FixedAssignment.create(8L, 15L, 3, 1L, NOW), 42L);
        FixedAssignment b = withId(FixedAssignment.create(9L, 16L, 4, 1L, NOW), 42L);
        FixedAssignment other = withId(FixedAssignment.create(8L, 15L, 3, 1L, NOW), 43L);

        // Assert
        assertThat(a)
                .isEqualTo(a)
                .isEqualTo(b)
                .isNotEqualTo(other)
                .isNotEqualTo(null)
                .isNotEqualTo("no-es-una-asignacion");
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    void shouldNotBeEqual_whenIdIsNull() {
        // Arrange: dos entidades transitorias (id null) nunca son iguales entre si
        FixedAssignment a = FixedAssignment.create(8L, 15L, 3, 1L, NOW);
        FixedAssignment b = FixedAssignment.create(8L, 15L, 3, 1L, NOW);

        // Assert
        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isZero();
    }

    private static FixedAssignment withId(FixedAssignment assignment, long id) throws Exception {
        Field field = FixedAssignment.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(assignment, id);
        return assignment;
    }
}
