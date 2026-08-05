package com.aleatica.parking.desk.application;

import com.aleatica.parking.desk.Desk;
import com.aleatica.parking.desk.DeskRepository;
import com.aleatica.parking.desk.dto.DeskCreateRequest;
import com.aleatica.parking.desk.dto.DeskResponse;
import com.aleatica.parking.desk.dto.DeskUpdateRequest;
import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.resource.ResourceDeactivationGuard;
import com.aleatica.parking.resource.ResourceType;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de gestion de puestos de oficina (alta, edicion de categoria/coordenadas,
 * activacion/desactivacion, listado y detalle), reservados al rol {@code ADMIN}.
 *
 * <p>Arquitectura hexagonal: la logica de negocio no depende de la web y nunca devuelve
 * entidades JPA (convierte a DTO antes de salir, OWASP API3). El rango 1-65 y el rango de
 * coordenadas los valida el DTO de entrada (400); la unicidad de {@code number} se
 * comprueba aqui (mensaje claro) y la garantiza el indice unico {@code UX_desks_number}
 * frente a concurrencia (design §Decisions: unicidad en dos capas). La categoria
 * {@code EXECUTIVE} no altera ninguna regla de reserva: es una distincion visual.</p>
 */
@Service
public class DeskService {

    private static final String FIELD_NUMBER = "number";
    private static final String MSG_NUMBER_TAKEN = "El numero de puesto ya esta en uso";
    private static final String MSG_NOT_FOUND = "Puesto no encontrado: ";

    private final DeskRepository deskRepository;
    private final ResourceDeactivationGuard deactivationGuard;

    /**
     * @param deskRepository    repositorio de puestos
     * @param deactivationGuard guarda que impide desactivar un puesto con asignaciones futuras
     */
    public DeskService(DeskRepository deskRepository, ResourceDeactivationGuard deactivationGuard) {
        this.deskRepository = deskRepository;
        this.deactivationGuard = deactivationGuard;
    }

    /**
     * Lista puestos de forma paginada, con filtro opcional por estado.
     *
     * @param active   filtro por estado activo; {@code null} no filtra
     * @param pageable pagina y orden solicitados
     * @return pagina de puestos (DTO) con sus metadatos
     */
    @Transactional(readOnly = true)
    public PageResponse<DeskResponse> list(Boolean active, Pageable pageable) {
        Page<Desk> page = deskRepository.search(active, pageable);
        return PageResponse.from(page, DeskResponse::from);
    }

    /**
     * Devuelve el detalle de un puesto por su id.
     *
     * @param id identificador del puesto
     * @return el puesto (DTO)
     * @throws EntityNotFoundException si el puesto no existe
     */
    @Transactional(readOnly = true)
    public DeskResponse get(Long id) {
        return DeskResponse.from(findOrThrow(id));
    }

    /**
     * Da de alta un puesto validando la unicidad del {@code number} (el rango 1-65 lo
     * garantiza la validacion del DTO).
     *
     * @param request datos de alta ya validados sintacticamente
     * @return el puesto creado
     * @throws DeskConflictException si el {@code number} ya existe
     */
    @Transactional
    public DeskResponse create(DeskCreateRequest request) {
        if (deskRepository.existsByNumber(request.number())) {
            throw new DeskConflictException(FIELD_NUMBER, MSG_NUMBER_TAKEN);
        }
        Desk desk = Desk.create(
                request.number(), request.category(),
                request.coordXOrDefault(), request.coordYOrDefault());
        return DeskResponse.from(deskRepository.save(desk));
    }

    /**
     * Modifica la categoria y, si se indican, las coordenadas de un puesto. El
     * {@code number} es inmutable.
     *
     * @param id      id del puesto a modificar
     * @param request datos de edicion ya validados sintacticamente
     * @return el puesto actualizado
     * @throws EntityNotFoundException si el puesto no existe
     */
    @Transactional
    public DeskResponse update(Long id, DeskUpdateRequest request) {
        Desk desk = findOrThrow(id);
        desk.setCategory(request.category());
        if (request.coordX() != null) {
            desk.setCoordX(request.coordX());
        }
        if (request.coordY() != null) {
            desk.setCoordY(request.coordY());
        }
        return DeskResponse.from(deskRepository.save(desk));
    }

    /**
     * Activa o desactiva un puesto. Un puesto inactivo no aparece como disponible, sin
     * borrar la fila ni sus asignaciones (design §Decisions).
     *
     * @param id     id del puesto
     * @param active nuevo estado activo
     * @return el puesto actualizado
     * @throws EntityNotFoundException si el puesto no existe
     */
    @Transactional
    public DeskResponse setActivation(Long id, boolean active) {
        Desk desk = findOrThrow(id);
        // Al pasar de activo a inactivo, no desactivar en silencio si el puesto tiene
        // asignaciones vigentes o futuras (tarea 16): avisar con 409 y su desglose.
        if (desk.isActive() && !active) {
            deactivationGuard.assertCanDeactivate(id, ResourceType.DESK);
        }
        desk.setActive(active);
        return DeskResponse.from(deskRepository.save(desk));
    }

    /**
     * Borra un puesto por id (hard delete), como alternativa a la desactivacion. Si el
     * puesto tiene reservas/asignaciones (FK), la BD impide el borrado y la violacion de
     * integridad se mapea a 409 en el GlobalExceptionHandler (desactivar en su lugar).
     *
     * @param id id del puesto a borrar
     * @throws EntityNotFoundException si el puesto no existe
     */
    @Transactional
    public void delete(Long id) {
        Desk desk = findOrThrow(id);
        deskRepository.delete(desk);
    }

    private Desk findOrThrow(Long id) {
        return deskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(MSG_NOT_FOUND + id));
    }
}
