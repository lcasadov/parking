package com.aleatica.parking.fixedassignment.infrastructure;

import com.aleatica.parking.fixedassignment.domain.FixedAssignment;

/**
 * Mapper a mano entidad&harr;dominio del agregado {@code fixedassignment} (design §D2: sin
 * MapStruct).
 *
 * <p>Traduce entre la entidad de persistencia {@link FixedAssignmentEntity} y el modelo de
 * dominio {@link FixedAssignment} en ambos sentidos, con metodos estaticos en linea con el
 * estilo del proyecto ({@code CurrentUser.from(...)}). El mapeo es plano (todos los campos son
 * escalares o identificadores {@code Long}), por lo que no justifica un procesador de
 * anotaciones.</p>
 */
public final class FixedAssignmentMapper {

    private FixedAssignmentMapper() {
        // utilidad
    }

    /**
     * Mapea la entidad de persistencia a su modelo de dominio.
     *
     * @param entity entidad origen
     * @return el modelo de dominio equivalente
     */
    public static FixedAssignment toDomain(FixedAssignmentEntity entity) {
        return FixedAssignment.restore(
                entity.getId(),
                entity.getResourceId(),
                entity.getResourceType(),
                entity.getEmployeeId(),
                entity.getDayOfWeek(),
                entity.isActive(),
                entity.getCreatedById(),
                entity.getCreatedAt(),
                entity.getRevokedById(),
                entity.getRevokedAt());
    }

    /**
     * Mapea el modelo de dominio a la entidad de persistencia. El {@code id} viaja tal cual:
     * {@code null} produce un alta (insert), presente produce una actualizacion (merge).
     *
     * @param assignment modelo de dominio origen
     * @return la entidad de persistencia equivalente
     */
    public static FixedAssignmentEntity toEntity(FixedAssignment assignment) {
        return new FixedAssignmentEntity(
                assignment.getId(),
                assignment.getResourceId(),
                assignment.getResourceType(),
                assignment.getEmployeeId(),
                assignment.getDayOfWeek(),
                assignment.isActive(),
                assignment.getCreatedById(),
                assignment.getCreatedAt(),
                assignment.getRevokedById(),
                assignment.getRevokedAt());
    }
}
