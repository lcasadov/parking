-- =====================================================================
-- V39 — Histórico de cambios de un vehículo de empleado
-- (change employee-vehicle-self-service, Fase 2).
--
-- Registra el ciclo de vida del vehículo para que el ADMIN pueda ver cómo ha
-- ido cambiando: alta, ediciones del empleado (con foto de los datos previos),
-- cambios de estado (en trámite / aprobado / rechazado con motivo / restaurado)
-- y la solicitud de borrado. Se borra en cascada con el vehículo (el histórico
-- vive mientras el vehículo exista, incl. "pendiente de borrado").
-- =====================================================================

CREATE TABLE dbo.employee_vehicle_history (
    id                 BIGINT IDENTITY(1,1) NOT NULL,
    vehicle_id         BIGINT NOT NULL,
    event_type         NVARCHAR(30) NOT NULL,   -- CREATED | EDITED | STATUS_CHANGED | DELETION_REQUESTED
    actor_employee_id  BIGINT NULL,
    actor_role         NVARCHAR(20) NULL,       -- EMPLOYEE | ADMIN
    from_status        NVARCHAR(20) NULL,
    to_status          NVARCHAR(20) NULL,
    note               NVARCHAR(500) NULL,      -- p.ej. motivo del rechazo
    snapshot_json      NVARCHAR(MAX) NULL,      -- datos previos en una edición
    created_at         DATETIME2(3) NOT NULL
        CONSTRAINT DF_employee_vehicle_history_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_employee_vehicle_history PRIMARY KEY (id),
    CONSTRAINT FK_employee_vehicle_history_vehicle FOREIGN KEY (vehicle_id)
        REFERENCES dbo.employee_vehicles(id) ON DELETE CASCADE
);
GO

CREATE INDEX IX_employee_vehicle_history_vehicle
    ON dbo.employee_vehicle_history(vehicle_id);
GO
