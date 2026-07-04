package com.aleatica.parking.visitor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de entrada de {@code POST createVisitor} y {@code PUT updateVisitor}
 * (schema {@code VisitorCreateRequest} de la API, autoridad {@code docs/openapi.yaml}).
 *
 * <p>La validacion sintactica ({@code @NotBlank} sobre los campos obligatorios y
 * {@code @Size} sobre las longitudes) es la primera capa (UX y 400); la unicidad de
 * {@code nationalId} la verifica el caso de uso (409, frontera de seguridad OWASP A04).
 * Las claves contractuales se fijan con {@link JsonProperty} para que Jackson no derive
 * otra clave del record.</p>
 *
 * @param firstName    nombre (obligatorio, 1..100)
 * @param lastName     apellidos (obligatorio, 1..150)
 * @param nationalId   documento de identidad, clave natural unica (obligatorio, 1..20)
 * @param licensePlate matricula (opcional, max 15)
 * @param company      empresa (opcional, max 150)
 * @param usualReason  motivo habitual de visita (opcional, max 255)
 */
@Schema(description = "Peticion de alta/edicion de una ficha de visitante")
public record VisitorCreateRequest(
        @Schema(description = "Nombre", example = "Ada", maxLength = 100)
        @JsonProperty("firstName")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String firstName,

        @Schema(description = "Apellidos", example = "Lovelace", maxLength = 150)
        @JsonProperty("lastName")
        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 150, message = "Los apellidos no pueden superar 150 caracteres")
        String lastName,

        @Schema(description = "Documento de identidad (clave natural unica)", example = "X1234567Z",
                maxLength = 20)
        @JsonProperty("nationalId")
        @NotBlank(message = "El documento de identidad es obligatorio")
        @Size(max = 20, message = "El documento de identidad no puede superar 20 caracteres")
        String nationalId,

        @Schema(description = "Matricula", example = "1234ABC", maxLength = 15)
        @JsonProperty("licensePlate")
        @Size(max = 15, message = "La matricula no puede superar 15 caracteres")
        String licensePlate,

        @Schema(description = "Empresa", example = "Contoso", maxLength = 150)
        @JsonProperty("company")
        @Size(max = 150, message = "La empresa no puede superar 150 caracteres")
        String company,

        @Schema(description = "Motivo habitual de visita", example = "Reunion comercial", maxLength = 255)
        @JsonProperty("usualReason")
        @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
        String usualReason) {
}
