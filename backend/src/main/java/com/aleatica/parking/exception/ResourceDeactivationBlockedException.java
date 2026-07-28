package com.aleatica.parking.exception;

/**
 * Senala que un recurso (plaza o puesto) NO puede desactivarse porque tiene asignaciones
 * vigentes o futuras: asignaciones fijas de empleado, solicitudes aprobadas para hoy o
 * fechas futuras, o reservas de visitante futuras (change {@code admin-improvements},
 * tarea 16: no desactivar silenciosamente).
 *
 * <p>El manejador global la traduce a {@code 409 Conflict} con el codigo
 * {@code RESOURCE_HAS_FUTURE_ASSIGNMENTS} y el detalle de cuantas asignaciones de cada tipo
 * afectan en {@code fields}, para que el frontend avise indicando a quien/que afecta antes de
 * poder desactivar.</p>
 */
public class ResourceDeactivationBlockedException extends RuntimeException {

    private final transient long fixedAssignments;
    private final transient long approvedRequests;
    private final transient long visitorReservations;

    /**
     * @param fixedAssignments    asignaciones fijas activas del recurso
     * @param approvedRequests    solicitudes aprobadas del recurso para hoy o futuro
     * @param visitorReservations reservas de visitante del recurso para hoy o futuro
     */
    public ResourceDeactivationBlockedException(
            long fixedAssignments, long approvedRequests, long visitorReservations) {
        super("El recurso tiene asignaciones vigentes o futuras y no puede desactivarse");
        this.fixedAssignments = fixedAssignments;
        this.approvedRequests = approvedRequests;
        this.visitorReservations = visitorReservations;
    }

    /** @return asignaciones fijas activas del recurso */
    public long getFixedAssignments() {
        return fixedAssignments;
    }

    /** @return solicitudes aprobadas del recurso para hoy o futuro */
    public long getApprovedRequests() {
        return approvedRequests;
    }

    /** @return reservas de visitante del recurso para hoy o futuro */
    public long getVisitorReservations() {
        return visitorReservations;
    }
}
