package com.aleatica.parking.desk;

import static org.assertj.core.api.Assertions.assertThat;

import com.aleatica.parking.resource.ResourceType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de la entidad {@link Desk}: alta activa por defecto, materializacion de
 * {@code BookableResource} con tipo {@link ResourceType#DESK}, etiqueta derivada del numero
 * e igualdad por <em>business key</em> ({@code number}), no por el {@code id} autogenerado.
 */
class DeskTest {

    private static final Integer NUMBER = 5;
    private static final BigDecimal X = new BigDecimal("30.5");
    private static final BigDecimal Y = new BigDecimal("47.0");

    @Test
    void shouldBeActiveByDefault_whenCreated() {
        // Act
        Desk desk = Desk.create(NUMBER, DeskCategory.STANDARD, X, Y);

        // Assert
        assertThat(desk.getNumber()).isEqualTo(NUMBER);
        assertThat(desk.getCategory()).isEqualTo(DeskCategory.STANDARD);
        assertThat(desk.getCoordX()).isEqualByComparingTo(X);
        assertThat(desk.getCoordY()).isEqualByComparingTo(Y);
        assertThat(desk.isActive()).isTrue();
    }

    @Test
    void shouldMaterializeDeskResource_whenCreated() {
        // Act
        Desk desk = Desk.create(NUMBER, DeskCategory.EXECUTIVE, X, Y);

        // Assert: tipo DESK y etiqueta derivada del numero (D-05)
        assertThat(desk.getResourceType()).isEqualTo(ResourceType.DESK);
        assertThat(desk.getLabel()).isEqualTo("D-05");
    }

    @Test
    void shouldBeEqualByNumber_whenSameNumber() {
        // Arrange
        Desk one = Desk.create(NUMBER, DeskCategory.STANDARD, X, Y);
        Desk other = Desk.create(NUMBER, DeskCategory.EXECUTIVE, Y, X);

        // Assert: igualdad por numero, no por categoria ni coordenadas
        assertThat(one)
                .isEqualTo(one)
                .isEqualTo(other)
                .hasSameHashCodeAs(other);
    }

    @Test
    void shouldNotBeEqual_whenDifferentNumberOrType() {
        // Arrange
        Desk desk = Desk.create(NUMBER, DeskCategory.STANDARD, X, Y);

        // Assert
        assertThat(desk)
                .isNotEqualTo(Desk.create(6, DeskCategory.STANDARD, X, Y))
                .isNotEqualTo(null)
                .isNotEqualTo("D-05");
    }
}
