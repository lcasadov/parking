package com.aleatica.parking.floorplan;

/**
 * Estado de un puesto en el plano para una fecha, relativo al empleado que consulta.
 *
 * <p>Estado derivado (no persistido): se calcula en lectura a partir de las asignaciones
 * fijas, solicitudes y liberaciones del puesto para la fecha (design §Decisions), por lo
 * que depende del solicitante ({@link #MINE}). No revela titulares ajenos: un puesto de un
 * tercero se devuelve como {@link #ASSIGNED} (o {@link #REQUESTED}) sin identificar al
 * titular, y solo el propio aparece como {@link #MINE} (RGPD / minimizacion,
 * {@code docs/security-design.md}).</p>
 */
public enum FloorPlanDeskState {

    /** Libre para la fecha: solicitable pinchandolo en el plano. */
    FREE,

    /** Ocupado por un tercero (asignacion fija vigente o solicitud aprobada); sin titular. */
    ASSIGNED,

    /** Solicitado por un tercero (solicitud de puesto pendiente para esa fecha); sin titular. */
    REQUESTED,

    /** Del propio solicitante para la fecha (asignacion fija, solicitud aprobada o pendiente). */
    MINE,

    /** Puesto fijo liberado para la fecha: vuelve a estar disponible para solicitud. */
    RELEASED
}
