-- =====================================================================
-- V7 — Tabla fixed_assignments (change funcional init-fixed-assignments).
-- Vinculo indefinido empleado <-> plaza de parking por dia de la semana
-- (day_of_week 1-7). Revocacion LOGICA (active=0 + revoked_at/revoked_by_id),
-- nunca borrado fisico. Esquema segun docs/data-model.md §3.3.
--
-- Unicidad de negocio SOLO entre filas activas (permite que el historico
-- revocado coexista con futuras filas activas del mismo par plaza/dia o
-- empleado/dia): se impone con INDICES UNICOS FILTRADOS de SQL Server
-- (CREATE UNIQUE INDEX ... WHERE active = 1). Un UNIQUE clasico prohibiria
-- tambien los duplicados historicos (data-model §"Why filtered, not constraints").
-- La segunda insercion en conflicto -> DataIntegrityViolation -> 409 en la app.
-- =====================================================================

CREATE TABLE dbo.fixed_assignments (
    id                BIGINT IDENTITY(1,1) NOT NULL,
    parking_space_id  BIGINT NOT NULL,
    employee_id       BIGINT NOT NULL,
    day_of_week       TINYINT NOT NULL,
    active            BIT NOT NULL CONSTRAINT DF_fixed_assignments_active DEFAULT 1,
    created_by_id     BIGINT NOT NULL,
    created_at        DATETIME2(3) NOT NULL CONSTRAINT DF_fixed_assignments_created_at DEFAULT SYSUTCDATETIME(),
    revoked_by_id     BIGINT NULL,
    revoked_at        DATETIME2(3) NULL,
    CONSTRAINT PK_fixed_assignments PRIMARY KEY (id),
    CONSTRAINT CK_fixed_assignments_day_of_week CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT FK_fixed_assignments_parking_spaces FOREIGN KEY (parking_space_id) REFERENCES dbo.parking_spaces(id),
    CONSTRAINT FK_fixed_assignments_employee       FOREIGN KEY (employee_id)      REFERENCES dbo.employees(id),
    CONSTRAINT FK_fixed_assignments_created_by     FOREIGN KEY (created_by_id)    REFERENCES dbo.employees(id),
    CONSTRAINT FK_fixed_assignments_revoked_by     FOREIGN KEY (revoked_by_id)    REFERENCES dbo.employees(id)
);
GO

-- Una plaza no puede estar asignada a dos empleados el mismo dia (entre filas activas).
CREATE UNIQUE INDEX UX_fixed_assignments_space_day_active
    ON dbo.fixed_assignments(parking_space_id, day_of_week) WHERE active = 1;
GO

-- Un empleado no puede tener dos asignaciones fijas el mismo dia (entre filas activas).
CREATE UNIQUE INDEX UX_fixed_assignments_employee_day_active
    ON dbo.fixed_assignments(employee_id, day_of_week) WHERE active = 1;
GO

-- Indice de soporte: asignaciones activas de un empleado (consulta y revocacion).
CREATE INDEX IX_fixed_assignments_employee_id_active
    ON dbo.fixed_assignments(employee_id, active);
GO
