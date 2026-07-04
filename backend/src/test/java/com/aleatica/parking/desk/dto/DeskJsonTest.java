package com.aleatica.parking.desk.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.aleatica.parking.desk.DeskCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Fija las claves JSON contractuales del puesto (autoridad {@code docs/openapi.yaml}:
 * {@code number}, {@code category}, {@code coordX}, {@code coordY}, {@code active}).
 * Mismo blindaje que el bug #21 con {@code isCorporate}:
 * {@link com.fasterxml.jackson.annotation.JsonProperty} evita que Jackson derive otra
 * clave del componente record, en serializacion (respuesta) y en binding (alta).
 */
class DeskJsonTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void shouldSerializeContractKeys_whenSerializingResponse() throws Exception {
        // Arrange
        DeskResponse response = new DeskResponse(1L, 12, "D-12", DeskCategory.EXECUTIVE,
                new BigDecimal("30.5"), new BigDecimal("47.0"), true,
                Instant.parse("2026-01-01T00:00:00Z"));

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert: claves contractuales exactas y enum por nombre, sin prefijo derivado
        assertThat(json)
                .contains("\"number\":12")
                .contains("\"category\":\"EXECUTIVE\"")
                .contains("\"coordX\":30.5")
                .contains("\"coordY\":47.0")
                .contains("\"active\":true")
                .doesNotContain("isActive");
    }

    @Test
    void shouldBindCreateBody_whenDeserializingRequest() throws Exception {
        // Arrange
        String body = "{\"number\":12,\"category\":\"STANDARD\",\"coordX\":30.5,\"coordY\":47.0}";

        // Act
        DeskCreateRequest request = objectMapper.readValue(body, DeskCreateRequest.class);

        // Assert
        assertThat(request.number()).isEqualTo(12);
        assertThat(request.category()).isEqualTo(DeskCategory.STANDARD);
        assertThat(request.coordXOrDefault()).isEqualByComparingTo("30.5");
    }

    @Test
    void shouldDefaultCoordinates_whenCreateBodyOmitsThem() throws Exception {
        // Arrange
        String body = "{\"number\":12,\"category\":\"STANDARD\"}";

        // Act
        DeskCreateRequest request = objectMapper.readValue(body, DeskCreateRequest.class);

        // Assert
        assertThat(request.coordX()).isNull();
        assertThat(request.coordXOrDefault()).isEqualByComparingTo("50");
        assertThat(request.coordYOrDefault()).isEqualByComparingTo("50");
    }
}
