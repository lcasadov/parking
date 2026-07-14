package com.aleatica.parking.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.aleatica.parking.notification.EmailOutbox;
import com.aleatica.parking.notification.NotificationEventType;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.resource.ResourceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de {@link NotificationOutboxMapper}: el ciclo orden -> fila outbox -> orden
 * conserva el tipo de evento, el destinatario y la instantanea de la solicitud (JSON), de modo
 * que el reintento pueda re-renderizar con los mismos datos. Usa un {@link ObjectMapper} con
 * soporte JSR-310 equivalente al de la aplicacion.
 */
class NotificationOutboxMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");
    private static final Long RECIPIENT_ID = 15L;

    private final ObjectMapper objectMapper =
            JsonMapper.builder().addModule(new JavaTimeModule()).build();
    private final NotificationOutboxMapper mapper = new NotificationOutboxMapper(objectMapper);

    @Test
    void shouldRoundTripCommandWithRequest_whenSerializingAndDeserializing() {
        // Arrange
        RequestResponse request = new RequestResponse(
                42L, RECIPIENT_ID, LocalDate.of(2026, 7, 10), RequestStatus.APPROVED,
                3005L, "Bienvenido", null, null, 1L, NOW, NOW, ResourceType.PARKING);
        NotificationCommand original =
                new NotificationCommand(NotificationEventType.REQUEST_APPROVED, RECIPIENT_ID, request);

        // Act
        EmailOutbox entry = mapper.toPendingOutbox(original, "smtp down", NOW);
        NotificationCommand restored = mapper.toCommand(entry);

        // Assert: la fila guarda el evento (no HTML) y el ciclo conserva todos los datos
        assertThat(entry.getEventType()).isEqualTo(NotificationEventType.REQUEST_APPROVED);
        assertThat(entry.getRecipientEmployeeId()).isEqualTo(RECIPIENT_ID);
        assertThat(entry.getRequestPayload()).isNotBlank();
        assertThat(restored).isEqualTo(original);
    }

    @Test
    void shouldRoundTripCommandWithoutRequest_whenRequestIsNull() {
        // Arrange: un evento sin solicitud (revocacion de asignacion)
        NotificationCommand original =
                new NotificationCommand(NotificationEventType.ASSIGNMENT_REVOKED, RECIPIENT_ID, null);

        // Act
        EmailOutbox entry = mapper.toPendingOutbox(original, "smtp down", NOW);
        NotificationCommand restored = mapper.toCommand(entry);

        // Assert: payload nulo y orden reconstruida sin solicitud
        assertThat(entry.getRequestPayload()).isNull();
        assertThat(restored).isEqualTo(original);
    }
}
