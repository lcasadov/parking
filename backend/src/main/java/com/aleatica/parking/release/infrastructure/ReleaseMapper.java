package com.aleatica.parking.release.infrastructure;

import com.aleatica.parking.release.domain.Release;

/**
 * Mapper a mano entidad&harr;dominio del agregado {@code release} (design §D2: sin MapStruct).
 *
 * <p>Traduce entre la entidad de persistencia {@link ReleaseEntity} y el modelo de dominio
 * {@link Release} en ambos sentidos, con metodos estaticos en linea con el estilo del proyecto
 * ({@code CurrentUser.from(...)}). El mapeo es plano (todos los campos son escalares o
 * identificadores {@code Long}), por lo que no justifica un procesador de anotaciones.</p>
 */
public final class ReleaseMapper {

    private ReleaseMapper() {
        // utilidad
    }

    /**
     * Mapea la entidad de persistencia a su modelo de dominio.
     *
     * @param entity entidad origen
     * @return el modelo de dominio equivalente
     */
    public static Release toDomain(ReleaseEntity entity) {
        return Release.restore(
                entity.getId(),
                entity.getResourceId(),
                entity.getResourceType(),
                entity.getEmployeeId(),
                entity.getReleaseDate(),
                entity.getType(),
                entity.getReason(),
                entity.getReleasedById(),
                entity.getCreatedAt());
    }

    /**
     * Mapea el modelo de dominio a la entidad de persistencia. El {@code id} viaja tal cual:
     * {@code null} produce un alta (insert), presente produce una actualizacion (merge).
     *
     * @param release modelo de dominio origen
     * @return la entidad de persistencia equivalente
     */
    public static ReleaseEntity toEntity(Release release) {
        return new ReleaseEntity(
                release.getId(),
                release.getResourceId(),
                release.getResourceType(),
                release.getEmployeeId(),
                release.getReleaseDate(),
                release.getType(),
                release.getReason(),
                release.getReleasedById(),
                release.getCreatedAt());
    }
}
