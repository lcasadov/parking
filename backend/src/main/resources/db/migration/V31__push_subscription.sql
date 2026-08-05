-- =====================================================================
-- V31 — Suscripciones Web Push (change admin/push-notifications).
-- Cada fila es la suscripción de un navegador/dispositivo de un empleado
-- (endpoint + claves p256dh/auth del PushManager). Un empleado puede tener
-- varias (multi-dispositivo). Unicidad por endpoint (re-suscribir = upsert).
-- Borrado en cascada al eliminar el empleado.
-- =====================================================================
CREATE TABLE dbo.push_subscription (
    id           BIGINT IDENTITY(1,1) NOT NULL,
    employee_id  BIGINT        NOT NULL,
    endpoint     VARCHAR(1024) NOT NULL,
    p256dh       VARCHAR(255)  NOT NULL,
    auth         VARCHAR(255)  NOT NULL,
    user_agent   NVARCHAR(255) NULL,
    created_at   DATETIME2(3)  NOT NULL CONSTRAINT DF_push_subscription_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_push_subscription PRIMARY KEY (id),
    CONSTRAINT UQ_push_subscription_endpoint UNIQUE (endpoint),
    CONSTRAINT FK_push_subscription_employee FOREIGN KEY (employee_id)
        REFERENCES dbo.employees(id) ON DELETE CASCADE
);
GO
CREATE INDEX IX_push_subscription_employee ON dbo.push_subscription(employee_id);
GO
