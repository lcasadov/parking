package com.aleatica.parking.employee.dto;

import com.aleatica.parking.employee.AuthOrigin;
import com.aleatica.parking.employee.EmployeeCategory;
import com.aleatica.parking.employee.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de alta de un empleado (schema {@code EmployeeCreate} de la API).
 *
 * <p>La validacion sintactica ({@code @NotBlank}, {@code @Email}, {@code @Size})
 * es la primera barrera (UX); la unicidad y las reglas de negocio se validan en
 * el caso de uso (frontera de seguridad, OWASP A04).</p>
 *
 * @param firstName   nombre (obligatorio)
 * @param lastName    apellidos (obligatorio)
 * @param login       login unico (obligatorio)
 * @param email       email unico bien formado (obligatorio)
 * @param department  departamento (opcional)
 * @param mobilePhone telefono movil (opcional)
 * @param licensePlate matricula (opcional)
 * @param corporate   si es empleado corporativo
 * @param authOrigin  origen de autenticacion (por defecto {@code LOCAL})
 * @param role        rol funcional (obligatorio)
 * @param category    categoria jerarquica (obligatorio)
 */
@Schema(description = "Datos para dar de alta un empleado")
public record EmployeeCreateRequest(
        @Schema(description = "Nombre", example = "Juan")
        @NotBlank @Size(max = 100) String firstName,

        @Schema(description = "Apellidos", example = "Perez Gomez")
        @NotBlank @Size(max = 150) String lastName,

        @Schema(description = "Login unico", example = "jperez")
        @NotBlank @Size(max = 100) String login,

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

        @Schema(description = "Origen de autenticacion")
        AuthOrigin authOrigin,

        @Schema(description = "Rol funcional")
        @NotNull Role role,

        @Schema(description = "Categoria jerarquica (rango organizativo)")
        @NotNull EmployeeCategory category) {

    /**
     * @return el origen de autenticacion indicado o {@link AuthOrigin#LOCAL} por defecto
     */
    public AuthOrigin authOriginOrDefault() {
        return authOrigin == null ? AuthOrigin.LOCAL : authOrigin;
    }
}
