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

    @Test
    void shouldDeriveLabelFromNumber_whenCreatedByNumber() {
        // Act
        ParkingSpace space = ParkingSpace.create(1007);

        // Assert
        assertThat(space.getNumber()).isEqualTo(1007);
        assertThat(space.getLabel()).isEqualTo("1007");
        assertThat(space.isActive()).isTrue();
    }

    @Test
    void shouldDeriveFloorAsNumberDividedByThousand() {
        // Assert: floor = number / 1000 (division entera)
        assertThat(ParkingSpace.create(1007).floor()).isEqualTo(1);
        assertThat(ParkingSpace.create(2001).floor()).isEqualTo(2);
        assertThat(ParkingSpace.create(3025).floor()).isEqualTo(3);
        assertThat(ParkingSpace.create(12010).floor()).isEqualTo(12);
    }

    @Test
    void shouldRecalculateLabelAndFloor_whenNumberChanges() {
        // Arrange
        ParkingSpace space = ParkingSpace.create(1001);

        // Act
        space.setNumber(2003);

        // Assert
        assertThat(space.getLabel()).isEqualTo("2003");
        assertThat(space.floor()).isEqualTo(2);
    }

    @Test
    void shouldReturnNullFloor_whenNumberNotAssigned() {
        // Act: fabrica de compatibilidad por label, sin numero
        ParkingSpace space = ParkingSpace.create(LABEL);

        // Assert
        assertThat(space.getNumber()).isNull();
        assertThat(space.floor()).isNull();
    }
}
