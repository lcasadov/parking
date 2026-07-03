package com.aleatica.parking.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Envoltorio de paginacion estable de la API (schema {@code PageMeta} + contenido).
 *
 * <p>Se usa en lugar de serializar {@code org.springframework.data.domain.Page}
 * directamente, cuya forma JSON no es contractualmente estable entre versiones de
 * Spring. Los metadatos coinciden con el schema {@code PageMeta} de la API.</p>
 *
 * @param <T>           tipo del contenido de la pagina
 * @param content       elementos de la pagina actual
 * @param totalElements total de elementos en todas las paginas
 * @param totalPages    total de paginas
 * @param size          tamano de pagina solicitado
 * @param number        indice de la pagina actual (base 0)
 * @param first         si es la primera pagina
 * @param last          si es la ultima pagina
 */
@Schema(description = "Pagina de resultados con metadatos")
public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int size,
        int number,
        boolean first,
        boolean last) {

    /**
     * Construye un {@link PageResponse} a partir de una {@link Page} de entidades,
     * mapeando cada elemento con la funcion dada.
     *
     * @param page   pagina de dominio origen
     * @param mapper funcion de mapeo entidad -&gt; DTO
     * @param <E>    tipo de la entidad origen
     * @param <T>    tipo del DTO destino
     * @return la pagina de DTOs con sus metadatos
     */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize(),
                page.getNumber(),
                page.isFirst(),
                page.isLast());
    }
}
