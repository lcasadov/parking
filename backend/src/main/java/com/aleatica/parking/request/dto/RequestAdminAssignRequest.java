package com.aleatica.parking.request.dto;

import com.aleatica.parking.resource.ResourceType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Cuerpo de entrada de {@code POST adminAssignRequest} (schema
 * {@code RequestAdminAssignRequest} de la API, change {@code restructure-admin-workflows},
 * capability {@code admin-punctual-assignment}).
 *
 * <p>Asignacion puntual del {@code ADMIN}: crea una {@code Request} para el empleado y la
 * fecha indicados que nace directamente {@code APPROVED}, sin pasar por {@code PENDING}
 * (design §D1). {@code employeeId} y {@code requestedDate} son obligatorios;
 * {@code resourceType} es opcional (por defecto {@code PARKING}); {@code resourceId} es
 * opcional solo para {@code PARKING} (auto-asignacion por categoria/planta si se omite). Para
 * {@code DESK} el puesto es obligatorio: la asignacion puntual no auto-asigna puestos (no hay
 * un criterio de negocio documentado para elegir uno, a diferencia de las plazas).</p>
 *
 * @param employeeId    empleado al que se asigna el recurso (obligatorio)
 * @param requestedDate fecha concreta de la asignacion (obligatoria)
 * @param resourceType  tipo de recurso; {@code null} = {@code PARKING} por defecto
 * @param resourceId    recurso elegido; opcional en {@code PARKING} (auto-asignacion),
 *                      obligatorio en {@code DESK}
 */
@Schema(description = "Peticion de asignacion puntual del admin: empleado, fecha y recurso opcional")
public record RequestAdminAssignRequest(
        @Schema(description = "Empleado al que se asigna el recurso", example = "15")
        @JsonProperty("employeeId")
        @NotNull(message = "El empleado es obligatorio")
        Long employeeId,

        @Schema(description = "Fecha concreta de la asignacion (ISO-8601)", example = "2026-07-10")
        @JsonProperty("requestedDate")
        @NotNull(message = "La fecha solicitada es obligatoria")
        LocalDate requestedDate,

        @Schema(description = "Tipo de recurso; por defecto PARKING", example = "PARKING",
                defaultValue = "PARKING")
        @JsonProperty("resourceType")
        ResourceType resourceType,

        @Schema(description = "Recurso a asignar; opcional en PARKING (auto-asignacion si se "
                + "omite), obligatorio en DESK", example = "8")
        @JsonProperty("resourceId")
        Long resourceId) {

    /**
     * @return el {@code resourceType} indicado, o {@code PARKING} si se omite
     */
    public ResourceType resourceTypeOrDefault() {
        return resourceType == null ? ResourceType.PARKING : resourceType;
    }
}
