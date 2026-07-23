package com.aleatica.parking.employee.dto;

import com.aleatica.parking.employee.Employee;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Opcion minima de empleado (id + nombre completo) para poblar un selector, sin exponer
 * datos personales de contacto ni credenciales.
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Lo consume el selector del flujo de
 * liberacion administrativa (ADMIN/AGENCIA), independiente del CRUD de empleados (ADMIN-only):
 * por eso solo lleva {@code id} y {@code fullName}, la informacion imprescindible para elegir
 * a quien liberar. Cada componente viaja bajo su clave contractual exacta fijada con
 * {@link JsonProperty}.</p>
 *
 * @param id       identificador del empleado
 * @param fullName nombre completo del empleado ({@code firstName + " " + lastName})
 */
@Schema(description = "Opcion minima de empleado (id + nombre) para un selector")
public record EmployeeOptionResponse(
        @Schema(description = "Identificador del empleado", example = "15")
        @JsonProperty("id") Long id,

        @Schema(description = "Nombre completo del empleado", example = "Ada Lovelace")
        @JsonProperty("fullName") String fullName) {

    /**
     * Proyecta un empleado a su opcion minima de seleccion.
     *
     * @param employee empleado a proyectar
     * @return la opcion con {@code id} y nombre completo
     */
    public static EmployeeOptionResponse from(Employee employee) {
        return new EmployeeOptionResponse(
                employee.getId(), employee.getFirstName() + " " + employee.getLastName());
    }
}
