package com.aleatica.parking.employee;

/**
 * Tipo de evento del histórico de un vehículo de empleado (change
 * {@code employee-vehicle-self-service}, Fase 2).
 */
public enum VehicleHistoryEventType {

    /** Alta del vehículo. */
    CREATED,

    /** Edición de los datos por el empleado (guarda una foto de los datos previos). */
    EDITED,

    /** Cambio de estado por el administrador (en trámite / aprobado / rechazado / restaurado). */
    STATUS_CHANGED,

    /** El empleado ha solicitado el borrado (queda pendiente de borrado). */
    DELETION_REQUESTED
}
