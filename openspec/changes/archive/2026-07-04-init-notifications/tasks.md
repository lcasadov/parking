# Tasks: init-notifications

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

> **Nombres de test:** el proyecto usa la convención camelCase `shouldX_whenY` (Google Java
> Style + `docs/TESTING-STRATEGY.md`), no snake_case. A la derecha de cada caso se indica el
> test concreto (clase::método) que lo cubre. IT = `NotificationOutboxIT` (SQL Server real,
> `EmailSenderPort` mockeado); el resto son unit tests con Mockito.

## 1. Tests primero — RED (deben fallar antes de implementar)
- [x] 1.1 admins activos, excluye `active=false` → `NotificationOutboxIT::shouldSendRequestCreatedEmailToActiveAdminsOnly_whenRequestCreated` + `NotificationDispatcherTest::shouldQueueOneEmailPerActiveAdmin_whenRequestCreated`.
- [x] 1.2 aprobada con `approvalNote` → `EmailContentRendererTest::shouldCarryApprovalNoteInBody_whenRenderingRequestApproved` + `NotificationDispatcherTest::shouldQueueEmailToRequester_whenRequestApproved`.
- [x] 1.3 rollback no envía → `NotificationOutboxIT::shouldNotSendAnyEmail_whenOriginatingTransactionRollsBack`.
- [x] 1.4 fallo SMTP mantiene APPROVED + encola → `NotificationOutboxIT::shouldLogFailureAndKeepRequestApproved_whenSmtpFails` + `NotificationDeliveryServiceTest::shouldQueuePendingEmail_whenSmtpFailsOnImmediateSend`.
- [x] 1.5 reintento del job (éxito→SENT, fallo→PENDING) → `NotificationOutboxIT::shouldRetryPendingEmailsAndMarkSent_whenScheduledJobRuns` + `NotificationDeliveryServiceTest::{shouldMarkSent_whenRetrySucceeds, shouldKeepPending_whenRetryFailsBelowMaxAttempts, shouldMarkFailed_whenRetryExhaustsMaxAttempts}`.
- [x] 1.6 liberación voluntaria no envía → `NotificationOutboxIT::shouldNotSendEmail_whenVoluntaryReleaseCreated`.
- [x] 1.7 cancelación propia no envía → `NotificationOutboxIT::shouldNotSendEmail_whenEmployeeCancelsOwnRequest`.
- [x] 1.8 revocación → empleado afectado → `NotificationOutboxIT::shouldSendAssignmentRevokedEmailToAffectedEmployee_whenFixedAssignmentRevoked` + `NotificationDispatcherTest::shouldQueueEmailToAffectedEmployee_whenAssignmentRevoked` + `FixedAssignmentServiceTest::shouldPublishRevokedEvent_whenAdminRevokesAssignments`.
- [x] 1.9 sin admins activos no envía → `NotificationOutboxIT::shouldNotSendAnyEmail_whenNoActiveAdminsExistOnRequestCreated` + `NotificationDispatcherTest::shouldQueueNothing_whenNoActiveAdminsExistOnRequestCreated`.
- [x] 1.10 idempotencia (no reenvía SENT) → `NotificationOutboxIT::shouldNotResendAlreadySentEmail_whenRetryJobRunsAgain` + `NotificationDeliveryServiceTest::shouldNotResendAlreadySent_whenRetryJobRunsAgain`.
- [x] 1.11 cada scenario BDD del spec cubierto por ≥1 test (ver mapeo 1.1–1.10).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [x] 2.1 Eventos de dominio: `RequestCreatedEvent`, `RequestApprovedEvent`, `RequestRejectedEvent`, `FixedAssignmentRevokedEvent`, 🔵 `PasswordResetEvent` (en `com.aleatica.parking.notification.event`). Se **consolidó** el stub previo `RequestNotificationEvent(Kind)` (borrado) en estos eventos distintos; `RequestService` publica los nuevos, comportamiento idéntico.
- [x] 2.2 Puerto `EmailSenderPort` + adaptador SMTP `SmtpEmailSender` (JavaMailSender) + render Thymeleaf (`EmailContentRenderer`). Añadido `spring-boot-starter-thymeleaf` al pom.
- [x] 2.3 Listener `EmailNotificationListener` `@TransactionalEventListener(AFTER_COMMIT)`: un método por evento → `NotificationDispatcher`.
- [x] 2.4 Resolución de destinatarios (`NotificationDispatcher`): `EmployeeRepository.findByRoleAndActiveTrue(ADMIN)` para "nueva solicitud"; empleado afectado (`findById`) para el resto.
- [x] 2.5 Plantillas Thymeleaf en `templates/email/`: `request-created`, `request-approved` (con `approvalNote`), `request-rejected` (con `rejectionReason`), `assignment-revoked`, 🔵 `password-reset`.
- [x] 2.6 Resiliencia SMTP (`NotificationDeliveryService.sendOrQueue`): try/catch que loguea (sin PII) y encola vía `PendingEmailStore` (`REQUIRES_NEW`), sin relanzar al flujo origen.
- [x] 2.7 Almacén `email_outbox` (entidad `EmailOutbox` + `EmailOutboxRepository`) + migración **`V11__email_outbox.sql`**. ⚠️ **`docs/data-model.md` NO define esta tabla**; se diseñó aquí (recipient, subject, body_html, status PENDING/SENT/FAILED, attempts, timestamps) y queda **pendiente de reflejar en `docs/data-model.md`** por el orquestador.
- [x] 2.8 Job `EmailRetryJob` `@Scheduled(fixedDelayString=parking.notifications.retry-interval-ms)` → `retryPending()`: relee PENDING, reintenta, marca SENT, idempotente; política `parking.notifications.max-attempts` (5). `@EnableScheduling` en `NotificationSchedulingConfig`.
- [x] 2.9 `ClockPort` inyectado en `NotificationDeliveryService`/`EmailOutbox` para timestamps y ventanas de reintento (reutiliza el puerto existente).
- [x] 2.10 Exclusiones verificadas: `ReleaseService`/`VisitorService(Reservation)` publican solo eventos de AUDIT (no notificación) y `RequestService.cancel` no publica nada → sin email en liberación voluntaria, cancelación propia ni visitantes (ITs 1.6/1.7 + estructura).

## 3. Refactor
- [x] 3.1 Complejidad < 15 por método (listener 1 llamada/método, dispatcher/delivery con helpers `findRecipient`/`retryOne`), literales repetidos → constantes `static final` (S1192), sin `Thread.sleep` en tests (S2925: `retryPending()` invocado directamente + AFTER_COMMIT síncrono). Quality Gate JaCoCo verde (≥80/75).

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [x] 4.1 **Sin UI propia** (capability transversal). No se añade frontend en este change; las pantallas que disparan los eventos ya muestran su toast de confirmación de la operación (no del envío del email).
