package com.aleatica.parking.availability;

/**
 * Estado diario de la vista "Mi Semana" (schema {@code MyWeekDayState} de la API,
 * autoridad {@code docs/openapi.yaml}).
 *
 * <p>"Mi Semana" es centrada en el empleado y el dia (no en la plaza): refleja unicamente
 * los recursos propios del solicitante. Una solicitud aprobada del propio empleado se
 * proyecta como {@link #ASSIGNED} (tiene recurso ese dia) con el estado de la solicitud
 * viajando aparte; por eso este enum no incluye {@code REQUEST_APPROVED}. Nunca expone
 * identidad de terceros (privacidad, {@code docs/security-design.md}).</p>
 */
public enum MyWeekDayState {

    /** El empleado dispone de recurso ese dia (asignacion fija vigente o solicitud aprobada). */
    ASSIGNED,

    /** El empleado tiene asignacion fija ese dia pero la ha liberado para esa fecha. */
    RELEASED,

    /** El empleado tiene una solicitud puntual pendiente para esa fecha. */
    REQUEST_PENDING,

    /** El empleado no tiene recurso ni solicitud para esa fecha. */
    FREE
}
