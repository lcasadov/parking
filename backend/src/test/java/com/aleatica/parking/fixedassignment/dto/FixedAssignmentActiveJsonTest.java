package com.aleatica.parking.fixedassignment.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.aleatica.parking.resource.ResourceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Fija las claves JSON contractuales del schema {@code FixedAssignment} (autoridad
 * {@code docs/openapi.yaml}), en especial el flag booleano {@code active} y el entero
 * {@code dayOfWeek}. Mismo blindaje que el bug #21 con {@code isCorporate}:
 * {@link com.fasterxml.jackson.annotation.JsonProperty} evita que Jackson derive otra
 * clave del componente record.
 */
class FixedAssignmentActiveJsonTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void shouldSerializeContractKeys_whenSerializingActiveResponse() throws Exception {
        // Arrange
        FixedAssignmentResponse response = new FixedAssignmentResponse(
                42L, 8L, 15L, 1, true, 1L,
                Instant.parse("2026-01-01T00:00:00Z"), null, null, ResourceType.PARKING);

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert: cada clave contractual exacta, sin prefijo derivado (p. ej. isActive)
        assertThat(json)
                .contains("\"active\":true")
                .contains("\"dayOfWeek\":1")
                .contains("\"parkingSpaceId\":8")
                .contains("\"employeeId\":15")
                .contains("\"createdById\":1")
                .doesNotContain("isActive");
    }

    @Test
    void shouldSerializeRevokedFields_whenAssignmentIsRevoked() throws Exception {
        // Arrange
        FixedAssignmentResponse response = new FixedAssignmentResponse(
                42L, 8L, 15L, 3, false, 1L,
                Instant.parse("2026-01-01T00:00:00Z"), 1L, Instant.parse("2026-02-01T00:00:00Z"), ResourceType.PARKING);

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert
        assertThat(json)
                .contains("\"active\":false")
                .contains("\"revokedById\":1")
                .contains("\"revokedAt\":");
    }
}
