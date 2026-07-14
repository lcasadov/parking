package com.aleatica.parking.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.aleatica.parking.auth.domain.ClockPort;
import com.aleatica.parking.employee.Employee;
import com.aleatica.parking.employee.EmployeeRepository;
import com.aleatica.parking.employee.Role;
import com.aleatica.parking.notification.EmailOutbox;
import com.aleatica.parking.notification.EmailOutboxRepository;
import com.aleatica.parking.notification.EmailOutboxStatus;
import com.aleatica.parking.notification.NotificationEventType;
import com.aleatica.parking.parkingspace.ParkingSpace;
import com.aleatica.parking.parkingspace.ParkingSpaceRepository;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.resource.ResourceType;
import com.aleatica.parking.support.EmployeeTestFactory;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.dialect.SpringStandardDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Test de regresion del bug del outbox: prueba que un reintento de {@code REQUEST_APPROVED}
 * <strong>re-renderiza con la plantilla vigente</strong> (no reenvia HTML congelado). Encadena
 * los colaboradores reales de renderizado (Thymeleaf real, {@link EmailContentRenderer},
 * {@link NotificationRenderer}, {@link NotificationOutboxMapper}) sobre una entrada
 * {@code PENDING} del outbox y verifica que el cuerpo reenviado contiene el texto de la
 * plantilla ACTUAL ("Estimado/a", "Se le ha asignado la plaza nº"), y no el de la plantilla
 * antigua ni el requestId como referencia del recurso. Solo se mockea el borde SMTP y el
 * repositorio del outbox.
 */
@ExtendWith(MockitoExtension.class)
class NotificationRetryRerenderTest {

    private static final Instant NOW = Instant.parse("2026-07-04T10:00:00Z");
    private static final Long EMP_ID = 15L;
    private static final Long REQUEST_ID = 42L;
    private static final Long PARKING_RESOURCE_ID = 8L;
    private static final int MAX_ATTEMPTS = 5;

    @Mock
    private EmailSenderPort emailSenderPort;

    @Mock
    private EmailOutboxRepository outboxRepository;

    @Mock
    private PendingEmailStore pendingEmailStore;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ParkingSpaceRepository parkingSpaceRepository;

    @Mock
    private com.aleatica.parking.desk.DeskRepository deskRepository;

    private final NotificationOutboxMapper mapper =
            new NotificationOutboxMapper(JsonMapper.builder().addModule(new JavaTimeModule()).build());
    private final ClockPort clock = () -> NOW;

    private NotificationDeliveryService service() {
        EmailContentRenderer contentRenderer = new EmailContentRenderer(templateEngine());
        NotificationRenderer notificationRenderer = new NotificationRenderer(
                employeeRepository, contentRenderer, parkingSpaceRepository, deskRepository);
        return new NotificationDeliveryService(
                emailSenderPort, outboxRepository, pendingEmailStore,
                notificationRenderer, mapper, clock, MAX_ATTEMPTS);
    }

    @Test
    void shouldReRenderWithCurrentTemplate_whenRetryingApprovedNotification() {
        // Arrange: una entrada PENDING encolada como orden del evento (no HTML congelado)
        RequestResponse approved = new RequestResponse(
                REQUEST_ID, EMP_ID, LocalDate.of(2026, 7, 10), RequestStatus.APPROVED,
                PARKING_RESOURCE_ID, "Plaza junto al ascensor", null, null, 1L, NOW, NOW,
                ResourceType.PARKING);
        NotificationCommand command =
                new NotificationCommand(NotificationEventType.REQUEST_APPROVED, EMP_ID, approved);
        EmailOutbox pending = mapper.toPendingOutbox(command, "smtp down", NOW);

        Employee employee = EmployeeTestFactory.active(EMP_ID, "emp", "emp@aleatica.com", null, Role.EMPLOYEE);
        given(outboxRepository.findByStatus(EmailOutboxStatus.PENDING)).willReturn(List.of(pending));
        given(employeeRepository.findById(EMP_ID)).willReturn(Optional.of(employee));
        given(parkingSpaceRepository.findById(PARKING_RESOURCE_ID))
                .willReturn(Optional.of(ParkingSpace.create(3005)));

        // Act
        service().retryPending();

        // Assert: el cuerpo reenviado usa la plantilla ACTUAL (no la antigua)
        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        org.mockito.Mockito.verify(emailSenderPort).send(captor.capture());
        String body = captor.getValue().htmlBody();
        assertThat(body)
                .contains("Estimado/a Sr./Sra. Test User")
                .contains("APROBADA")
                .contains("Se le ha asignado la plaza n")
                .contains("3005")
                .contains("Plaza junto al ascensor");
        // No reenvia el formato antiguo ni el requestId como referencia del recurso
        assertThat(body).doesNotContain("referencia");
        assertThat(body).doesNotContain(String.valueOf(REQUEST_ID));
        assertThat(pending.getStatus()).isEqualTo(EmailOutboxStatus.SENT);
    }

    private static TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        TemplateEngine engine = new TemplateEngine();
        engine.setDialect(new SpringStandardDialect());
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
