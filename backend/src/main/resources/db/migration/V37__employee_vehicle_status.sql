-- =====================================================================
-- V37 — Estado de validación de los vehículos de empleado
-- (change funcional employee-vehicle-self-service, Fase 1).
--
-- Los vehículos ganan un ciclo de validación: PENDING (alta/edición por el
-- propio empleado en el self-service), APPROVED (validado; también el alta
-- por el ADMIN nace APPROVED) y REJECTED (con motivo). Se añade auditoría
-- mínima de la revisión (reviewed_at / reviewed_by).
--
-- Los vehículos YA existentes se dieron de alta por el ADMIN (gestión previa),
-- así que se consideran validados -> se marcan APPROVED.
-- =====================================================================

ALTER TABLE dbo.employee_vehicles
    ADD status NVARCHAR(20) NOT NULL
            CONSTRAINT DF_employee_vehicles_status DEFAULT 'PENDING',
        rejection_reason NVARCHAR(500) NULL,
        reviewed_at DATETIME2(3) NULL,
        reviewed_by BIGINT NULL;
GO

-- Los vehículos preexistentes (alta por ADMIN) ya están validados.
UPDATE dbo.employee_vehicles SET status = 'APPROVED';
GO

-- Lookup de la bandeja de validación (Fase 2) y del recuento de pendientes.
CREATE INDEX IX_employee_vehicles_status ON dbo.employee_vehicles(status);
GO
