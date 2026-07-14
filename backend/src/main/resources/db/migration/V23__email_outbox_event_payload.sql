-- =====================================================================
-- V23 — Rediseno de email_outbox: guardar el EVENTO (event_type + payload) en lugar del
-- HTML ya renderizado, para que el job de reintento re-renderice con la plantilla vigente
-- (change fix-email-outbox-rerender, capability notifications).
--
-- Motivo: el outbox guardaba recipient/subject/body_html (HTML congelado en el momento del
-- fallo). Si una plantilla cambiaba (p. ej. templates/email/request-approved.html), los
-- reintentos reenviaban el contenido ANTIGUO. Ahora la fila guarda los datos del evento y el
-- reintento re-resuelve el destinatario y el recurso y re-renderiza la plantilla ACTUAL.
--
-- PURGA (obligatoria): las filas PENDING existentes reenvian HTML obsoleto y no contienen
-- datos de evento con los que re-renderizar; se ELIMINAN todas las filas (la tabla es un
-- almacen best-effort transitorio) antes de migrar el esquema.
--
-- Seguridad (RGPD / OWASP A02): PASSWORD_RESET NO se encola en el outbox porque su correo
-- lleva la contrasena temporal en claro y no debe persistirse en reposo; por eso event_type
-- no admite ese valor (ver enum NotificationEventType).
-- =====================================================================

-- 1) Purga de filas obsoletas (reenviarian el HTML antiguo; sin datos de evento migrables).
DELETE FROM dbo.email_outbox;
GO

-- 2) Elimina las columnas del mensaje ya renderizado.
ALTER TABLE dbo.email_outbox DROP COLUMN recipient, subject, body_html;
GO

-- 3) Anade las columnas del modelo por evento. La tabla quedo vacia en el paso 1, por lo que
--    las columnas NOT NULL pueden anadirse sin valor por defecto.
ALTER TABLE dbo.email_outbox ADD
    event_type            VARCHAR(30) NOT NULL,
    recipient_employee_id BIGINT NOT NULL,
    request_payload       NVARCHAR(MAX) NULL;
GO

-- 4) Restringe event_type al conjunto conocido (PASSWORD_RESET excluido por seguridad).
ALTER TABLE dbo.email_outbox ADD CONSTRAINT CK_email_outbox_event_type
    CHECK (event_type IN ('REQUEST_CREATED', 'REQUEST_APPROVED', 'REQUEST_REJECTED', 'ASSIGNMENT_REVOKED'));
GO
