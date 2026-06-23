-- =====================================================================
-- V2 — Tablas de auditoria: audit_log y login_log.
-- Esquema segun docs/data-model.md §3.8 y §3.9.
--
-- Nota de alcance: las columnas actor_employee_id / employee_id NO llevan aun
-- la FOREIGN KEY a dbo.employees porque esa tabla la crea el change funcional de
-- empleados (este change es solo infraestructura). El change funcional anadira la
-- FK cuando exista employees. Hasta entonces son BIGINT NULL sin constraint.
-- =====================================================================

-- 3.8 audit_log — rastro de auditoria funcional (lo puebla un aspecto AOP).
CREATE TABLE dbo.audit_log (
    id                 BIGINT IDENTITY(1,1) NOT NULL,
    actor_employee_id  BIGINT NULL,
    action             NVARCHAR(100) NOT NULL,
    entity_type        NVARCHAR(100) NOT NULL,
    entity_id          BIGINT NULL,
    details            NVARCHAR(MAX) NULL,
    occurred_at        DATETIME2(3) NOT NULL CONSTRAINT DF_audit_log_occurred_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_audit_log PRIMARY KEY (id)
);
GO

-- 3.9 login_log — todo intento de autenticacion (separado de la auditoria funcional).
CREATE TABLE dbo.login_log (
    id               BIGINT IDENTITY(1,1) NOT NULL,
    login_attempted  NVARCHAR(100) NOT NULL,
    employee_id      BIGINT NULL,
    result           VARCHAR(20) NOT NULL,
    phase            VARCHAR(10) NOT NULL,
    ip_address       NVARCHAR(45) NULL,
    user_agent       NVARCHAR(512) NULL,
    occurred_at      DATETIME2(3) NOT NULL CONSTRAINT DF_login_log_occurred_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_login_log PRIMARY KEY (id),
    CONSTRAINT CK_login_log_result CHECK (result IN ('OK','INVALID_CREDENTIALS','LOCKED','INACTIVE','NO_ACCESS','FALLBACK_OK')),
    CONSTRAINT CK_login_log_phase  CHECK (phase  IN ('PHASE_1','PHASE_2','FALLBACK'))
);
GO
