package com.aleatica.parking.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de la entidad {@link Visitor}: fabrica de alta, edicion de los campos
 * mutables y contrato {@code equals}/{@code hashCode} basado en el identificador. No toca
 * la base de datos.
 */
class VisitorTest {

    @Test
    void shouldCreateVisitor_whenFactoryUsed() {
        // Act
        Visitor visitor = Visitor.create("Ada", "Lovelace", "X1234567Z", "1234ABC", "Contoso",
                "Reunion", 1L);

        // Assert
        assertThat(visitor.getFirstName()).isEqualTo("Ada");
        assertThat(visitor.getLastName()).isEqualTo("Lovelace");
        assertThat(visitor.getNationalId()).isEqualTo("X1234567Z");
        assertThat(visitor.getLicensePlate()).isEqualTo("1234ABC");
        assertThat(visitor.getCompany()).isEqualTo("Contoso");
        assertThat(visitor.getUsualReason()).isEqualTo("Reunion");
        assertThat(visitor.getCreatedById()).isEqualTo(1L);
    }

    @Test
    void shouldUpdateMutableFields_whenUpdateUsed() {
        // Arrange
        Visitor visitor = Visitor.create("Ada", "Lovelace", "X1234567Z", "OLD", null, null, 1L);

        // Act
        visitor.update("Grace", "Hopper", "Y7654321X", "NEW", "Navy", "Auditoria");

        // Assert
        assertThat(visitor.getFirstName()).isEqualTo("Grace");
        assertThat(visitor.getLastName()).isEqualTo("Hopper");
        assertThat(visitor.getNationalId()).isEqualTo("Y7654321X");
        assertThat(visitor.getLicensePlate()).isEqualTo("NEW");
        assertThat(visitor.getCompany()).isEqualTo("Navy");
        assertThat(visitor.getUsualReason()).isEqualTo("Auditoria");
    }

    @Test
    void shouldBeEqualById_whenSameIdentifier() throws Exception {
        // Arrange
        Visitor a = withId(Visitor.create("Ada", "L", "X1", null, null, null, 1L), 42L);
        Visitor b = withId(Visitor.create("Grace", "H", "Y2", null, null, null, 1L), 42L);
        Visitor other = withId(Visitor.create("Ada", "L", "X1", null, null, null, 1L), 43L);

        // Assert
        assertThat(a)
                .isEqualTo(a)
                .isEqualTo(b)
                .isNotEqualTo(other)
                .isNotEqualTo(null)
                .isNotEqualTo("no-es-un-visitante");
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    void shouldNotBeEqual_whenIdIsNull() {
        // Arrange: dos entidades transitorias (id null) nunca son iguales entre si
        Visitor a = Visitor.create("Ada", "L", "X1", null, null, null, 1L);
        Visitor b = Visitor.create("Ada", "L", "X1", null, null, null, 1L);

        // Assert
        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isZero();
    }

    private static Visitor withId(Visitor visitor, long id) throws Exception {
        Field field = Visitor.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(visitor, id);
        return visitor;
    }
}
