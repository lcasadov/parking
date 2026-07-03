package com.aleatica.parking.parkingspace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de la entidad {@link ParkingSpace}: alta activa por defecto e
 * igualdad por <em>business key</em> ({@code label}), no por el {@code id}
 * autogenerado.
 */
class ParkingSpaceTest {

    private static final String LABEL = "P-08";

    @Test
    void shouldBeActiveByDefault_whenCreated() {
        // Act
        ParkingSpace space = ParkingSpace.create(LABEL);

        // Assert
        assertThat(space.getLabel()).isEqualTo(LABEL);
        assertThat(space.isActive()).isTrue();
    }

    @Test
    void shouldBeEqualByLabel_whenSameLabel() {
        // Arrange
        ParkingSpace one = ParkingSpace.create(LABEL);
        ParkingSpace other = ParkingSpace.create(LABEL);

        // Assert
        assertThat(one)
                .isEqualTo(one)
                .isEqualTo(other)
                .hasSameHashCodeAs(other);
    }

    @Test
    void shouldNotBeEqual_whenDifferentLabelOrType() {
        // Arrange
        ParkingSpace space = ParkingSpace.create(LABEL);

        // Assert
        assertThat(space)
                .isNotEqualTo(ParkingSpace.create("P-09"))
                .isNotEqualTo(null)
                .isNotEqualTo("P-08");
    }
}
