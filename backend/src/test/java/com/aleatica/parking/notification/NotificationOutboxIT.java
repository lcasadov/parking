package com.aleatica.parking.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aleatica.parking.notification.application.EmailDeliveryException;
import com.aleatica.parking.notification.application.EmailMessage;
import com.aleatica.parking.notification.application.NotificationDeliveryService;
import com.aleatica.parking.notification.event.RequestApprovedEvent;
import com.aleatica.parking.request.domain.RequestStatus;
import com.aleatica.parking.request.dto.RequestResponse;
import com.aleatica.parking.support.BaseIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tests de integracion de la capability {@code notifications} contra un SQL Server real
 * (Testcontainers), con el puerto de envio SMTP sustituido por un mock ({@link EmailSenderPort}).
 *
 * <p>Verifica las garantias que solo se observan con transaccion + BD reales: envio
 * {@code AFTER_COMMIT} a los admins activos (excluyendo inactivos), no envio ante rollback,
 * fallo SMTP que persiste un {@code email_outbox} PENDING sin revertir la operacion,
 * reintento del job (PENDING -> SENT), idempotencia, y las exclusiones (liberacion
 * voluntaria y cancelacion de la propia solicitud no notifican). El aislamiento entre ITs lo
 * garantiza {@link BaseIntegrationTest#resetDomainState()} (incluye {@code email_outbox}).</p>
 */
class NotificationOutboxIT extends BaseIntegrationTest {

    private static final String REQUESTS_URL = "/api/v1/requests";
    private static final String RELEASES_URL = "/api/v1/releases";
    private static final String ASSIGNMENTS_URL = "/api/v1/fixed-assignments";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String SESSION_COOKIE = "parking_SESSION";

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_PASSWORD = "Admin#Parking2026";
    private static final String ADMIN2_LOGIN = "ittest.notif.admin2";
    private static final String INACTIVE_ADMIN_LOGIN = "ittest.notif.admin3";
    private static final String EMP_LOGIN = "ittest.notif.emp";
    private static final String EMP_PASSWORD = "Notif#Pass1word";

    private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);
    private static final LocalDate WITHIN = TODAY.plusDays(3);

    @Autowired
    private NotificationDeliveryService deliveryService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie adminSession;
    private Cookie empSession;
    private long empId;
    private long spaceId;

    @BeforeEach
    void seed() throws Exception {
        empId = insertEmployee(EMP_LOGIN, "EMPLOYEE", true);
        insertEmployee(ADMIN2_LOGIN, "ADMIN", true);
        insertEmployee(INACTIVE_ADMIN_LOGIN, "ADMIN", false);
        adminSession = login(ADMIN_LOGIN, ADMIN_PASSWORD);
        empSession = login(EMP_LOGIN, EMP_PASSWORD);
        spaceId = insertSpace("P-NOTIF-01");
    }

    // ---- 1.1 / 1.9: nueva solicitud -> admins activos (excluye inactivos) ----

    @Test
    void shouldSendRequestCreatedEmailToActiveAdminsOnly_whenRequestCreated() throws Exception {
        // Arrange: destinatarios esperados = todos los ADMIN activos (admin seed + admin2),
        // nunca el admin inactivo.
        List<String> activeAdminEmails = activeAdminEmails();
        String inactiveAdminEmail = emailOf(INACTIVE_ADMIN_LOGIN);

        // Act: el empleado crea una solicitud (evento AFTER_COMMIT)
        createRequest(empSession, WITHIN).andExpect(status().isCreated());

        // Assert: un email por admin activo, ninguno al admin inactivo
        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSenderPort, times(activeAdminEmails.size())).send(captor.capture());
        List<String> recipients = captor.getAllValues().stream().map(EmailMessage::to).toList();
        assertThat(recipients).containsExactlyInAnyOrderElementsOf(activeAdminEmails);
        assertThat(recipients).doesNotContain(inactiveAdminEmail);
    }

    @Test
    void shouldNotSendAnyEmail_whenNoActiveAdminsExistOnRequestCreated() throws Exception {
        // Arrange: desactiva TODOS los administradores
        jdbcTemplate.update("UPDATE dbo.employees SET active = 0 WHERE role = 'ADMIN'");

        // Act
        createRequest(empSession, WITHIN).andExpect(status().isCreated());

        // Assert: sin destinatarios, no se envia ni encola nada; el flujo no falla
        verify(emailSenderPort, never()).send(any());
        assertThat(outboxCount()).isZero();
    }

    // ---- 1.3: transaccion revertida no genera email ----

    @Test
    void shouldNotSendAnyEmail_whenOriginatingTransactionRollsBack() {
        // Arrange: publica un evento dentro de una transaccion que se revierte
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        RequestResponse snapshot = new RequestResponse(
                1L, empId, WITHIN, RequestStatus.APPROVED, spaceId, "nota",
                null, null, adminId(), Instant.now(), Instant.now(), com.aleatica.parking.resource.ResourceType.PARKING);

        // Act
        tx.execute(statusCallback -> {
            eventPublisher.publishEvent(new RequestApprovedEvent(snapshot));
            statusCallback.setRollbackOnly();
            return null;
        });

        // Assert: AFTER_COMMIT no dispara ante rollback -> ningun envio
        verify(emailSenderPort, never()).send(any());
    }

    // ---- 1.4: fallo SMTP no revierte la operacion + persiste PENDING ----

    @Test
    void shouldLogFailureAndKeepRequestApproved_whenSmtpFails() throws Exception {
        // Arrange: el SMTP rechaza cualquier envio; hay una solicitud PENDING
        willThrow(new EmailDeliveryException("smtp down", new RuntimeException()))
                .given(emailSenderPort).send(any());
        long requestId = insertRequest(empId, WITHIN, "PENDING");

        // Act: aprobar -> 200 (la operacion NO se revierte pese al fallo de email)
        approve(adminSession, requestId, spaceId, "Bienvenido").andExpect(status().isOk());

        // Assert: la solicitud sigue APPROVED y el email quedo PENDING para reintento
        assertThat(statusOf(requestId)).isEqualTo("APPROVED");
        assertThat(pendingOutboxCount()).isEqualTo(1);
    }

    // ---- 1.5 / 1.10: reintento del job (PENDING -> SENT) + idempotencia ----

    @Test
    void shouldRetryPendingEmailsAndMarkSent_whenScheduledJobRuns() {
        // Arrange: una entrada PENDING sembrada; el SMTP ahora responde (mock ok por defecto)
        insertOutbox(empId, EmailOutboxStatus.PENDING);

        // Act
        deliveryService.retryPending();

        // Assert: reenviada con exito -> SENT
        verify(emailSenderPort, times(1)).send(any());
        assertThat(statusCountInOutbox(EmailOutboxStatus.SENT)).isEqualTo(1);
        assertThat(statusCountInOutbox(EmailOutboxStatus.PENDING)).isZero();
    }

    @Test
    void shouldNotResendAlreadySentEmail_whenRetryJobRunsAgain() {
        // Arrange: una entrada ya SENT (terminal)
        insertOutbox(empId, EmailOutboxStatus.SENT);

        // Act
        deliveryService.retryPending();

        // Assert: el job solo relee PENDING -> no reenvia la SENT (idempotencia)
        verify(emailSenderPort, never()).send(any());
        assertThat(statusCountInOutbox(EmailOutboxStatus.SENT)).isEqualTo(1);
    }

    // ---- 1.6: liberacion voluntaria no notifica ----

    @Test
    void shouldNotSendEmail_whenVoluntaryReleaseCreated() throws Exception {
        // Arrange: el empleado tiene una asignacion fija para la plaza ese dia de la semana
        insertFixedAssignment(spaceId, empId, WITHIN.getDayOfWeek().getValue());

        // Act: libera voluntariamente su recurso para WITHIN
        String body = "{\"releaseDate\":\"" + WITHIN + "\",\"parkingSpaceId\":" + spaceId + "}";
        mockMvc.perform(post(RELEASES_URL).cookie(empSession)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        // Assert: la liberacion voluntaria esta excluida -> ningun email
        verify(emailSenderPort, never()).send(any());
        assertThat(outboxCount()).isZero();
    }

    // ---- 1.7: cancelacion de la propia solicitud no notifica ----

    @Test
    void shouldNotSendEmail_whenEmployeeCancelsOwnRequest() throws Exception {
        // Arrange: el empleado crea una solicitud (esto SI notifica a los admins) y luego
        // se limpia el registro de invocaciones para aislar el efecto de la cancelacion.
        long requestId = insertRequest(empId, WITHIN, "PENDING");
        clearInvocations(emailSenderPort);

        // Act: el empleado cancela su propia solicitud
        mockMvc.perform(post(REQUESTS_URL + "/" + requestId + "/cancel").cookie(empSession))
                .andExpect(status().isOk());

        // Assert: la cancelacion propia esta excluida -> ningun email
        verify(emailSenderPort, never()).send(any());
    }

    // ---- 2.10(c): las reservas de visitante no notifican ----

    @Test
    void shouldNotSendEmail_whenVisitorReservationCreated() throws Exception {
        // Arrange: una ficha de visitante y una plaza/fecha libres
        long visitorId = insertVisitor("87654321X");

        // Act: el admin crea una reserva de visitante (plaza disponible ese dia)
        String body = "{\"visitorId\":" + visitorId + ",\"parkingSpaceId\":" + spaceId
                + ",\"reservationDate\":\"" + WITHIN + "\"}";
        mockMvc.perform(post("/api/v1/visitor-reservations").cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        // Assert: los visitantes no tienen cuenta y no reciben emails -> ningun envio ni encolado
        verify(emailSenderPort, never()).send(any());
        assertThat(outboxCount()).isZero();
    }

    // ---- 1.8: revocacion de asignacion fija -> empleado afectado ----

    @Test
    void shouldSendAssignmentRevokedEmailToAffectedEmployee_whenFixedAssignmentRevoked() throws Exception {
        // Arrange: el empleado tiene una asignacion fija activa
        insertFixedAssignment(spaceId, empId, WITHIN.getDayOfWeek().getValue());

        // Act: el admin revoca las asignaciones del empleado
        mockMvc.perform(delete(ASSIGNMENTS_URL + "/employee/" + empId).cookie(adminSession))
                .andExpect(status().isNoContent());

        // Assert: se notifica al empleado afectado (un unico email a su direccion)
        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSenderPort, times(1)).send(captor.capture());
        assertThat(captor.getValue().to()).isEqualTo(emailOf(EMP_LOGIN));
    }

    // ---- helpers ----

    private ResultActions createRequest(Cookie session, LocalDate date) throws Exception {
        String body = "{\"requestedDate\":\"" + date + "\"}";
        return mockMvc.perform(post(REQUESTS_URL).cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions approve(Cookie session, long id, long space, String note) throws Exception {
        String body = "{\"parkingSpaceId\":" + space + ",\"approvalNote\":\"" + note + "\"}";
        return mockMvc.perform(post(REQUESTS_URL + "/" + id + "/approve")
                .cookie(session).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private Cookie login(String login, String password) throws Exception {
        Cookie cookie = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(SESSION_COOKIE);
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private long insertEmployee(String login, String role, boolean active) {
        jdbcTemplate.update(
                "INSERT INTO dbo.employees "
                        + "(first_name, last_name, login, email, password_hash, password_must_change, "
                        + "is_corporate, auth_origin, role, enabled, active) "
                        + "VALUES ('IT', 'User', ?, ?, ?, 0, 0, 'LOCAL', ?, 1, ?)",
                login, login + "@aleatica.com", passwordEncoder.encode(EMP_PASSWORD), role,
                active ? 1 : 0);
        return idOfEmployee(login);
    }

    private long insertSpace(String label) {
        jdbcTemplate.update("INSERT INTO dbo.parking_spaces (number, label, active) VALUES ((SELECT ISNULL(MAX(number),1000)+1 FROM dbo.parking_spaces), ?, 1)", label);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.parking_spaces WHERE label = ?", Long.class, label);
        return id == null ? 0L : id;
    }

    private long insertRequest(long employeeId, LocalDate date, String status) {
        jdbcTemplate.update(
                "INSERT INTO dbo.requests (employee_id, requested_date, status, created_at) "
                        + "VALUES (?, ?, ?, ?)",
                employeeId, Date.valueOf(date), status, Timestamp.from(Instant.now()));
        Long id = jdbcTemplate.queryForObject(
                "SELECT TOP 1 id FROM dbo.requests WHERE employee_id = ? AND requested_date = ? "
                        + "AND status = ? ORDER BY id DESC",
                Long.class, employeeId, Date.valueOf(date), status);
        return id == null ? 0L : id;
    }

    private void insertFixedAssignment(long space, long employeeId, int dayOfWeek) {
        jdbcTemplate.update(
                "INSERT INTO dbo.fixed_assignments (resource_id, employee_id, day_of_week, "
                        + "active, created_by_id, created_at) VALUES (?, ?, ?, 1, ?, ?)",
                space, employeeId, dayOfWeek, adminId(), Timestamp.from(Instant.now()));
    }

    private long insertVisitor(String nationalId) {
        jdbcTemplate.update(
                "INSERT INTO dbo.visitors (first_name, last_name, national_id, created_by_id) "
                        + "VALUES ('Visita', 'Notif', ?, ?)",
                nationalId, adminId());
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.visitors WHERE national_id = ?", Long.class, nationalId);
        return id == null ? 0L : id;
    }

    private void insertOutbox(long recipientEmployeeId, EmailOutboxStatus status) {
        // Modelo por evento: se guarda el tipo de evento + destinatario (no HTML renderizado);
        // el reintento re-renderiza la plantilla vigente. ASSIGNMENT_REVOKED no requiere payload.
        jdbcTemplate.update(
                "INSERT INTO dbo.email_outbox "
                        + "(event_type, recipient_employee_id, status, attempts, created_at) "
                        + "VALUES ('ASSIGNMENT_REVOKED', ?, ?, 1, ?)",
                recipientEmployeeId, status.name(), Timestamp.from(Instant.now()));
    }

    private List<String> activeAdminEmails() {
        return jdbcTemplate.queryForList(
                "SELECT email FROM dbo.employees WHERE role = 'ADMIN' AND active = 1", String.class);
    }

    private String emailOf(String login) {
        return jdbcTemplate.queryForObject(
                "SELECT email FROM dbo.employees WHERE login = ?", String.class, login);
    }

    private long idOfEmployee(String login) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.employees WHERE login = ?", Long.class, login);
        return id == null ? 0L : id;
    }

    private long adminId() {
        return idOfEmployee(ADMIN_LOGIN);
    }

    private String statusOf(long requestId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM dbo.requests WHERE id = ?", String.class, requestId);
    }

    private int outboxCount() {
        return count("SELECT COUNT(*) FROM dbo.email_outbox");
    }

    private int pendingOutboxCount() {
        return statusCountInOutbox(EmailOutboxStatus.PENDING);
    }

    private int statusCountInOutbox(EmailOutboxStatus status) {
        return count("SELECT COUNT(*) FROM dbo.email_outbox WHERE status = ?", status.name());
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }
}
