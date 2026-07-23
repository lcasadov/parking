-- =====================================================================
-- V24 — Amplia CK_email_outbox_event_type con los valores de NotificationEventType
-- anadidos despues de V23: REQUEST_CANCELLED (capability "liberacion notifica a admins") y
-- REQUEST_ADMIN_ASSIGNED (change restructure-admin-workflows, capability
-- admin-punctual-assignment).
--
-- Bug corregido: al fallar el envio SMTP de una asignacion puntual del admin, el
-- encolado en email_outbox violaba esta CHECK constraint (evento no listado) y la fila
-- PENDING nunca se persistia; el fallo quedaba silenciado porque ocurre en la fase
-- AFTER_COMMIT (Spring solo lo registra en el log, no revierte la operacion ya
-- confirmada). El mismo hueco afectaba, sin test que lo cubriera aun, a
-- REQUEST_CANCELLED. PASSWORD_RESET se mantiene deliberadamente excluido (RGPD / OWASP
-- A02, ver NotificationEventType).
-- =====================================================================

ALTER TABLE dbo.email_outbox DROP CONSTRAINT CK_email_outbox_event_type;
GO

ALTER TABLE dbo.email_outbox ADD CONSTRAINT CK_email_outbox_event_type
    CHECK (event_type IN (
        'REQUEST_CREATED', 'REQUEST_APPROVED', 'REQUEST_REJECTED', 'REQUEST_CANCELLED',
        'ASSIGNMENT_REVOKED', 'REQUEST_ADMIN_ASSIGNED'));
GO
