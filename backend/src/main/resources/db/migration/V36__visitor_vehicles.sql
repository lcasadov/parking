-- =====================================================================
-- V36 — Tabla visitor_vehicles (change funcional visitor-vehicles).
-- Varios vehiculos por visitante (1:N): marca/modelo/color opcionales, matricula
-- OBLIGATORIA. Solo el ADMIN los gestiona. El borrado del visitante elimina en
-- cascada sus vehiculos (ON DELETE CASCADE, mismo estilo que V35__employee_vehicles).
--
-- La matricula es unica POR VISITANTE (un mismo visitante no repite matricula):
--   * UX_visitor_vehicles_visitor_plate: (visitor_id, license_plate) unico ->
--     el duplicado (secuencial o carrera concurrente) viola el indice ->
--     DataIntegrityViolation -> 409 (red dura frente a concurrencia). La unicidad
--     GLOBAL de matricula se descarta (coches compartidos entre visitantes posible).
-- La matricula se normaliza (trim + mayusculas) en la capa de aplicacion antes de
-- persistir/comparar, para que "1234abc" y "1234ABC" no se dupliquen.
-- =====================================================================

CREATE TABLE dbo.visitor_vehicles (
    id             BIGINT IDENTITY(1,1) NOT NULL,
    visitor_id     BIGINT NOT NULL,
    license_plate  NVARCHAR(15) NOT NULL,
    brand          NVARCHAR(60) NULL,
    model          NVARCHAR(60) NULL,
    color          NVARCHAR(30) NULL,
    created_at     DATETIME2(3) NOT NULL
        CONSTRAINT DF_visitor_vehicles_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_visitor_vehicles PRIMARY KEY (id),
    CONSTRAINT FK_visitor_vehicles_visitor FOREIGN KEY (visitor_id)
        REFERENCES dbo.visitors(id) ON DELETE CASCADE
);
GO

-- Matricula unica por visitante (red de concurrencia frente a duplicados del mismo visitante).
CREATE UNIQUE INDEX UX_visitor_vehicles_visitor_plate
    ON dbo.visitor_vehicles(visitor_id, license_plate);
GO

-- Lookup del listado de vehiculos por visitante.
CREATE INDEX IX_visitor_vehicles_visitor ON dbo.visitor_vehicles(visitor_id);
GO
