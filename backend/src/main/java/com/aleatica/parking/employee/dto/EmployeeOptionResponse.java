package com.aleatica.parking.employee.dto;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Opcion minima de empleado (id + nombre completo + categoria) para poblar un selector, sin
 * exponer datos personales de contacto ni credenciales.
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Lo consume el selector del flujo de
 * liberacion administrativa (ADMIN/AGENCIA), independiente del CRUD de empleados (ADMIN-only):
 * por eso solo lleva {@code id}, {@code fullName} y {@code category} (esta ultima permite al
 * frontend mostrar el rango del empleado en el selector y anticipar la planta que le
 * correspondera en la auto-asignacion, sin exponer el resto de datos del empleado). Cada
 * componente viaja bajo su clave contractual exacta fijada con {@link JsonProperty}.</p>
 *
 * @param id       identificador del empleado
 * @param fullName nombre completo del empleado ({@code firstName + " " + lastName})
 * @param category categoria jerarquica del empleado (determina la planta de auto-asignacion)
 */
@Schema(description = "Opcion minima de empleado (id + nombre + categoria) para un selector")
public record EmployeeOptionResponse(
        @Schema(description = "Identificador del empleado", example = "15")
        @JsonProperty("id") Long id,

        @Schema(description = "Nombre completo del empleado", example = "Ada Lovelace")
        @JsonProperty("fullName") String fullName,

        @Schema(description = "Categoria jerarquica del empleado", example = "GERENTE")
        @JsonProperty("category") EmployeeCategory category) {

    /**
     * Proyecta un empleado a su opcion minima de seleccion.
     *
     * @param employee empleado a proyectar
     * @return la opcion con {@code id}, nombre completo y categoria
     */
    public static EmployeeOptionResponse from(Employee employee) {
        return new EmployeeOptionResponse(
                employee.getId(), employee.getFirstName() + " " + employee.getLastName(),
                employee.getCategory());
    }
}
