package com.aleatica.parking.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios del serializador de detalles de auditoria: compone el contexto del actor
 * con un snapshot, y es best-effort ante un objeto no serializable.
 */
class AuditDetailsSerializerTest {

    private final AuditDetailsSerializer serializer = new AuditDetailsSerializer(new ObjectMapper());

    @Test
    void should_serialize_context_and_snapshot_to_json() {
        // Given
        AuditContext context = new AuditContext(7L, "admin", "10.0.0.5", "JUnit");

        // When
        String json = serializer.serialize(context, new Snapshot(1L, "ok"));

        // Then
        assertThat(json).contains("\"actorLogin\":\"admin\"")
                .contains("\"ip\":\"10.0.0.5\"")
                .contains("\"userAgent\":\"JUnit\"")
                .contains("\"snapshot\"");
    }

    @Test
    void should_serialize_context_without_snapshot_when_snapshot_null() {
        // Given
        AuditContext context = AuditContext.system();

        // When
        String json = serializer.serialize(context, null);

        // Then: incluye las claves de contexto (con valores nulos) y no la clave snapshot
        assertThat(json).contains("actorLogin").doesNotContain("snapshot");
    }

    @Test
    void should_return_null_when_serializing_null_snapshot_only() {
        // When / Then
        assertThat(serializer.serialize(null)).isNull();
    }

    @Test
    void should_return_json_for_non_null_snapshot_only() {
        // When / Then
        assertThat(serializer.serialize(new Snapshot(9L, "x"))).contains("\"id\":9");
    }

    private record Snapshot(Long id, String label) {
    }
}
