package com.aleatica.parking.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de la entidad {@link VisitorReservation}: fabrica de alta y contrato
 * {@code equals}/{@code hashCode} basado en el identificador. No toca la base de datos.
 */
class VisitorReservationTest {

    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");
    private static final LocalDate DATE = LocalDate.of(2026, 7, 10);

    @Test
    void shouldCreateReservation_whenFactoryUsed() {
        // Act
        VisitorReservation reservation = VisitorReservation.create(42L, 8L, DATE, "Puerta norte", 1L, NOW);

        // Assert
        assertThat(reservation.getVisitorId()).isEqualTo(42L);
        assertThat(reservation.getParkingSpaceId()).isEqualTo(8L);
        assertThat(reservation.getReservationDate()).isEqualTo(DATE);
        assertThat(reservation.getNotes()).isEqualTo("Puerta norte");
        assertThat(reservation.getCreatedById()).isEqualTo(1L);
        assertThat(reservation.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldBeEqualById_whenSameIdentifier() throws Exception {
        // Arrange
        VisitorReservation a = withId(VisitorReservation.create(42L, 8L, DATE, null, 1L, NOW), 7L);
        VisitorReservation b = withId(VisitorReservation.create(43L, 9L, DATE, "x", 1L, NOW), 7L);
        VisitorReservation other = withId(VisitorReservation.create(42L, 8L, DATE, null, 1L, NOW), 8L);

        // Assert
        assertThat(a)
                .isEqualTo(a)
                .isEqualTo(b)
                .isNotEqualTo(other)
                .isNotEqualTo(null)
                .isNotEqualTo("no-es-una-reserva");
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    void shouldNotBeEqual_whenIdIsNull() {
        // Arrange: dos entidades transitorias (id null) nunca son iguales entre si
        VisitorReservation a = VisitorReservation.create(42L, 8L, DATE, null, 1L, NOW);
        VisitorReservation b = VisitorReservation.create(42L, 8L, DATE, null, 1L, NOW);

        // Assert
        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isZero();
    }

    private static VisitorReservation withId(VisitorReservation reservation, long id) throws Exception {
        Field field = VisitorReservation.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(reservation, id);
        return reservation;
    }
}
