package com.aleatica.parking.visitor.dto;
import com.aleatica.parking.resource.ResourceType;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Fija las claves JSON contractuales de los schemas {@code Visitor},
 * {@code VisitorCreateRequest}, {@code VisitorReservation} y
 * {@code VisitorReservationCreateRequest} (autoridad {@code docs/openapi.yaml}). Mismo
 * blindaje que el bug #21 con {@code isCorporate}:
 * {@link com.fasterxml.jackson.annotation.JsonProperty} evita que Jackson derive otra
 * clave del componente record.
 */
class VisitorJsonTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void shouldSerializeContractKeys_whenSerializingVisitor() throws Exception {
        // Arrange
        VisitorResponse response = new VisitorResponse(
                42L, "Ada", "Lovelace", "X1234567Z", "1234ABC", "Contoso", "Reunion", 1L,
                Instant.parse("2026-07-04T10:00:00Z"));

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert
        assertThat(json)
                .contains("\"id\":42")
                .contains("\"firstName\":\"Ada\"")
                .contains("\"lastName\":\"Lovelace\"")
                .contains("\"nationalId\":\"X1234567Z\"")
                .contains("\"licensePlate\":\"1234ABC\"")
                .contains("\"company\":\"Contoso\"")
                .contains("\"usualReason\":\"Reunion\"")
                .contains("\"createdById\":1")
                .contains("\"createdAt\":");
    }

    @Test
    void shouldSerializeContractKeys_whenSerializingReservation() throws Exception {
        // Arrange
        VisitorReservationResponse response = new VisitorReservationResponse(
                 7L, 42L, ResourceType.PARKING, 8L, LocalDate.of(2026, 7, 10), "Puerta norte", 1L,
                Instant.parse("2026-07-04T10:00:00Z"));

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert
        assertThat(json)
                .contains("\"id\":7")
                .contains("\"visitorId\":42")
                .contains("\"resourceType\":\"PARKING\",\"resourceId\":8")
                .contains("\"reservationDate\":\"2026-07-10\"")
                .contains("\"notes\":\"Puerta norte\"")
                .contains("\"createdById\":1")
                .contains("\"createdAt\":");
    }

    @Test
    void shouldDeserializeCreateBody_whenReadingVisitorRequest() throws Exception {
        // Arrange
        String body = "{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"nationalId\":\"X1234567Z\","
                + "\"licensePlate\":\"1234ABC\",\"company\":\"Contoso\",\"usualReason\":\"Reunion\"}";

        // Act
        VisitorCreateRequest parsed = objectMapper.readValue(body, VisitorCreateRequest.class);

        // Assert
        assertThat(parsed.firstName()).isEqualTo("Ada");
        assertThat(parsed.lastName()).isEqualTo("Lovelace");
        assertThat(parsed.nationalId()).isEqualTo("X1234567Z");
        assertThat(parsed.licensePlate()).isEqualTo("1234ABC");
        assertThat(parsed.company()).isEqualTo("Contoso");
        assertThat(parsed.usualReason()).isEqualTo("Reunion");
    }

    @Test
    void shouldDeserializeCreateBody_whenReadingReservationRequest() throws Exception {
        // Arrange
        String body = "{\"visitorId\":42,\"resourceType\":\"PARKING\",\"resourceId\":8,\"reservationDate\":\"2026-07-10\","
                + "\"notes\":\"Puerta norte\"}";

        // Act
        VisitorReservationCreateRequest parsed =
                objectMapper.readValue(body, VisitorReservationCreateRequest.class);

        // Assert
        assertThat(parsed.visitorId()).isEqualTo(42L);
        assertThat(parsed.resourceType()).isEqualTo(ResourceType.PARKING);
        assertThat(parsed.resourceId()).isEqualTo(8L);
        assertThat(parsed.reservationDate()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(parsed.notes()).isEqualTo("Puerta norte");
    }
}
