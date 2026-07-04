package com.aleatica.parking.notification.application;

import static org.mockito.Mockito.verify;

import com.aleatica.parking.notification.event.FixedAssignmentRevokedEvent;
import com.aleatica.parking.notification.event.RequestApprovedEvent;
import com.aleatica.parking.notification.event.RequestCreatedEvent;
import com.aleatica.parking.notification.event.RequestRejectedEvent;
import com.aleatica.parking.request.RequestStatus;
import com.aleatica.parking.request.dto.RequestResponse;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios de {@link EmailNotificationListener}: cada tipo de evento se enruta al
 * caso de uso correcto del {@link NotificationDispatcher}. Con el dispatcher mockeado.
 */
@ExtendWith(MockitoExtension.class)
class EmailNotificationListenerTest {

    private static final Long EMP_ID = 15L;
    private static final RequestResponse REQUEST = new RequestResponse(
            42L, EMP_ID, LocalDate.of(2026, 7, 10), RequestStatus.PENDING,
            null, null, null, null, null, null, Instant.parse("2026-07-04T10:00:00Z"), com.aleatica.parking.resource.ResourceType.PARKING);

    @Mock
    private NotificationDispatcher dispatcher;

    private EmailNotificationListener listener() {
        return new EmailNotificationListener(dispatcher);
    }

    @Test
    void shouldRouteToRequestCreated_whenRequestCreatedEvent() {
        listener().onRequestCreated(new RequestCreatedEvent(REQUEST));
        verify(dispatcher).requestCreated(REQUEST);
    }

    @Test
    void shouldRouteToRequestApproved_whenRequestApprovedEvent() {
        listener().onRequestApproved(new RequestApprovedEvent(REQUEST));
        verify(dispatcher).requestApproved(REQUEST);
    }

    @Test
    void shouldRouteToRequestRejected_whenRequestRejectedEvent() {
        listener().onRequestRejected(new RequestRejectedEvent(REQUEST));
        verify(dispatcher).requestRejected(REQUEST);
    }

    @Test
    void shouldRouteToAssignmentRevoked_whenFixedAssignmentRevokedEvent() {
        listener().onFixedAssignmentRevoked(new FixedAssignmentRevokedEvent(EMP_ID));
        verify(dispatcher).assignmentRevoked(EMP_ID);
    }
}
