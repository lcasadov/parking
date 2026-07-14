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
     * Numero humano del recurso, el que el usuario reconoce (p. ej. {@code 3005} para una
     * plaza o {@code 12} para un puesto). Es distinto del {@code resource_id} interno de BD
     * ({@link #getResourceId()}); es este numero el que se muestra al empleado, no la PK.
     *
     * @return numero humano del recurso
     */
    Integer getNumber();

    /**
     * Planta a la que pertenece el recurso cuando su tipo la define; {@code null} para los
     * tipos sin planta derivada (p. ej. puestos de oficina).
     *
     * @return la planta del recurso, o {@code null} si no aplica
     */
    default Integer getFloor() {
        return null;
    }

    /**
     * @return {@code true} si el recurso esta activo (elegible para reserva)
     */
    boolean isActive();
}
