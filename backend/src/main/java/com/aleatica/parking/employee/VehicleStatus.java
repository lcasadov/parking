package com.aleatica.parking.employee;

/**
 * Estado de validación / ciclo de vida de un vehículo de empleado (change
 * {@code employee-vehicle-self-service}).
 *
 * <p>Un vehículo dado de alta o modificado por el propio empleado en el self-service queda
 * {@link #PENDING} hasta que un {@code ADMIN} lo pone {@link #IN_PROGRESS en trámite}, lo
 * {@link #APPROVED aprueba} o lo {@link #REJECTED rechaza} (con motivo en texto libre). El alta por
 * el {@code ADMIN} nace {@link #APPROVED}. Si el empleado borra un vehículo que está
 * {@link #IN_PROGRESS} o {@link #APPROVED}, no se borra directamente: queda
 * {@link #PENDING_DELETION pendiente de borrado} para que el {@code ADMIN} lo procese.</p>
 */
public enum VehicleStatus {

    /** Pendiente de validación por un administrador (alta/edición del empleado). */
    PENDING,

    /** En trámite: el administrador lo está gestionando (p.ej. alta con la mutua). */
    IN_PROGRESS,

    /** Validado (alta por admin, o aprobado por un admin en la bandeja de validación). */
    APPROVED,

    /** Rechazado por un administrador; conserva el motivo del rechazo (texto libre). */
    REJECTED,

    /**
     * Pendiente de borrado: el empleado pidió borrar un vehículo que estaba en trámite o aprobado;
     * el administrador debe confirmar el borrado o restaurarlo.
     */
    PENDING_DELETION
}
