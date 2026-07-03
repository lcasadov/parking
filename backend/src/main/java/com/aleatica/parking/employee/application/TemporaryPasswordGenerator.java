package com.aleatica.parking.employee.application;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Genera contrasenas temporales seguras que cumplen la politica de contrasena
 * de Fase 1 ({@link com.aleatica.parking.auth.domain.PasswordPolicy}).
 *
 * <p>Usa {@link SecureRandom} (CSPRNG, OWASP A07): nunca {@code Math.random}.
 * Por construccion la contrasena incluye al menos una mayuscula, una minuscula,
 * un digito y un simbolo, y supera la longitud minima, de modo que valida la
 * politica sin necesidad de reintentos.</p>
 */
@Component
public class TemporaryPasswordGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%*?-_";
    private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;
    private static final int LENGTH = 16;

    private final SecureRandom random = new SecureRandom();

    /**
     * Genera una contrasena temporal aleatoria de {@value #LENGTH} caracteres que
     * cumple la politica de contrasena.
     *
     * @return la contrasena temporal en claro
     */
    public String generate() {
        List<Character> chars = new ArrayList<>(LENGTH);
        chars.add(pick(UPPER));
        chars.add(pick(LOWER));
        chars.add(pick(DIGITS));
        chars.add(pick(SYMBOLS));
        while (chars.size() < LENGTH) {
            chars.add(pick(ALL));
        }
        Collections.shuffle(chars, random);
        StringBuilder builder = new StringBuilder(LENGTH);
        for (char c : chars) {
            builder.append(c);
        }
        return builder.toString();
    }

    private char pick(String source) {
        return source.charAt(random.nextInt(source.length()));
    }
}
