package com.aleatica.parking.push;

import static org.assertj.core.api.Assertions.assertThat;

import com.aleatica.parking.notification.NotificationEventType;
import com.aleatica.parking.notification.application.NotificationCommand;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.resource.ResourceType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests de {@link PushContentRenderer}: cada tipo de evento produce un JSON con title/body/url,
 * el deep-link es de empleado o de admin segun el evento, y el tipo de recurso rotula la etiqueta.
 */
class PushContentRendererTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final PushContentRenderer renderer = new PushContentRenderer(MAPPER);

    private static RequestResponse parking() {
        return new RequestResponse(42L, 15L, LocalDate.of(2026, 7, 10), RequestStatus.APPROVED,
                3005L, null, null, null, 1L, Instant.parse("2026-07-04T10:00:00Z"),
                Instant.parse("2026-07-04T10:00:00Z"), ResourceType.PARKING, null, null);
    }

    @ParameterizedTest
    @EnumSource(NotificationEventType.class)
    void shouldRenderTitleBodyAndUrl_forEveryEventType(NotificationEventType type) throws Exception {
        String json = renderer.render(new NotificationCommand(type, 15L, parking()));
        JsonNode node = MAPPER.readTree(json);
        assertThat(node.get("title").asText()).isNotBlank();
        assertThat(node.get("body").asText()).isNotBlank();
        assertThat(node.get("url").asText()).startsWith("/");
    }

    @Test
    void shouldDeepLinkToAdminTray_whenAdminEvent() throws Exception {
        String json = renderer.render(new NotificationCommand(NotificationEventType.REQUEST_CREATED, 1L, parking()));
        assertThat(MAPPER.readTree(json).get("url").asText()).isEqualTo("/admin/requests");
    }

    @Test
    void shouldDeepLinkToEmployeeWeek_whenEmployeeEvent() throws Exception {
        String json = renderer.render(new NotificationCommand(NotificationEventType.REQUEST_APPROVED, 15L, parking()));
        assertThat(MAPPER.readTree(json).get("url").asText()).isEqualTo("/employee/my-week");
    }

    @Test
    void shouldLabelDesk_whenResourceIsDesk() throws Exception {
        RequestResponse desk = new RequestResponse(1L, 15L, LocalDate.of(2026, 7, 10), RequestStatus.APPROVED,
                1L, null, null, null, 1L, Instant.parse("2026-07-04T10:00:00Z"),
                Instant.parse("2026-07-04T10:00:00Z"), ResourceType.DESK, null, null);
        String json = renderer.render(new NotificationCommand(NotificationEventType.REQUEST_APPROVED, 15L, desk));
        assertThat(MAPPER.readTree(json).get("body").asText()).contains("puesto");
    }

    @Test
    void shouldHandleNullRequest_whenAssignmentRevoked() throws Exception {
        String json = renderer.render(new NotificationCommand(NotificationEventType.ASSIGNMENT_REVOKED, 15L, null));
        assertThat(MAPPER.readTree(json).get("title").asText()).isNotBlank();
    }
}
