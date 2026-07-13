package com.aleatica.parking.employee;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Fija el contrato del enum {@link EmployeeCategory}: 7 valores en el orden
 * jerarquico exacto (mayor a menor rango). El orden es semantico (los consumidores
 * comparan por {@code ordinal()}), asi que un reordenamiento accidental debe romper
 * este test.
 */
class EmployeeCategoryTest {

    @Test
    void shouldDeclareSevenCategoriesInHierarchicalOrder() {
        // Act
        EmployeeCategory[] values = EmployeeCategory.values();

        // Assert
        assertThat(values).containsExactly(
                EmployeeCategory.CEO,
                EmployeeCategory.CONSEJO,
                EmployeeCategory.DIRECTOR_N1,
                EmployeeCategory.DIRECTOR_N2,
                EmployeeCategory.GERENTE,
                EmployeeCategory.MANDO_INTERMEDIO,
                EmployeeCategory.EMPLEADO);
    }

    @Test
    void shouldRankCeoAboveEmpleado() {
        // Assert: menor ordinal = mayor rango
        assertThat(EmployeeCategory.CEO.ordinal())
                .isLessThan(EmployeeCategory.EMPLEADO.ordinal());
    }
}
