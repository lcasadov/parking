package com.aleatica.parking.resource;

/**
 * Abstraccion de dominio de un recurso reservable, comun a plazas de parking y
 * (en el futuro) puestos de oficina.
 *
 * <p>Es el concepto compartido sobre el que operan la asignacion fija, la solicitud,
 * la liberacion y el calculo de disponibilidad sin acoplarse a una tabla concreta.
 * Hoy la materializa {@code ParkingSpace} (tipo {@link ResourceType#PARKING});
 * cuando llegue la capability {@code desks} la materializara tambien {@code Desk}
 * (tipo {@link ResourceType#DESK}), reutilizando la misma logica de dominio.</p>
 */
public interface BookableResource {

    /**
     * @return identificador del recurso dentro de su tabla concreta ({@code resource_id})
     */
    Long getResourceId();

    /**
     * @return tipo del recurso, discriminador entre las materializaciones concretas
     */
    ResourceType getResourceType();

    /**
     * @return etiqueta humana del recurso (p. ej. {@code P-08})
     */
    String getLabel();

    /**
     * @return {@code true} si el recurso esta activo (elegible para reserva)
     */
    boolean isActive();
}
