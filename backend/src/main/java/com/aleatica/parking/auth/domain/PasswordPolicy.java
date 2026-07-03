package com.aleatica.parking.auth.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Politica de contrasena de Fase 1 (regla de dominio pura, sin Spring).
 *
 * <p>Una contrasena es valida si cumple TODAS estas condiciones
 * ({@code docs/security-design.md} §2):</p>
 * <ul>
 *   <li>longitud &ge; 10 caracteres;</li>
 *   <li>al menos una mayuscula, una minuscula, un digito y un simbolo;</li>
 *   <li>distinta (ignorando mayusculas/minusculas) del {@code login} y del {@code email}.</li>
 * </ul>
 *
 * <p>No hay caducidad obligatoria en Fase 1. La clase es {@code final} y sin
 * estado: se expone como un conjunto de funciones de validacion reutilizables
 * por el caso de uso de cambio de contrasena.</p>
 */
public final class PasswordPolicy {

    /** Longitud minima exigida a la contrasena. */
    public static final int MIN_LENGTH = 10;

    private PasswordPolicy() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Valida una contrasena contra la politica.
     *
     * @param password contrasena en claro propuesta (puede ser {@code null})
     * @param login    login del empleado (la contrasena no puede coincidir)
     * @param email    email del empleado (la contrasena no puede coincidir)
     * @return lista de violaciones legibles; vacia si la contrasena cumple la politica
     */
    public static List<String> validate(String password, String login, String email) {
        List<String> violations = new ArrayList<>();
        if (password == null || password.length() < MIN_LENGTH) {
            violations.add("Debe tener al menos " + MIN_LENGTH + " caracteres");
        }
        if (password != null) {
            addCharClassViolations(password, violations);
            addEqualityViolations(password, login, email, violations);
        }
        return violations;
    }

    /**
     * Indica si la contrasena cumple integramente la politica.
     *
     * @param password contrasena en claro propuesta
     * @param login    login del empleado
     * @param email    email del empleado
     * @return {@code true} si no hay ninguna violacion
     */
    public static boolean isValid(String password, String login, String email) {
        return validate(password, login, email).isEmpty();
    }

    private static void addCharClassViolations(String password, List<String> violations) {
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            violations.add("Debe contener al menos una mayuscula");
        }
        if (!password.chars().anyMatch(Character::isLowerCase)) {
            violations.add("Debe contener al menos una minuscula");
        }
        if (!password.chars().anyMatch(Character::isDigit)) {
            violations.add("Debe contener al menos un digito");
        }
        if (!password.chars().anyMatch(PasswordPolicy::isSymbol)) {
            violations.add("Debe contener al menos un simbolo");
        }
    }

    private static void addEqualityViolations(
            String password, String login, String email, List<String> violations) {
        if (login != null && password.equalsIgnoreCase(login)) {
            violations.add("No puede ser igual al login");
        }
        if (email != null && password.equalsIgnoreCase(email)) {
            violations.add("No puede ser igual al email");
        }
    }

    private static boolean isSymbol(int ch) {
        return !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch);
    }
}
