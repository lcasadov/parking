package com.aleatica.parking.export;

/**
 * Sanitizador de inyeccion de formulas en hojas de calculo (CSV injection / OWASP).
 *
 * <p>Una celda cuyo valor empieza por {@code =}, {@code +}, {@code -}, {@code @} o por un
 * caracter de control ({@code \t} tab, {@code \r} retorno de carro) puede interpretarse como
 * formula al abrir el fichero en Excel/LibreOffice, ejecutando codigo en el equipo del
 * destinatario. Se neutraliza anteponiendo un apostrofo ({@code '}), que fuerza a la hoja a
 * tratar el contenido como texto literal. Se aplica de forma uniforme a las celdas de datos
 * de CSV y a las celdas de texto de XLSX (defensa en profundidad).</p>
 */
public final class FormulaSanitizer {

    private static final char PREFIX = '\'';

    private FormulaSanitizer() {
        // Utilidad estatica: sin instancias.
    }

    /**
     * Antepone un apostrofo si el valor comienza por un caracter peligroso de formula.
     *
     * @param value valor de la celda; puede ser {@code null}
     * @return el valor saneado; {@code ""} si el original es {@code null}
     */
    public static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }
        return isDangerous(value.charAt(0)) ? PREFIX + value : value;
    }

    private static boolean isDangerous(char first) {
        return first == '=' || first == '+' || first == '-' || first == '@'
                || first == '\t' || first == '\r';
    }
}
