package com.aleatica.parking.parkingspace.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Fija la clave JSON contractual del flag booleano de plaza (autoridad
 * {@code docs/openapi.yaml}: {@code active}). Mismo blindaje que el bug #21 con
 * {@code isCorporate}: {@link com.fasterxml.jackson.annotation.JsonProperty} evita
 * que Jackson derive otra clave del componente record, en serializacion (respuesta)
 * y en binding (peticion de alta/edicion).
 */
class ParkingSpaceActiveJsonTest {

    private static final String CONTRACT_KEY = "\"active\"";

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void shouldSerializeActiveFlagAsActive_whenSerializingResponse() throws Exception {
        // Arrange
        ParkingSpaceResponse response = new ParkingSpaceResponse(
                1L, 1007, "1007", 1, true, Instant.parse("2026-01-01T00:00:00Z"));

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert
        assertThat(json).contains(CONTRACT_KEY);
    }

    @Test
    void shouldBindActiveFlag_whenDeserializingRequestWithActive() throws Exception {
        // Arrange
        String body = "{\"number\":1007,\"active\":false}";

        // Act
        ParkingSpaceRequest request = objectMapper.readValue(body, ParkingSpaceRequest.class);

        // Assert
        assertThat(request.active()).isFalse();
        assertThat(request.activeOrDefault()).isFalse();
    }

    @Test
    void shouldDefaultActiveToTrue_whenRequestOmitsActive() throws Exception {
        // Arrange
        String body = "{\"number\":1007}";

        // Act
        ParkingSpaceRequest request = objectMapper.readValue(body, ParkingSpaceRequest.class);

        // Assert
        assertThat(request.active()).isNull();
        assertThat(request.activeOrDefault()).isTrue();
    }
}
