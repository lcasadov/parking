package com.aleatica.parking.employee.dto;

import com.aleatica.parking.employee.EmployeeCategory;
import com.aleatica.parking.employee.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de edicion de un empleado (schema {@code EmployeeUpdate} de la API).
 *
 * <p>El {@code login} es inmutable por contrato ({@code EmployeeUpdate} no lo
 * incluye): la identidad de acceso no cambia tras el alta. El {@code email} si
 * es editable y su unicidad se valida excluyendo al propio empleado.</p>
 *
 * @param firstName    nombre
 * @param lastName     apellidos
 * @param email        email unico bien formado
 * @param department   departamento (opcional)
 * @param mobilePhone  telefono movil (opcional)
 * @param licensePlate matricula (opcional)
 * @param corporate    si es empleado corporativo
 * @param role         rol funcional
 * @param category     categoria jerarquica (obligatorio)
 */
@Schema(description = "Datos para modificar un empleado")
public record EmployeeUpdateRequest(
        @Schema(description = "Nombre", example = "Juan")
        @NotBlank @Size(max = 100) String firstName,

        @Schema(description = "Apellidos", example = "Perez Gomez")
        @NotBlank @Size(max = 150) String lastName,

        @Schema(description = "Email unico", example = "jperez@aleatica.com")
        @NotBlank @Email @Size(max = 255) String email,

        @Schema(description = "Departamento")
        @Size(max = 100) String department,

        @Schema(description = "Telefono movil")
        @Size(max = 30) String mobilePhone,

        @Schema(description = "Matricula del vehiculo")
        @Size(max = 15) String licensePlate,

        @Schema(description = "Empleado corporativo (con EntraID)")
        @JsonProperty("isCorporate") boolean corporate,

        @Schema(description = "Rol funcional")
        @NotNull Role role,

        @Schema(description = "Categoria jerarquica (rango organizativo)")
        @NotNull EmployeeCategory category,

        @Schema(description = "Recibe avisos por email; null => true (change push-notifications)")
        @JsonProperty("emailNotificationsEnabled") Boolean emailNotificationsEnabled,

        @Schema(description = "Recibe avisos por push; null => true")
        @JsonProperty("pushNotificationsEnabled") Boolean pushNotificationsEnabled) {

    /** @return preferencia de email; por defecto {@code true} si se omite. */
    public boolean emailNotificationsEnabledOrDefault() {
        return emailNotificationsEnabled == null || emailNotificationsEnabled;
    }

    /** @return preferencia de push; por defecto {@code true} si se omite. */
    public boolean pushNotificationsEnabledOrDefault() {
        return pushNotificationsEnabled == null || pushNotificationsEnabled;
    }
}
