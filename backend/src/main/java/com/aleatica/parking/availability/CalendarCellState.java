package com.aleatica.parking.availability;

/**
 * Estado de una celda del calendario semanal del {@code ADMIN} (schema
 * {@code CalendarCellState} de la API, autoridad {@code docs/openapi.yaml}).
 *
 * <p>El calendario admin es centrado en la plaza: cada celda es un par (plaza, dia). Por
 * eso {@link #REQUEST_APPROVED} es atribuible a una plaza concreta (la solicitud aprobada
 * ya lleva {@code parking_space_id}), mientras que las solicitudes {@code PENDING} no se
 * proyectan a una plaza (nacen sin plaza asignada) y por diseno no ocupan celda de plaza.</p>
 */
public enum CalendarCellState {

    /** La plaza tiene una asignacion fija vigente para el dia de la semana, sin liberar. */
    ASSIGNED,

    /** La plaza tiene asignacion fija ese dia pero esta liberada para esa fecha concreta. */
    RELEASED,

    /** Reservado por el contrato; no atribuible a plaza (las solicitudes PENDING nacen sin plaza). */
    REQUEST_PENDING,

    /** La plaza esta ocupada por una solicitud aprobada para esa fecha. */
    REQUEST_APPROVED,

    /** El recurso esta ocupado por una reserva de visitante para esa fecha (plaza o puesto). */
    VISITOR_RESERVATION,

    /** La plaza esta libre esa fecha (ni asignada vigente, ni ocupada por solicitud aprobada). */
    FREE
}
