package com.aleatica.parking.employee.application;

/**
 * Fase de despliegue del producto, que condiciona el comportamiento del reset
 * administrativo de contrasena.
 *
 * <p>Se resuelve de la propiedad {@code parking.phase} (por defecto
 * {@link #PHASE_1}). En Fase 1 el reset devuelve la contrasena temporal en la
 * respuesta; en Fase 2 se envia por email y no se expone en la API.</p>
 */
public enum Phase {

    /** Fase 1 🟢: sin canal de email garantizado; la temporal se muestra en pantalla. */
    PHASE_1,

    /** Fase 2 🔵: la temporal se envia por email y no se devuelve en claro. */
    PHASE_2
}
