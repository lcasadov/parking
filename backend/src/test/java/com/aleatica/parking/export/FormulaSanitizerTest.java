package com.aleatica.parking.export;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests unitarios del sanitizador de inyeccion de formulas (edge case 1.11): un valor que
 * empieza por un caracter peligroso ({@code = + - @ \t \r}) se antepone con un apostrofo;
 * el resto se deja intacto; {@code null} se normaliza a cadena vacia.
 */
class FormulaSanitizerTest {

    @ParameterizedTest
    @ValueSource(strings = {"=SUM(A1:A2)", "+1+1", "-5", "@cmd", "\tvalue", "\rvalue"})
    void shouldPrefixWithApostrophe_whenValueStartsWithFormulaChar(String dangerous) {
        // Act
        String sanitized = FormulaSanitizer.sanitize(dangerous);

        // Assert
        assertThat(sanitized).startsWith("'").isEqualTo("'" + dangerous);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Juan", "jperez@aleatica.com", "1234ABC", "2026-07-04", "0"})
    void shouldLeaveValueUnchanged_whenValueIsSafe(String safe) {
        // Act / Assert
        assertThat(FormulaSanitizer.sanitize(safe)).isEqualTo(safe);
    }

    @Test
    void shouldReturnEmptyString_whenValueIsNull() {
        // Act / Assert
        assertThat(FormulaSanitizer.sanitize(null)).isEmpty();
    }

    @Test
    void shouldReturnEmptyString_whenValueIsEmpty() {
        // Act / Assert
        assertThat(FormulaSanitizer.sanitize("")).isEmpty();
    }
}
