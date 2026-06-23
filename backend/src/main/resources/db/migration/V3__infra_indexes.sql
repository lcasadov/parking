-- =====================================================================
-- V3 — Indices de infraestructura sobre las tablas de auditoria.
-- Soportan el navegado por fecha y el escaneo de la purga de retencion
-- (occurred_at < cutoff). Nombres segun docs/data-model.md §5.
-- =====================================================================

CREATE INDEX IX_audit_log_occurred_at ON dbo.audit_log(occurred_at);
GO

CREATE INDEX IX_login_log_occurred_at ON dbo.login_log(occurred_at);
GO
