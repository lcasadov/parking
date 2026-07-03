package com.aleatica.parking.auth.application;

import java.util.List;

/**
 * La nueva contrasena incumple la politica -> {@code 400} con detalle por campo.
 *
 * <p>Transporta la lista de violaciones de {@code PasswordPolicy} para que el
 * manejador global las exponga en {@code ApiError.fields} bajo la clave
 * {@code newPassword}.</p>
 */
public class PasswordPolicyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<String> violations;

    /**
     * @param violations lista de incumplimientos de la politica (no vacia)
     */
    public PasswordPolicyException(List<String> violations) {
        super("La nueva contrasena no cumple la politica");
        this.violations = List.copyOf(violations);
    }

    /**
     * @return las violaciones de politica detectadas (lista inmutable)
     */
    public List<String> getViolations() {
        return violations;
    }
}
