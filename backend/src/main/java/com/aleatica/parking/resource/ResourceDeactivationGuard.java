package com.aleatica.parking.resource;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.exception.ResourceDeactivationBlockedException;
import com.aleatica.parking.fixedassignment.infrastructure.FixedAssignmentJpaRepository;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.infrastructure.RequestJpaRepository;
import com.aleatica.parking.visitor.VisitorReservationRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guarda que impide desactivar un recurso (plaza o puesto) con asignaciones vigentes o
 * futuras, para no dejar sin sitio a un empleado o visitante de forma silenciosa
 * (change {@code admin-improvements}, tarea 16).
 *
 * <p>Un recurso NO puede desactivarse si tiene: (a) alguna asignacion fija activa (ocupacion
 * recurrente futura), (b) alguna solicitud {@code APPROVED} para hoy o una fecha futura, o
 * (c) alguna reserva de visitante para hoy o una fecha futura. En ese caso lanza
 * {@link ResourceDeactivationBlockedException} con el desglose por tipo, que el manejador
 * global traduce a {@code 409} para que el frontend avise indicando a quien/que afecta.</p>
 */
@Service
public class ResourceDeactivationGuard {

    private final FixedAssignmentJpaRepository fixedAssignmentRepository;
    private final RequestJpaRepository requestRepository;
    private final VisitorReservationRepository visitorReservationRepository;
    private final ClockPort clock;

    /**
     * @param fixedAssignmentRepository    repositorio de asignaciones fijas
     * @param requestRepository            repositorio de solicitudes
     * @param visitorReservationRepository repositorio de reservas de visitante
     * @param clock                        reloj inyectable (fecha de hoy)
     */
    public ResourceDeactivationGuard(
            FixedAssignmentJpaRepository fixedAssignmentRepository,
            RequestJpaRepository requestRepository,
            VisitorReservationRepository visitorReservationRepository,
            ClockPort clock) {
        this.fixedAssignmentRepository = fixedAssignmentRepository;
        this.requestRepository = requestRepository;
        this.visitorReservationRepository = visitorReservationRepository;
        this.clock = clock;
    }

    /**
     * Verifica que el recurso puede desactivarse; si tiene asignaciones vigentes o futuras,
     * lanza {@link ResourceDeactivationBlockedException} con el desglose.
     *
     * @param resourceId   identificador del recurso dentro de su tabla
     * @param resourceType tipo de recurso ({@code PARKING}/{@code DESK})
     * @throws ResourceDeactivationBlockedException si el recurso tiene asignaciones vigentes o futuras
     */
    @Transactional(readOnly = true)
    public void assertCanDeactivate(Long resourceId, ResourceType resourceType) {
        LocalDate today = LocalDate.ofInstant(clock.now(), ZoneOffset.UTC);
        long fixed = fixedAssignmentRepository
                .countByResourceIdAndResourceTypeAndActiveTrue(resourceId, resourceType);
        long approved = requestRepository
                .countByResourceIdAndResourceTypeAndStatusAndRequestedDateGreaterThanEqual(
                        resourceId, resourceType, RequestStatus.APPROVED, today);
        long visitor = visitorReservationRepository
                .countByResourceTypeAndResourceIdAndReservationDateGreaterThanEqual(
                        resourceType, resourceId, today);
        if (fixed + approved + visitor > 0) {
            throw new ResourceDeactivationBlockedException(fixed, approved, visitor);
        }
    }
}
