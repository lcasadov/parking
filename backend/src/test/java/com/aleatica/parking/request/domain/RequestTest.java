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
}
