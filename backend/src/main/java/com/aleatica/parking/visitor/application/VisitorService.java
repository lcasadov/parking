package com.aleatica.parking.visitor.application;

import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.dto.PageResponse;
import com.aleatica.parking.visitor.Visitor;
import com.aleatica.parking.visitor.VisitorRepository;
import com.aleatica.parking.visitor.dto.VisitorCreateRequest;
import com.aleatica.parking.visitor.dto.VisitorResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de fichas de visitante: creacion (con unicidad de {@code nationalId}),
 * edicion, detalle y listado paginado con busqueda libre. Reservado al {@code ADMIN}.
 *
 * <p>Arquitectura hexagonal: la logica de negocio no depende de la web y nunca devuelve
 * entidades JPA (convierte a DTO antes de salir, OWASP API3). La unicidad de
 * {@code nationalId} la garantiza el indice unico de la BD; el servicio hace una
 * comprobacion previa (mensaje claro) pero deja que la violacion de indice se traduzca a
 * 409 bajo concurrencia. Editar la ficha no reescribe reservas existentes: la reserva
 * referencia al visitante por id y no denormaliza sus campos, por lo que el cambio afecta
 * solo a futuras reservas. La auditoria se dispara {@code AFTER_COMMIT} via eventos
 * ({@link VisitorAuditEvent}).</p>
 */
@Service
public class VisitorService {

    private static final String MSG_ACTOR_NOT_FOUND = "Usuario de sesion no encontrado: ";
    private static final String MSG_VISITOR_NOT_FOUND = "Visitante no encontrado: ";
    private static final String MSG_NATIONAL_ID_TAKEN =
            "Ya existe un visitante con ese documento de identidad";

    private final VisitorRepository visitorRepository;
    private final EmployeeRepository employeeRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @param visitorRepository  repositorio de fichas de visitante
     * @param employeeRepository repositorio de empleados (resolucion del ADMIN creador)
     * @param eventPublisher     publicador de eventos de auditoria
     */
    public VisitorService(
            VisitorRepository visitorRepository,
            EmployeeRepository employeeRepository,
            ApplicationEventPublisher eventPublisher) {
        this.visitorRepository = visitorRepository;
        this.employeeRepository = employeeRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Crea una ficha de visitante atribuida al {@code ADMIN} de la sesion, garantizando
     * la unicidad de {@code nationalId}.
     *
     * @param adminLogin login del {@code ADMIN} creador (principal de la sesion)
     * @param request    datos de la ficha
     * @return la ficha creada (DTO)
     * @throws DuplicateNationalIdException si el {@code nationalId} ya existe
     */
    @Transactional
    public VisitorResponse create(String adminLogin, VisitorCreateRequest request) {
        Long adminId = resolveEmployeeId(adminLogin);
        if (visitorRepository.existsByNationalId(request.nationalId())) {
            throw new DuplicateNationalIdException(MSG_NATIONAL_ID_TAKEN);
        }
        Visitor saved = visitorRepository.saveAndFlush(Visitor.create(
                request.firstName(), request.lastName(), request.nationalId(),
                request.licensePlate(), request.company(), request.usualReason(), adminId));
        VisitorResponse response = VisitorResponse.from(saved);
        eventPublisher.publishEvent(new VisitorAuditEvent(VisitorAuditEvent.Kind.CREATED, response));
        return response;
    }

    /**
     * Edita una ficha de visitante existente, garantizando la unicidad de
     * {@code nationalId} frente a otras fichas. El cambio no reescribe reservas ya
     * creadas (afecta solo a futuras reservas).
     *
     * @param id      identificador de la ficha
     * @param request nuevos datos de la ficha
     * @return la ficha actualizada (DTO)
     * @throws EntityNotFoundException      si la ficha no existe
     * @throws DuplicateNationalIdException si el {@code nationalId} lo usa otra ficha
     */
    @Transactional
    public VisitorResponse update(Long id, VisitorCreateRequest request) {
        Visitor visitor = loadVisitor(id);
        if (visitorRepository.existsByNationalIdAndIdNot(request.nationalId(), id)) {
            throw new DuplicateNationalIdException(MSG_NATIONAL_ID_TAKEN);
        }
        visitor.update(
                request.firstName(), request.lastName(), request.nationalId(),
                request.licensePlate(), request.company(), request.usualReason());
        VisitorResponse response = VisitorResponse.from(visitorRepository.saveAndFlush(visitor));
        eventPublisher.publishEvent(new VisitorAuditEvent(VisitorAuditEvent.Kind.UPDATED, response));
        return response;
    }

    /**
     * Devuelve el detalle de una ficha por su id.
     *
     * @param id identificador de la ficha
     * @return la ficha (DTO)
     * @throws EntityNotFoundException si no existe
     */
    @Transactional(readOnly = true)
    public VisitorResponse get(Long id) {
        return VisitorResponse.from(loadVisitor(id));
    }

    /**
     * Lista de forma paginada las fichas de visitante con busqueda libre opcional por
     * {@code nationalId}, nombre, apellidos o matricula.
     *
     * @param q        texto de busqueda libre; {@code null}/vacio para no filtrar
     * @param pageable pagina y tamano solicitados
     * @return pagina de fichas (DTO)
     */
    @Transactional(readOnly = true)
    public PageResponse<VisitorResponse> list(String q, Pageable pageable) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return PageResponse.from(visitorRepository.search(query, pageable), VisitorResponse::from);
    }

    private Visitor loadVisitor(Long id) {
        return visitorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(MSG_VISITOR_NOT_FOUND + id));
    }

    private Long resolveEmployeeId(String login) {
        return employeeRepository.findByLogin(login)
                .map(Employee::getId)
                .orElseThrow(() -> new EntityNotFoundException(MSG_ACTOR_NOT_FOUND + login));
    }
}
