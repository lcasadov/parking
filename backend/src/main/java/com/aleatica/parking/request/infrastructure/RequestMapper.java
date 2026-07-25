package com.aleatica.parking.request.infrastructure;

import com.aleatica.parking.request.domain.Request;

/**
 * Mapper a mano entidad&harr;dominio del agregado {@code request} (design §D2: sin MapStruct).
 *
 * <p>Traduce entre la entidad de persistencia {@link RequestEntity} y el modelo de dominio
 * {@link Request} en ambos sentidos, con metodos estaticos en linea con el estilo del proyecto
 * ({@code CurrentUser.from(...)}). El mapeo es plano (todos los campos son escalares o
 * identificadores {@code Long}), por lo que no justifica un procesador de anotaciones.</p>
 */
public final class RequestMapper {

    private RequestMapper() {
        // utilidad
    }

    /**
     * Mapea la entidad de persistencia a su modelo de dominio.
     *
     * @param entity entidad origen
     * @return el modelo de dominio equivalente
     */
    public static Request toDomain(RequestEntity entity) {
        return Request.restore(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getRequestedDate(),
                entity.getStatus(),
                entity.getResourceId(),
                entity.getResourceType(),
                entity.getApprovalNote(),
                entity.getRejectionReasonCode(),
                entity.getRejectionReason(),
                entity.getResolvedById(),
                entity.getResolvedAt(),
                entity.getCreatedAt(),
                entity.getLastRemindedAt(),
                entity.isWaitlisted());
    }

    /**
     * Mapea el modelo de dominio a la entidad de persistencia. El {@code id} viaja tal cual:
     * {@code null} produce un alta (insert), presente produce una actualizacion (merge).
     *
     * @param request modelo de dominio origen
     * @return la entidad de persistencia equivalente
     */
    public static RequestEntity toEntity(Request request) {
        return new RequestEntity(
                request.getId(),
                request.getEmployeeId(),
                request.getRequestedDate(),
                request.getStatus(),
                request.getResourceId(),
                request.getResourceType(),
                request.getApprovalNote(),
                request.getRejectionReasonCode(),
                request.getRejectionReason(),
                request.getResolvedById(),
                request.getResolvedAt(),
                request.getCreatedAt(),
                request.getLastRemindedAt(),
                request.isWaitlisted());
    }
}
