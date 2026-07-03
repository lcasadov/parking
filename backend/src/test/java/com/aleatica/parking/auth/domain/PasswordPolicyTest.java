package com.aleatica.parking.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de la politica de contrasena (regla de dominio pura).
 * Flujo critico de autenticacion: cobertura exhaustiva de cada condicion.
 */
class PasswordPolicyTest {

    private static final String LOGIN = "jperez";
    private static final String EMAIL = "jperez@aleatica.com";

    @Test
    void shouldReturnNoViolations_whenPasswordMeetsAllRules() {
        // Arrange
        String password = "Valid#Pass1word";

        // Act
        List<String> violations = PasswordPolicy.validate(password, LOGIN, EMAIL);

        // Assert
        assertThat(violations).isEmpty();
        assertThat(PasswordPolicy.isValid(password, LOGIN, EMAIL)).isTrue();
    }

    @Test
    void shouldReportLength_whenPasswordShorterThanMinimum() {
        // Arrange
        String password = "Ab1#cde";

        // Act
        List<String> violations = PasswordPolicy.validate(password, LOGIN, EMAIL);

        // Assert
        assertThat(violations).anyMatch(v -> v.contains("al menos 10"));
    }

    @Test
    void shouldReportMissingUppercase_whenNoUppercasePresent() {
        // Act
        List<String> violations = PasswordPolicy.validate("lower#pass1word", LOGIN, EMAIL);

        // Assert
        assertThat(violations).anyMatch(v -> v.contains("mayuscula"));
    }

    @Test
    void shouldReportMissingLowercase_whenNoLowercasePresent() {
        // Act
        List<String> violations = PasswordPolicy.validate("UPPER#PASS1WORD", LOGIN, EMAIL);

        // Assert
        assertThat(violations).anyMatch(v -> v.contains("minuscula"));
    }

    @Test
    void shouldReportMissingDigit_whenNoDigitPresent() {
        // Act
        List<String> violations = PasswordPolicy.validate("NoDigit#Password", LOGIN, EMAIL);

        // Assert
        assertThat(violations).anyMatch(v -> v.contains("digito"));
    }

    @Test
    void shouldReportMissingSymbol_whenNoSymbolPresent() {
        // Act
        List<String> violations = PasswordPolicy.validate("NoSymbol1Password", LOGIN, EMAIL);

        // Assert
        assertThat(violations).anyMatch(v -> v.contains("simbolo"));
    }

    @Test
    void shouldReportEqualToLogin_whenPasswordEqualsLoginIgnoringCase() {
        // Arrange: a password that otherwise complies but equals the login
        String login = "Strong#Pass1";

        // Act
        List<String> violations = PasswordPolicy.validate("strong#pass1", login, EMAIL);

        // Assert
        assertThat(violations).anyMatch(v -> v.contains("login"));
    }

    @Test
    void shouldReportEqualToEmail_whenPasswordEqualsEmailIgnoringCase() {
        // Arrange
        String email = "Strong#Pass1word";

        // Act
        List<String> violations = PasswordPolicy.validate("strong#pass1word", LOGIN, email);

        // Assert
        assertThat(violations).anyMatch(v -> v.contains("email"));
    }

    @Test
    void shouldReportLengthAndClasses_whenPasswordIsNull() {
        // Act
        List<String> violations = PasswordPolicy.validate(null, LOGIN, EMAIL);

        // Assert: null is treated as too short, no NPE
        assertThat(violations).anyMatch(v -> v.contains("al menos 10"));
        assertThat(PasswordPolicy.isValid(null, LOGIN, EMAIL)).isFalse();
    }

    @Test
    void shouldNotFailEquality_whenLoginAndEmailAreNull() {
        // Act
        List<String> violations = PasswordPolicy.validate("Valid#Pass1word", null, null);

        // Assert
        assertThat(violations).isEmpty();
    }
}
