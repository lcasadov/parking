-- =====================================================================
-- V4 — Tabla employees (NUEVA en el change funcional auth-local).
-- Esquema completo segun docs/data-model.md §3.1: identidad, credenciales
-- locales (Fase 1 / fallback), rol, estado y campos de bloqueo por intentos.
--
-- Tras crear employees, se anaden las FOREIGN KEY pendientes desde las tablas
-- de auditoria (audit_log.actor_employee_id, login_log.employee_id), que
-- bootstrap-mvp dejo como BIGINT NULL sin constraint hasta que existiera esta
-- tabla (ver V2__audit_and_login_log.sql).
-- =====================================================================

CREATE TABLE dbo.employees (
    id                       BIGINT IDENTITY(1,1) NOT NULL,
    first_name               NVARCHAR(100) NOT NULL,
    last_name                NVARCHAR(150) NOT NULL,
    login                    NVARCHAR(100) NOT NULL,
    email                    NVARCHAR(255) NOT NULL,
    password_hash            VARCHAR(72)   NULL,
    password_must_change     BIT NOT NULL CONSTRAINT DF_employees_password_must_change DEFAULT 0,
    department               NVARCHAR(100) NULL,
    mobile_phone             NVARCHAR(30)  NULL,
    license_plate            NVARCHAR(15)  NULL,
    is_corporate             BIT NOT NULL CONSTRAINT DF_employees_is_corporate DEFAULT 0,
    auth_origin              VARCHAR(10) NOT NULL CONSTRAINT DF_employees_auth_origin DEFAULT 'LOCAL',
    role                     VARCHAR(10) NOT NULL,
    enabled                  BIT NOT NULL CONSTRAINT DF_employees_enabled DEFAULT 1,
    active                   BIT NOT NULL CONSTRAINT DF_employees_active DEFAULT 1,
    failed_login_attempts    INT NOT NULL CONSTRAINT DF_employees_failed_attempts DEFAULT 0,
    locked_until             DATETIME2(3) NULL,
    last_password_change_at  DATETIME2(3) NULL,
    created_at               DATETIME2(3) NOT NULL CONSTRAINT DF_employees_created_at DEFAULT SYSUTCDATETIME(),
    updated_at               DATETIME2(3) NULL,
    CONSTRAINT PK_employees PRIMARY KEY (id),
    CONSTRAINT CK_employees_role        CHECK (role IN ('ADMIN','EMPLOYEE')),
    CONSTRAINT CK_employees_auth_origin CHECK (auth_origin IN ('LOCAL','ENTRA_ID'))
);
GO

CREATE UNIQUE INDEX UX_employees_login ON dbo.employees(login);
GO

CREATE UNIQUE INDEX UX_employees_email ON dbo.employees(email);
GO

-- FKs pendientes desde las tablas de auditoria (creadas sin constraint en V2).
ALTER TABLE dbo.audit_log
    ADD CONSTRAINT FK_audit_log_actor
        FOREIGN KEY (actor_employee_id) REFERENCES dbo.employees(id);
GO

ALTER TABLE dbo.login_log
    ADD CONSTRAINT FK_login_log_employee
        FOREIGN KEY (employee_id) REFERENCES dbo.employees(id);
GO
