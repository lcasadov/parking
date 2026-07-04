package com.aleatica.parking.visitor.dto;

import com.aleatica.parking.visitor.Visitor;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Representacion de salida de una ficha de visitante (schema {@code Visitor} de la API,
 * autoridad {@code docs/openapi.yaml}).
 *
 * <p>Es un DTO, no la entidad JPA (S4684 / OWASP API3). Cada componente viaja bajo su
 * clave contractual exacta fijada con {@link JsonProperty}, de modo que Jackson no
 * derive otra clave del record.</p>
 *
 * @param id           identificador
 * @param firstName    nombre
 * @param lastName     apellidos
 * @param nationalId   documento de identidad (clave natural unica)
 * @param licensePlate matricula; {@code null} si no aplica
 * @param company      empresa; {@code null} si no aplica
 * @param usualReason  motivo habitual de visita; {@code null} si no aplica
 * @param createdById  {@code ADMIN} que creo la ficha
 * @param createdAt    instante de creacion (UTC)
 */
@Schema(description = "Datos de una ficha de visitante")
public record VisitorResponse(
        @Schema(description = "Identificador unico", example = "42")
        @JsonProperty("id") Long id,

        @Schema(description = "Nombre", example = "Ada")
        @JsonProperty("firstName") String firstName,

        @Schema(description = "Apellidos", example = "Lovelace")
        @JsonProperty("lastName") String lastName,

        @Schema(description = "Documento de identidad", example = "X1234567Z")
        @JsonProperty("nationalId") String nationalId,

        @Schema(description = "Matricula; null si no aplica", example = "1234ABC")
        @JsonProperty("licensePlate") String licensePlate,

        @Schema(description = "Empresa; null si no aplica", example = "Contoso")
        @JsonProperty("company") String company,

        @Schema(description = "Motivo habitual de visita; null si no aplica", example = "Reunion comercial")
        @JsonProperty("usualReason") String usualReason,

        @Schema(description = "ADMIN que creo la ficha", example = "1")
        @JsonProperty("createdById") Long createdById,

        @Schema(description = "Instante de creacion (ISO-8601)")
        @JsonProperty("createdAt") Instant createdAt) {

    /**
     * Mapea la entidad de persistencia a su DTO de salida.
     *
     * @param visitor entidad origen
     * @return el DTO equivalente
     */
    public static VisitorResponse from(Visitor visitor) {
        return new VisitorResponse(
                visitor.getId(),
                visitor.getFirstName(),
                visitor.getLastName(),
                visitor.getNationalId(),
                visitor.getLicensePlate(),
                visitor.getCompany(),
                visitor.getUsualReason(),
                visitor.getCreatedById(),
                visitor.getCreatedAt());
    }
}
