package com.aleatica.parking.availability;

/**
 * Origen por el que un recurso queda ocupado para una fecha en la vista de ocupacion
 * ("Liberar por fecha").
 *
 * <p>Distingue las fuentes de ocupacion que el {@code ADMIN} ve al elegir una fecha: la
 * asignacion fija del titular ({@link #FIXED_ASSIGNMENT}), una solicitud puntual aprobada
 * ({@link #REQUEST_APPROVED}) y una reserva de visitante ({@link #VISITOR_RESERVATION}). Las
 * dos primeras son liberables administrativamente; la reserva de visitante se anula (no se
 * libera) y su ocupante es el visitante, no un empleado.</p>
 */
public enum OccupancyOrigin {

    /** El recurso lo ocupa su titular por una asignacion fija vigente ese dia (no liberada). */
    FIXED_ASSIGNMENT,

    /** El recurso lo ocupa un empleado por una solicitud puntual aprobada para esa fecha. */
    REQUEST_APPROVED,

    /** El recurso lo ocupa un visitante por una reserva para esa fecha (plaza o puesto). */
    VISITOR_RESERVATION
}
