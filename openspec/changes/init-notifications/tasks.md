# Tasks: init-notifications

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [ ] 1.1 `should_send_request_created_email_to_active_admins_when_request_created` (excluye admins `active = false`).
- [ ] 1.2 `should_send_request_approved_email_with_approval_note_when_request_approved`.
- [ ] 1.3 `should_not_send_any_email_when_originating_transaction_rolls_back`.
- [ ] 1.4 `should_log_failure_and_keep_request_approved_when_smtp_fails`.
- [ ] 1.5 `should_retry_pending_emails_when_scheduled_job_runs` (éxito marca enviado, fallo conserva pendiente).
- [ ] 1.6 `should_not_send_email_when_voluntary_release_created`.
- [ ] 1.7 `should_not_send_email_when_employee_cancels_own_request`.
- [ ] 1.8 `should_send_assignment_revoked_email_to_affected_employee_when_fixed_assignment_revoked`.
- [ ] 1.9 `should_not_send_email_when_no_active_admins_exist_on_request_created`.
- [ ] 1.10 `should_not_resend_already_sent_email_when_retry_job_runs_again` (idempotencia).
- [ ] 1.11 Cada scenario BDD del spec cubierto por ≥1 test (nombres `should..._when...`).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [ ] 2.1 Definir eventos de dominio: `RequestCreatedEvent`, `RequestApprovedEvent`, `RequestRejectedEvent`, `FixedAssignmentRevokedEvent`, 🔵 `PasswordResetEvent`.
- [ ] 2.2 Puerto de salida `EmailSenderPort` + adaptador SMTP (JavaMailSender) con render Thymeleaf.
- [ ] 2.3 Listener `@TransactionalEventListener(phase = AFTER_COMMIT)` que mapea cada evento a plantilla + destinatarios.
- [ ] 2.4 Resolución de destinatarios: query de `Employee` con `role = ADMIN` y `active = true` para "nueva solicitud"; empleado afectado para el resto.
- [ ] 2.5 Plantillas Thymeleaf: `request-created.html`, `request-approved.html` (con `approval_note`), `request-rejected.html` (con `rejection_reason`), `assignment-revoked.html`, 🔵 `password-reset.html`.
- [ ] 2.6 Resiliencia SMTP: try/catch que registra log y persiste email pendiente de reintento, sin relanzar excepción al flujo origen.
- [ ] 2.7 Almacén de emails pendientes (entidad + repositorio puerto/adaptador) _[verificar con docs/data-model.md]_ + migración Flyway.
- [ ] 2.8 Job `@Scheduled` de reintento: relee pendientes, reintenta, marca enviados, idempotente; política de máximo de reintentos.
- [ ] 2.9 `ClockPort` inyectable para timestamps y ventanas de reintento.
- [ ] 2.10 Asegurar que NO se publican eventos en liberación voluntaria (`Release VOLUNTARY`), cancelación de la propia solicitud (`Request CANCELLED` por el empleado) ni reservas de visitante.

## 3. Refactor
- [ ] 3.1 Con los tests en verde: extraer métodos (complejidad < 15), eliminar duplicación y aplicar `docs/SONAR-STANDARDS.md`, sin cambiar comportamiento.

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [ ] 4.1 (Sin UI propia.) Verificar que las pantallas que disparan eventos muestran toast de confirmación de la operación (no del envío del email).
