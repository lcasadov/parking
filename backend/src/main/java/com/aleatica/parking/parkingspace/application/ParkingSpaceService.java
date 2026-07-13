package com.aleatica.parking.parkingspace.application;

import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.parkingspace.ParkingSpace;
import com.aleatica.parking.parkingspace.ParkingSpaceRepository;
import com.aleatica.parking.parkingspace.dto.ParkingSpaceRequest;
import com.aleatica.parking.parkingspace.dto.ParkingSpaceResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de gestion de plazas de parking (alta, edicion, activacion/baja,
 * configuracion masiva del total y listado), reservados al rol {@code ADMIN}.
 *
 * <p>Arquitectura hexagonal: la logica de negocio no depende de la web y nunca
 * devuelve entidades JPA (convierte a DTO antes de salir, OWASP API3). La unicidad
 * de {@code label} se comprueba aqui (mensaje claro) y la garantiza el indice unico
 * {@code UX_parking_spaces_label} frente a concurrencia (design §Decisions).</p>
 */
@Service
public class ParkingSpaceService {

    private static final String FIELD_NUMBER = "number";
    private static final String MSG_NUMBER_TAKEN = "El numero ya esta en uso";
    private static final String MSG_NOT_FOUND = "Plaza no encontrada: ";
    private static final int SPACES_PER_FLOOR = 5;
    private static final int FLOOR_RANGE = 1000;
    private static final Sort BY_ID = Sort.by(Sort.Direction.ASC, "id");

    private final ParkingSpaceRepository parkingSpaceRepository;

    /**
     * @param parkingSpaceRepository repositorio de plazas
     */
    public ParkingSpaceService(ParkingSpaceRepository parkingSpaceRepository) {
        this.parkingSpaceRepository = parkingSpaceRepository;
    }

    /**
     * Lista plazas de forma paginada, con filtro opcional por estado y por planta.
     *
     * <p>La planta {@code floor} se traduce al rango de numeros
     * {@code [floor*1000, floor*1000+999]} (design §Decision 1); {@code null} no
     * filtra por planta.</p>
     *
     * @param active   filtro por estado activo; {@code null} no filtra
     * @param floor    filtro por planta; {@code null} no filtra
     * @param pageable pagina y orden solicitados
     * @return pagina de plazas (DTO) con sus metadatos
     */
    @Transactional(readOnly = true)
    public PageResponse<ParkingSpaceResponse> list(Boolean active, Integer floor, Pageable pageable) {
        Integer minNumber = floor == null ? null : floor * FLOOR_RANGE;
        Integer maxNumber = floor == null ? null : floor * FLOOR_RANGE + (FLOOR_RANGE - 1);
        Page<ParkingSpace> page = parkingSpaceRepository.search(active, minNumber, maxNumber, pageable);
        return PageResponse.from(page, ParkingSpaceResponse::from);
    }

    /**
     * Da de alta una plaza validando la unicidad del {@code number}; el
     * {@code label} y la planta se derivan del numero.
     *
     * @param request datos de alta ya validados sintacticamente
     * @return la plaza creada
     * @throws ParkingSpaceConflictException si el {@code number} ya existe
     */
    @Transactional
    public ParkingSpaceResponse create(ParkingSpaceRequest request) {
        if (parkingSpaceRepository.existsByNumber(request.number())) {
            throw new ParkingSpaceConflictException(FIELD_NUMBER, MSG_NUMBER_TAKEN);
        }
        ParkingSpace space = ParkingSpace.create(request.number());
        space.setActive(request.activeOrDefault());
        return ParkingSpaceResponse.from(parkingSpaceRepository.save(space));
    }

    /**
     * Modifica una plaza existente: cambia el {@code number} (validando que no
     * colisione con otra plaza; recalcula {@code label} y planta derivada) y el
     * estado {@code active}.
     *
     * @param id      id de la plaza a modificar
     * @param request datos de edicion ya validados sintacticamente
     * @return la plaza actualizada
     * @throws EntityNotFoundException       si la plaza no existe
     * @throws ParkingSpaceConflictException si el {@code number} lo usa otra plaza
     */
    @Transactional
    public ParkingSpaceResponse update(Long id, ParkingSpaceRequest request) {
        ParkingSpace space = findOrThrow(id);
        if (parkingSpaceRepository.existsByNumberAndIdNot(request.number(), id)) {
            throw new ParkingSpaceConflictException(FIELD_NUMBER, MSG_NUMBER_TAKEN);
        }
        space.setNumber(request.number());
        space.setActive(request.activeOrDefault());
        return ParkingSpaceResponse.from(parkingSpaceRepository.save(space));
    }

    /**
     * Ajusta el numero total de plazas activas del parque al {@code total} indicado,
     * preservando el historico de las plazas existentes:
     * <ul>
     *   <li>si {@code total} supera las plazas activas, crea las plazas que faltan
     *       con etiquetas libres autogeneradas;</li>
     *   <li>si {@code total} es menor, desactiva las plazas sobrantes (las de mayor
     *       id) sin borrar la fila ni su historico;</li>
     *   <li>si coincide, no hay cambios.</li>
     * </ul>
     *
     * @param total numero total de plazas activas deseado ({@code >= 0})
     * @return el listado completo de plazas resultante, ordenado por id
     */
    @Transactional
    public List<ParkingSpaceResponse> configure(int total) {
        List<ParkingSpace> activeSpaces = parkingSpaceRepository.findByActiveTrueOrderByIdAsc();
        int current = activeSpaces.size();
        if (total > current) {
            createSpaces(total - current);
        } else if (total < current) {
            deactivateSurplus(activeSpaces, current - total);
        }
        return parkingSpaceRepository.findAll(BY_ID).stream()
                .map(ParkingSpaceResponse::from)
                .toList();
    }

    private void createSpaces(int count) {
        int index = 0;
        int created = 0;
        while (created < count) {
            int number = numberForIndex(index);
            index++;
            if (!parkingSpaceRepository.existsByNumber(number)) {
                parkingSpaceRepository.save(ParkingSpace.create(number));
                created++;
            }
        }
    }

    /**
     * Numero 1000-based para la posicion {@code index} (0-based) segun el reparto
     * por planta: {@code floor = 1 + index/SPACES_PER_FLOOR};
     * {@code number = floor*1000 + index%SPACES_PER_FLOOR + 1} (design §Decision 3).
     */
    private static int numberForIndex(int index) {
        int floor = 1 + index / SPACES_PER_FLOOR;
        return floor * FLOOR_RANGE + (index % SPACES_PER_FLOOR) + 1;
    }

    private void deactivateSurplus(List<ParkingSpace> activeSpaces, int surplus) {
        int from = activeSpaces.size() - surplus;
        for (int i = from; i < activeSpaces.size(); i++) {
            ParkingSpace space = activeSpaces.get(i);
            space.setActive(false);
            parkingSpaceRepository.save(space);
        }
    }

    private ParkingSpace findOrThrow(Long id) {
        return parkingSpaceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(MSG_NOT_FOUND + id));
    }
}
