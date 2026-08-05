package com.aleatica.parking.employee.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.aleatica.parking.employee.AuthOrigin;
import com.aleatica.parking.employee.EmployeeCategory;
import com.aleatica.parking.employee.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Regresion del bug #21: el flag corporativo DEBE viajar por el contrato como la
 * clave JSON {@code isCorporate} (autoridad {@code docs/openapi.yaml}), no
 * {@code corporate}. Jackson, por defecto, deriva {@code corporate} del
 * componente record; estas pruebas fijan la clave contractual en ambos sentidos
 * (serializacion de la respuesta y binding de las peticiones de alta/edicion).
 */
class EmployeeCorporateJsonTest {

    private static final String CONTRACT_KEY = "\"isCorporate\"";
    private static final String LEGACY_KEY = "\"corporate\"";

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void shouldSerializeCorporateFlagAsIsCorporate_whenSerializingResponse() throws Exception {
        // Arrange
        EmployeeResponse response = new EmployeeResponse(
                1L, "Juan", "Perez", "jperez", "jperez@aleatica.com",
                "IT", "600100200", "1234ABC", true, AuthOrigin.ENTRA_ID,
                Role.EMPLOYEE, EmployeeCategory.EMPLEADO, true, true, false, true, true,
                Instant.parse("2026-01-01T00:00:00Z"), null);

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert
        assertThat(json).contains(CONTRACT_KEY).doesNotContain(LEGACY_KEY);
    }

    @Test
    void shouldBindCorporateFlag_whenDeserializingCreateWithIsCorporate() throws Exception {
        // Arrange
        String body = "{\"firstName\":\"Juan\",\"lastName\":\"Perez\",\"login\":\"jperez\","
                + "\"email\":\"jperez@aleatica.com\",\"isCorporate\":true,\"role\":\"EMPLOYEE\"}";

        // Act
        EmployeeCreateRequest request = objectMapper.readValue(body, EmployeeCreateRequest.class);

        // Assert
        assertThat(request.corporate()).isTrue();
    }

    @Test
    void shouldBindCorporateFlag_whenDeserializingUpdateWithIsCorporate() throws Exception {
        // Arrange
        String body = "{\"firstName\":\"Juan\",\"lastName\":\"Perez\","
                + "\"email\":\"jperez@aleatica.com\",\"isCorporate\":true,\"role\":\"ADMIN\"}";

        // Act
        EmployeeUpdateRequest request = objectMapper.readValue(body, EmployeeUpdateRequest.class);

        // Assert
        assertThat(request.corporate()).isTrue();
    }
}
