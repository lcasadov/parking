package com.aleatica.parking.release;

/**
 * Tipo de liberacion de un recurso con asignacion fija (schema {@code ReleaseType}
 * de la API; CHECK {@code CK_releases_type} de {@code data-model.md §3.4}).
 *
 * <p>El valor se persiste por su nombre ({@code @Enumerated(EnumType.STRING)}),
 * identico al catalogo del CHECK de la BD.</p>
 */
public enum ReleaseType {

    /** Liberacion ejecutada por el propio titular de la asignacion fija ({@code reason} nulo). */
    VOLUNTARY,

    /** Liberacion ejecutada por un {@code ADMIN} sobre el recurso de otro empleado ({@code reason} obligatorio). */
    ADMINISTRATIVE
}
