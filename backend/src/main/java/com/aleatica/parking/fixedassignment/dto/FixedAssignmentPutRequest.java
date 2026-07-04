package com.aleatica.parking.fixedassignment.dto;

import com.aleatica.parking.resource.ResourceType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Cuerpo de entrada de {@code PUT setEmployeeFixedAssignments} (schema
 * {@code FixedAssignmentPutRequest} de la API).
 *
 * <p>Fija, para el empleado del path y la plaza indicada, el conjunto de dias de la
 * semana asignados. La validacion sintactica ({@code @NotNull}/{@code @NotEmpty}) es
 * la primera capa (UX); el rango 1-7 de cada dia lo verifica ademas el caso de uso
 * (frontera de seguridad, OWASP A04). Ambos fallos responden 400 sobre el campo
 * {@code daysOfWeek}.</p>
 *
 * <p>{@code resourceType} es opcional y por defecto {@code PARKING} (compatibilidad con
 * el contrato del nucleo): el campo {@code parkingSpaceId} identifica el recurso a
 * asignar (una plaza si {@code PARKING}, un puesto si {@code DESK}). Asi un empleado puede
 * tener asignacion fija de plaza y de puesto el mismo dia (recursos distintos).</p>
 *
 * @param parkingSpaceId recurso a asignar: plaza ({@code PARKING}) o puesto ({@code DESK})
 * @param daysOfWeek     dias de la semana (1-7), lista no vacia
 * @param resourceType   tipo de recurso; {@code null} = {@code PARKING} por defecto
 */
@Schema(description = "Peticion de asignacion fija: recurso + dias de la semana")
public record FixedAssignmentPutRequest(
        @Schema(description = "Recurso a asignar (plaza si PARKING, puesto si DESK)", example = "8")
        @JsonProperty("parkingSpaceId")
        @NotNull(message = "La plaza es obligatoria")
        Long parkingSpaceId,

        @Schema(description = "Dias de la semana (1=Lunes … 7=Domingo)", example = "[1, 2, 3]")
        @JsonProperty("daysOfWeek")
        @NotEmpty(message = "Debe indicar al menos un dia de la semana")
        List<Integer> daysOfWeek,

        @Schema(description = "Tipo de recurso; por defecto PARKING", example = "DESK",
                defaultValue = "PARKING")
        @JsonProperty("resourceType")
        ResourceType resourceType) {

    /**
     * @return el {@code resourceType} indicado, o {@code PARKING} si se omite
     */
    public ResourceType resourceTypeOrDefault() {
        return resourceType == null ? ResourceType.PARKING : resourceType;
    }
}
