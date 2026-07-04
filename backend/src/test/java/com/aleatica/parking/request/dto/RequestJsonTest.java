package com.aleatica.parking.request.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.aleatica.parking.request.RejectionReasonCode;
import com.aleatica.parking.request.RequestStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Fija las claves JSON contractuales de los schemas {@code Request},
 * {@code RequestRejectRequest} y {@code RequestApproveRequest} (autoridad
 * {@code docs/openapi.yaml}). Mismo blindaje que el bug #21 con {@code isCorporate}:
 * {@link com.fasterxml.jackson.annotation.JsonProperty} evita que Jackson derive otra
 * clave del componente record. En especial la clave de entrada del rechazo es
 * {@code reasonCode} (no {@code rejectionReasonCode}, que es la clave de salida).
 */
class RequestJsonTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void shouldSerializeContractKeys_whenSerializingResponse() throws Exception {
        // Arrange
        RequestResponse response = new RequestResponse(
                42L, 15L, LocalDate.of(2026, 7, 10), RequestStatus.PENDING,
                null, null, null, null, null, null, Instant.parse("2026-07-04T10:00:00Z"));

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert: claves contractuales exactas y enum por nombre
        assertThat(json)
                .contains("\"id\":42")
                .contains("\"employeeId\":15")
                .contains("\"requestedDate\":\"2026-07-10\"")
                .contains("\"status\":\"PENDING\"")
                .contains("\"parkingSpaceId\":null")
                .contains("\"createdAt\":");
    }

    @Test
    void shouldSerializeResolvedKeys_whenRequestApproved() throws Exception {
        // Arrange
        RequestResponse response = new RequestResponse(
                42L, 15L, LocalDate.of(2026, 7, 10), RequestStatus.APPROVED,
                8L, "Bienvenido", null, null, 1L, Instant.parse("2026-07-04T11:00:00Z"),
                Instant.parse("2026-07-04T10:00:00Z"));

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert
        assertThat(json)
                .contains("\"status\":\"APPROVED\"")
                .contains("\"parkingSpaceId\":8")
                .contains("\"approvalNote\":\"Bienvenido\"")
                .contains("\"resolvedById\":1")
                .contains("\"resolvedAt\":");
    }

    @Test
    void shouldSerializeRejectionKeys_whenRequestRejected() throws Exception {
        // Arrange
        RequestResponse response = new RequestResponse(
                42L, 15L, LocalDate.of(2026, 7, 10), RequestStatus.REJECTED,
                null, null, RejectionReasonCode.OTHER, "Motivo detallado", 1L,
                Instant.parse("2026-07-04T11:00:00Z"), Instant.parse("2026-07-04T10:00:00Z"));

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert
        assertThat(json)
                .contains("\"status\":\"REJECTED\"")
                .contains("\"rejectionReasonCode\":\"OTHER\"")
                .contains("\"rejectionReason\":\"Motivo detallado\"");
    }

    @Test
    void shouldDeserializeReasonCodeKey_whenReadingRejectBody() throws Exception {
        // Arrange: la clave de entrada del rechazo es reasonCode
        String body = "{\"reasonCode\":\"NO_AVAILABILITY\",\"rejectionReason\":\"sin sitio\"}";

        // Act
        RequestRejectRequest parsed = objectMapper.readValue(body, RequestRejectRequest.class);

        // Assert
        assertThat(parsed.reasonCode()).isEqualTo(RejectionReasonCode.NO_AVAILABILITY);
        assertThat(parsed.rejectionReason()).isEqualTo("sin sitio");
    }

    @Test
    void shouldDeserializeApproveBody_whenReadingApproveRequest() throws Exception {
        // Arrange
        String body = "{\"parkingSpaceId\":8,\"approvalNote\":\"ok\"}";

        // Act
        RequestApproveRequest parsed = objectMapper.readValue(body, RequestApproveRequest.class);

        // Assert
        assertThat(parsed.parkingSpaceId()).isEqualTo(8L);
        assertThat(parsed.approvalNote()).isEqualTo("ok");
    }

    @Test
    void shouldDeserializeCreateBody_whenReadingCreateRequest() throws Exception {
        // Arrange
        String body = "{\"requestedDate\":\"2026-07-10\"}";

        // Act
        RequestCreateRequest parsed = objectMapper.readValue(body, RequestCreateRequest.class);

        // Assert
        assertThat(parsed.requestedDate()).isEqualTo(LocalDate.of(2026, 7, 10));
    }
}
