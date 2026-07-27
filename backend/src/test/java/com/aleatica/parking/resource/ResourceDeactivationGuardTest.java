package com.aleatica.parking.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.exception.ResourceDeactivationBlockedException;
import com.aleatica.parking.fixedassignment.infrastructure.FixedAssignmentJpaRepository;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.infrastructure.RequestJpaRepository;
import com.aleatica.parking.visitor.VisitorReservationRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios de {@link ResourceDeactivationGuard} con los repositorios mockeados: no
 * bloquea si el recurso no tiene asignaciones vigentes/futuras, y lanza
 * {@link ResourceDeactivationBlockedException} con el desglose cuando las tiene. Reloj fijo.
 */
@ExtendWith(MockitoExtension.class)
class ResourceDeactivationGuardTest {

    private static final Long RESOURCE_ID = 7L;
    private static final Instant NOW = Instant.parse("2026-07-27T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.parse("2026-07-27");

    @Mock
    private FixedAssignmentJpaRepository fixedAssignmentRepository;

    @Mock
    private RequestJpaRepository requestRepository;

    @Mock
    private VisitorReservationRepository visitorReservationRepository;

    private final ClockPort clock = () -> NOW;

    private ResourceDeactivationGuard newGuard() {
        return new ResourceDeactivationGuard(
                fixedAssignmentRepository, requestRepository, visitorReservationRepository, clock);
    }

    @Test
    void shouldNotBlock_whenNoFutureAssignments() {
        given(fixedAssignmentRepository
                .countByResourceIdAndResourceTypeAndActiveTrue(RESOURCE_ID, ResourceType.PARKING))
                .willReturn(0L);
        given(requestRepository
                .countByResourceIdAndResourceTypeAndStatusAndRequestedDateGreaterThanEqual(
                        RESOURCE_ID, ResourceType.PARKING, RequestStatus.APPROVED, TODAY))
                .willReturn(0L);
        given(visitorReservationRepository
                .countByResourceTypeAndResourceIdAndReservationDateGreaterThanEqual(
                        ResourceType.PARKING, RESOURCE_ID, TODAY))
                .willReturn(0L);

        assertThatCode(() -> newGuard().assertCanDeactivate(RESOURCE_ID, ResourceType.PARKING))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldBlockWithBreakdown_whenFutureAssignmentsExist() {
        given(fixedAssignmentRepository
                .countByResourceIdAndResourceTypeAndActiveTrue(RESOURCE_ID, ResourceType.DESK))
                .willReturn(2L);
        given(requestRepository
                .countByResourceIdAndResourceTypeAndStatusAndRequestedDateGreaterThanEqual(
                        RESOURCE_ID, ResourceType.DESK, RequestStatus.APPROVED, TODAY))
                .willReturn(1L);
        given(visitorReservationRepository
                .countByResourceTypeAndResourceIdAndReservationDateGreaterThanEqual(
                        ResourceType.DESK, RESOURCE_ID, TODAY))
                .willReturn(0L);

        assertThatThrownBy(() -> newGuard().assertCanDeactivate(RESOURCE_ID, ResourceType.DESK))
                .isInstanceOf(ResourceDeactivationBlockedException.class)
                .satisfies(ex -> {
                    ResourceDeactivationBlockedException blocked =
                            (ResourceDeactivationBlockedException) ex;
                    assertThat(blocked.getFixedAssignments()).isEqualTo(2L);
                    assertThat(blocked.getApprovedRequests()).isEqualTo(1L);
                    assertThat(blocked.getVisitorReservations()).isZero();
                });
    }
}
