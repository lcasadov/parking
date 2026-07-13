package com.aleatica.parking.employee.dto;

import com.aleatica.parking.employee.AuthOrigin;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeCategory;
import com.aleatica.parking.employee.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Representacion de salida de un empleado (schema {@code Employee} de la API).
 *
 * <p>Es un DTO, no la entidad JPA: nunca expone {@code passwordHash},
 * {@code failedLoginAttempts} ni {@code lockedUntil} (security-design §1 /
 * OWASP API3 — exposicion excesiva de datos).</p>
 *
 * @param id                 identificador
 * @param firstName          nombre
 * @param lastName           apellidos
 * @param login              login unico
 * @param email              email unico
 * @param department         departamento (nulable)
 * @param mobilePhone        telefono movil (nulable)
 * @param licensePlate       matricula (nulable)
 * @param corporate          si es empleado corporativo (con EntraID)
 * @param authOrigin         origen de autenticacion
 * @param role               rol funcional
 * @param category           categoria jerarquica (rango organizativo)
 * @param enabled            si la cuenta puede iniciar sesion
 * @param active             si no esta dado de baja logicamente
 * @param passwordMustChange si debe cambiar la contrasena en el proximo acceso
 * @param createdAt          instante de alta (UTC)
 * @param updatedAt          instante de ultima modificacion (UTC, nulable)
 */
@Schema(description = "Datos de un empleado del sistema")
public record EmployeeResponse(
        @Schema(description = "Identificador unico", example = "42") Long id,
        @Schema(description = "Nombre", example = "Juan") String firstName,
        @Schema(description = "Apellidos", example = "Perez Gomez") String lastName,
        @Schema(description = "Login unico", example = "jperez") String login,
        @Schema(description = "Email unico", example = "jperez@aleatica.com") String email,
        @Schema(description = "Departamento") String department,
        @Schema(description = "Telefono movil") String mobilePhone,
        @Schema(description = "Matricula del vehiculo") String licensePlate,
        @Schema(description = "Empleado corporativo (con EntraID)")
        @JsonProperty("isCorporate") boolean corporate,
        @Schema(description = "Origen de autenticacion") AuthOrigin authOrigin,
        @Schema(description = "Rol funcional") Role role,
        @Schema(description = "Categoria jerarquica (rango organizativo)") EmployeeCategory category,
        @Schema(description = "Cuenta habilitada para login") boolean enabled,
        @Schema(description = "No dado de baja logicamente") boolean active,
        @Schema(description = "Debe cambiar la contrasena al acceder") boolean passwordMustChange,
        @Schema(description = "Instante de alta (ISO-8601)") Instant createdAt,
        @Schema(description = "Instante de ultima modificacion (ISO-8601)") Instant updatedAt) {

    /**
     * Mapea la entidad de persistencia a su DTO de salida.
     *
     * @param employee entidad origen
     * @return el DTO equivalente
     */
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getLogin(),
                employee.getEmail(),
                employee.getDepartment(),
                employee.getMobilePhone(),
                employee.getLicensePlate(),
                employee.isCorporate(),
                employee.getAuthOrigin(),
                employee.getRole(),
                employee.getCategory(),
                employee.isEnabled(),
                employee.isActive(),
                employee.isPasswordMustChange(),
                employee.getCreatedAt(),
                employee.getUpdatedAt());
    }
}
