-- =====================================================================
-- V35 — Tabla employee_vehicles (change funcional employee-vehicles).
-- Varios vehiculos por empleado (1:N): marca/modelo/color opcionales, matricula
-- OBLIGATORIA. Solo el ADMIN los gestiona. El borrado del empleado elimina en
-- cascada sus vehiculos (ON DELETE CASCADE, mismo estilo que V31__push_subscription).
--
-- La matricula es unica POR EMPLEADO (un mismo empleado no repite matricula):
--   * UX_employee_vehicles_employee_plate: (employee_id, license_plate) unico ->
--     el duplicado (secuencial o carrera concurrente) viola el indice ->
--     DataIntegrityViolation -> 409 (red dura frente a concurrencia). La unicidad
--     GLOBAL de matricula se descarta (coches compartidos entre empleados posible).
-- La matricula se normaliza (trim + mayusculas) en la capa de aplicacion antes de
-- persistir/comparar, para que "1234abc" y "1234ABC" no se dupliquen.
-- =====================================================================

CREATE TABLE dbo.employee_vehicles (
    id             BIGINT IDENTITY(1,1) NOT NULL,
    employee_id    BIGINT NOT NULL,
    license_plate  NVARCHAR(15) NOT NULL,
    brand          NVARCHAR(60) NULL,
    model          NVARCHAR(60) NULL,
    color          NVARCHAR(30) NULL,
    created_at     DATETIME2(3) NOT NULL
        CONSTRAINT DF_employee_vehicles_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_employee_vehicles PRIMARY KEY (id),
    CONSTRAINT FK_employee_vehicles_employee FOREIGN KEY (employee_id)
        REFERENCES dbo.employees(id) ON DELETE CASCADE
);
GO

-- Matricula unica por empleado (red de concurrencia frente a duplicados del mismo empleado).
CREATE UNIQUE INDEX UX_employee_vehicles_employee_plate
    ON dbo.employee_vehicles(employee_id, license_plate);
GO

-- Lookup del listado de vehiculos por empleado.
CREATE INDEX IX_employee_vehicles_employee ON dbo.employee_vehicles(employee_id);
GO
