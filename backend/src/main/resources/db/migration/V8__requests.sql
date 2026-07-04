-- =====================================================================
-- V8 — Tabla requests (change funcional init-requests).
-- Solicitud puntual de un empleado para una plaza en una fecha concreta,
-- con ciclo de vida de aprobacion/rechazo. Esquema segun docs/data-model.md
-- §3.5.
--
-- Maquina de estados: PENDING (parking_space_id NULL) -> APPROVED (plaza +
-- resolutor + resolved_at [+ approval_note]) | REJECTED (rejection_reason_code
-- + resolutor, texto libre opcional) | CANCELLED (por el empleado en PENDING).
--
-- Concurrencia resuelta en la BD con DOS indices unicos FILTRADOS de SQL Server
-- (CREATE UNIQUE INDEX ... WHERE status = '...'):
--   * UX_requests_employee_date_pending: una sola solicitud PENDING por empleado
--     y fecha; las filas REJECTED/CANCELLED no bloquean nuevas solicitudes.
--   * UX_requests_space_date_approved: una plaza solo puede estar APPROVED una vez
--     por fecha; la 2a aprobacion concurrente sobre la misma plaza/fecha viola el
--     indice -> DataIntegrityViolation -> 409 (red dura frente a concurrencia entre
--     administradores). Consolidara availability-calendar (B7) mas adelante.
-- =====================================================================

CREATE TABLE dbo.requests (
    id                    BIGINT IDENTITY(1,1) NOT NULL,
    employee_id           BIGINT NOT NULL,
    requested_date        DATE NOT NULL,
    status                VARCHAR(10) NOT NULL CONSTRAINT DF_requests_status DEFAULT 'PENDING',
    parking_space_id      BIGINT NULL,
    approval_note         NVARCHAR(500) NULL,
    rejection_reason_code VARCHAR(40) NULL,
    rejection_reason      NVARCHAR(500) NULL,
    resolved_by_id        BIGINT NULL,
    resolved_at           DATETIME2(3) NULL,
    created_at            DATETIME2(3) NOT NULL CONSTRAINT DF_requests_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_requests PRIMARY KEY (id),
    CONSTRAINT CK_requests_status CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    CONSTRAINT CK_requests_rejection_reason_code
        CHECK (rejection_reason_code IS NULL OR rejection_reason_code IN ('NO_AVAILABILITY','OUTSIDE_POLICY','OTHER')),
    CONSTRAINT FK_requests_employee       FOREIGN KEY (employee_id)      REFERENCES dbo.employees(id),
    CONSTRAINT FK_requests_parking_spaces FOREIGN KEY (parking_space_id) REFERENCES dbo.parking_spaces(id),
    CONSTRAINT FK_requests_resolved_by    FOREIGN KEY (resolved_by_id)   REFERENCES dbo.employees(id)
);
GO

-- Una sola solicitud PENDING por empleado y fecha (unicidad entre filas pendientes).
CREATE UNIQUE INDEX UX_requests_employee_date_pending
    ON dbo.requests(employee_id, requested_date) WHERE status = 'PENDING';
GO

-- Una plaza solo puede estar APPROVED una vez por fecha (red de concurrencia).
CREATE UNIQUE INDEX UX_requests_space_date_approved
    ON dbo.requests(parking_space_id, requested_date) WHERE status = 'APPROVED';
GO

-- Listado admin de pendientes en orden FIFO (WHERE status='PENDING' ORDER BY created_at ASC).
CREATE INDEX IX_requests_status_created_at
    ON dbo.requests(status, created_at);
GO

-- Listado "mis solicitudes" del empleado (WHERE employee_id=? ORDER BY requested_date).
CREATE INDEX IX_requests_employee_id_requested_date
    ON dbo.requests(employee_id, requested_date);
GO
