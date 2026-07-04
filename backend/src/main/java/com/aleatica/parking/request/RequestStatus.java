package com.aleatica.parking.request;

/**
 * Estados del ciclo de vida de una solicitud (schema {@code RequestStatus} de la
 * API, autoridad {@code docs/openapi.yaml}).
 *
 * <p>Maquina de estados: {@link #PENDING} es el unico estado inicial y de origen;
 * desde el se transita a {@link #APPROVED}, {@link #REJECTED} o {@link #CANCELLED}.
 * Los tres ultimos son terminales (sin transiciones salientes).</p>
 */
public enum RequestStatus {

    /** Solicitud creada, pendiente de resolucion; {@code parking_space_id = NULL}. */
    PENDING,

    /** Aprobada por un administrador, con plaza asignada. */
    APPROVED,

    /** Rechazada por un administrador, con motivo del catalogo. */
    REJECTED,

    /** Cancelada por el propio empleado mientras estaba en {@link #PENDING}. */
    CANCELLED
}
