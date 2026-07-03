package com.aleatica.parking.fixedassignment.dto;

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
 * @param parkingSpaceId plaza a asignar (obligatoria)
 * @param daysOfWeek     dias de la semana (1-7), lista no vacia
 */
@Schema(description = "Peticion de asignacion fija: plaza + dias de la semana")
public record FixedAssignmentPutRequest(
        @Schema(description = "Plaza a asignar", example = "8")
        @JsonProperty("parkingSpaceId")
        @NotNull(message = "La plaza es obligatoria")
        Long parkingSpaceId,

        @Schema(description = "Dias de la semana (1=Lunes … 7=Domingo)", example = "[1, 2, 3]")
        @JsonProperty("daysOfWeek")
        @NotEmpty(message = "Debe indicar al menos un dia de la semana")
        List<Integer> daysOfWeek) {
}
