package com.aleatica.parking.request.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.aleatica.parking.resource.ResourceType;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de la maquina de estados de dominio {@link Request}, en particular la
 * ampliacion del rechazo (change {@code request-auto-assignment} §D5): el rechazo se admite
 * desde {@code PENDING} y desde {@code APPROVED}, pero no desde los estados terminales
 * {@code REJECTED}/{@code CANCELLED}.
 */
class RequestTest {

    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 4);
    private static final LocalDate DATE = LocalDate.of(2026, 7, 10);
    private static final Long EMP_ID = 15L;
    private static final Long ADMIN_ID = 1L;
    private static final Long SPACE_ID = 8L;

    @Test
    void shouldAllowReject_whenPending() {
        // Arrange
        Request request = Request.create(EMP_ID, DATE, NOW);

        // Act
        request.reject(RejectionReasonCode.NO_AVAILABILITY, null, ADMIN_ID, NOW);

        // Assert
        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
    }

    @Test
    void shouldAllowReject_whenApproved() {
        // Arrange: solicitud aprobada (p. ej. auto-aprobada)
        Request request = Request.create(EMP_ID, DATE, NOW);
        request.approve(SPACE_ID, ADMIN_ID, "auto", NOW);

        // Act
        assertThat(request.canBeRejected()).isTrue();
        request.reject(RejectionReasonCode.OUTSIDE_POLICY, null, ADMIN_ID, NOW);

        // Assert: APPROVED -> REJECTED permitido
        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
    }

    @Test
    void shouldRejectTransition_whenAlreadyRejected() {
        // Arrange: estado terminal REJECTED
        Request request = Request.restore(
                1L, EMP_ID, DATE, RequestStatus.REJECTED, null, ResourceType.PARKING,
                null, RejectionReasonCode.OTHER, "motivo previo", ADMIN_ID, NOW, NOW);

        // Act / Assert
        assertThat(request.canBeRejected()).isFalse();
        assertThatThrownBy(() ->
                request.reject(RejectionReasonCode.OTHER, "otro motivo", ADMIN_ID, NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRejectTransition_whenAlreadyCancelled() {
        // Arrange: estado terminal CANCELLED
        Request request = Request.create(EMP_ID, DATE, NOW);
        request.cancel();

        // Act / Assert
        assertThat(request.canBeRejected()).isFalse();
        assertThatThrownBy(() ->
                request.reject(RejectionReasonCode.OTHER, "motivo", ADMIN_ID, NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldReportApprovedState_afterApprove() {
        // Arrange
        Request request = Request.create(EMP_ID, DATE, NOW);

        // Act
        request.approve(SPACE_ID, ADMIN_ID, null, NOW);

        // Assert
        assertThatCode(() -> assertThat(request.isApproved()).isTrue()).doesNotThrowAnyException();
    }

    // ---- Cancelacion: canBeCancelledBy (change cancel-approved-request) ----

    @Test
    void shouldAllowCancel_whenPendingRegardlessOfDate() {
        // Arrange: PENDING con fecha ya pasada
        Request request = Request.restore(
                1L, EMP_ID, TODAY.minusDays(3), RequestStatus.PENDING, null, ResourceType.PARKING,
                null, null, null, null, null, NOW);

        // Act / Assert: PENDING siempre cancelable (no ocupa recurso)
        assertThat(request.canBeCancelledBy(TODAY)).isTrue();
    }

    @Test
    void shouldAllowCancel_whenApprovedWithFutureDate() {
        // Arrange: APPROVED con fecha futura
        Request request = approved(TODAY.plusDays(2));

        // Act / Assert
        assertThat(request.canBeCancelledBy(TODAY)).isTrue();
    }

    @Test
    void shouldDenyCancel_whenApprovedWithPastDate() {
        // Arrange: APPROVED con fecha pasada (no se libera un recurso ya transcurrido)
        Request request = approved(TODAY.minusDays(1));

        // Act / Assert
        assertThat(request.canBeCancelledBy(TODAY)).isFalse();
    }

    @Test
    void shouldDenyCancel_whenRejected() {
        // Arrange: estado terminal REJECTED
        Request request = Request.restore(
                1L, EMP_ID, TODAY.plusDays(2), RequestStatus.REJECTED, SPACE_ID, ResourceType.PARKING,
                null, RejectionReasonCode.OTHER, "motivo", ADMIN_ID, NOW, NOW);

        // Act / Assert
        assertThat(request.canBeCancelledBy(TODAY)).isFalse();
    }

    @Test
    void shouldDenyCancel_whenAlreadyCancelled() {
        // Arrange: estado terminal CANCELLED
        Request request = Request.create(EMP_ID, TODAY.plusDays(2), NOW);
        request.cancel();

        // Act / Assert
        assertThat(request.canBeCancelledBy(TODAY)).isFalse();
    }

    @Test
    void shouldDenyCancel_whenApprovedDateIsYesterday() {
        // Arrange (borde inferior): ayer no es cancelable
        assertThat(approved(TODAY.minusDays(1)).canBeCancelledBy(TODAY)).isFalse();
    }

    @Test
    void shouldAllowCancel_whenApprovedDateIsToday() {
        // Arrange (borde: hoy inclusive es cancelable)
        assertThat(approved(TODAY).canBeCancelledBy(TODAY)).isTrue();
    }

    @Test
    void shouldAllowCancel_whenApprovedDateIsTomorrow() {
        // Arrange (borde superior): mañana es cancelable
        assertThat(approved(TODAY.plusDays(1)).canBeCancelledBy(TODAY)).isTrue();
    }

    @Test
    void shouldTransitionToCancelled_whenCancellingApproved() {
        // Arrange: APPROVED conserva el recurso al cancelar (traza del recurso liberado)
        Request request = approved(TODAY.plusDays(2));

        // Act
        request.cancel();

        // Assert: CANCELLED y resourceId conservado
        assertThat(request.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        assertThat(request.getResourceId()).isEqualTo(SPACE_ID);
    }

    @Test
    void shouldRejectCancelTransition_whenTerminal() {
        // Arrange: estado terminal CANCELLED
        Request request = Request.create(EMP_ID, DATE, NOW);
        request.cancel();

        // Act / Assert: segunda cancelacion no permitida
        assertThatThrownBy(request::cancel).isInstanceOf(IllegalStateException.class);
    }

    private static Request approved(LocalDate date) {
        Request request = Request.create(EMP_ID, date, NOW);
        request.approve(SPACE_ID, ADMIN_ID, "auto", NOW);
        return request;
    }
}
