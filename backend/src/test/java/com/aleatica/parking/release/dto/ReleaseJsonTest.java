package com.aleatica.parking.release.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.aleatica.parking.release.domain.ReleaseType;
import com.aleatica.parking.resource.ResourceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Fija las claves JSON contractuales de los schemas {@code Release},
 * {@code ReleaseCreateRequest} y {@code AdministrativeReleaseRequest} (autoridad
 * {@code docs/openapi.yaml}). Mismo blindaje que el bug #21 con {@code isCorporate}:
 * {@link com.fasterxml.jackson.annotation.JsonProperty} evita que Jackson derive otra
 * clave del componente record. En especial se verifican {@code releaseDate},
 * {@code releasedById} y {@code type}.
 */
class ReleaseJsonTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void shouldSerializeContractKeys_whenSerializingVoluntaryRelease() throws Exception {
        // Arrange
        ReleaseResponse response = new ReleaseResponse(
                42L, 8L, 15L, LocalDate.of(2026, 7, 10), ReleaseType.VOLUNTARY, null, 15L,
                Instant.parse("2026-07-04T10:00:00Z"), ResourceType.PARKING);

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert: claves contractuales exactas y enum por nombre
        assertThat(json)
                .contains("\"id\":42")
                .contains("\"parkingSpaceId\":8")
                .contains("\"employeeId\":15")
                .contains("\"releaseDate\":\"2026-07-10\"")
                .contains("\"type\":\"VOLUNTARY\"")
                .contains("\"reason\":null")
                .contains("\"releasedById\":15")
                .contains("\"createdAt\":");
    }

    @Test
    void shouldSerializeReason_whenSerializingAdministrativeRelease() throws Exception {
        // Arrange
        ReleaseResponse response = new ReleaseResponse(
                43L, 8L, 15L, LocalDate.of(2026, 7, 10), ReleaseType.ADMINISTRATIVE,
                "Ausencia justificada", 1L, Instant.parse("2026-07-04T10:00:00Z"), ResourceType.PARKING);

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert
        assertThat(json)
                .contains("\"type\":\"ADMINISTRATIVE\"")
                .contains("\"reason\":\"Ausencia justificada\"")
                .contains("\"releasedById\":1");
    }

    @Test
    void shouldDeserializeCreateBody_whenReadingCreateRequest() throws Exception {
        // Arrange
        String body = "{\"releaseDate\":\"2026-07-10\",\"parkingSpaceId\":8}";

        // Act
        ReleaseCreateRequest parsed = objectMapper.readValue(body, ReleaseCreateRequest.class);

        // Assert
        assertThat(parsed.releaseDate()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(parsed.parkingSpaceId()).isEqualTo(8L);
    }

    @Test
    void shouldDeserializeCreateBody_whenParkingSpaceOmitted() throws Exception {
        // Arrange: parkingSpaceId es opcional
        String body = "{\"releaseDate\":\"2026-07-10\"}";

        // Act
        ReleaseCreateRequest parsed = objectMapper.readValue(body, ReleaseCreateRequest.class);

        // Assert
        assertThat(parsed.releaseDate()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(parsed.parkingSpaceId()).isNull();
    }

    @Test
    void shouldDeserializeAdministrativeBody_whenReadingAdministrativeRequest() throws Exception {
        // Arrange
        String body = "{\"employeeId\":15,\"parkingSpaceId\":8,\"releaseDate\":\"2026-07-10\","
                + "\"reason\":\"Ausencia justificada\"}";

        // Act
        AdministrativeReleaseRequest parsed =
                objectMapper.readValue(body, AdministrativeReleaseRequest.class);

        // Assert
        assertThat(parsed.employeeId()).isEqualTo(15L);
        assertThat(parsed.parkingSpaceId()).isEqualTo(8L);
        assertThat(parsed.releaseDate()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(parsed.reason()).isEqualTo("Ausencia justificada");
    }
}
