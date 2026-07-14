package com.aleatica.parking.availability;

/**
 * Origen por el que un recurso queda ocupado para una fecha en la vista de ocupacion
 * ("Liberar por fecha").
 *
 * <p>Distingue las dos fuentes de ocupacion que el {@code ADMIN} ve al elegir una fecha:
 * la asignacion fija del titular ({@link #FIXED_ASSIGNMENT}) y una solicitud puntual
 * aprobada ({@link #REQUEST_APPROVED}). La reserva de visitante no entra en esta vista
 * (no es un recurso con asignacion fija liberable administrativamente).</p>
 */
public enum OccupancyOrigin {

    /** El recurso lo ocupa su titular por una asignacion fija vigente ese dia (no liberada). */
    FIXED_ASSIGNMENT,

    /** El recurso lo ocupa un empleado por una solicitud puntual aprobada para esa fecha. */
    REQUEST_APPROVED
}
