-- =====================================================================
-- V12 — Refactor a recurso generico (change init-generic-resource-refactor).
--
-- Generaliza la referencia reservable `parking_space_id` a `resource_id` +
-- `resource_type` (discriminador `ResourceType`: PARKING/DESK) en las tres
-- tablas del nucleo que la usan: fixed_assignments, requests y releases.
-- Prerrequisito de la capability `desks` (que anadira filas resource_type='DESK').
--
-- INVARIANTE DE NO-REGRESION: para el nucleo de parking el comportamiento
-- observable es IDENTICO. Todas las filas existentes se portan a
-- resource_id = parking_space_id y resource_type = 'PARKING'. Los indices
-- unicos filtrados CONSERVAN SU NOMBRE (para que la traduccion de violaciones
-- a 409 en GlobalExceptionHandler siga funcionando sin cambios) y su clausula
-- WHERE; solo cambian las columnas sobre las que se construyen, anadiendo el
-- discriminador de tipo para que un parking y un puesto no colisionen cuando
-- lleguen los puestos (id no es unico entre tablas de recurso).
--
-- VisitorReservation NO se generaliza (sigue en parking_space_id; out of scope).
--
-- ORDEN por tabla: (1) anadir columnas + backfill; (2) soltar indices unicos y
-- FK que referencian parking_space_id; (3) soltar la columna vieja; (4) fijar
-- NOT NULL donde procede; (5) recrear indices unicos sobre las nuevas columnas;
-- (6) reponer FK a parking_spaces (era PARKING) y CHECK del discriminador.
--
-- ROLLBACK inverso documentado al final de este fichero (Flyway Community no
-- ejecuta undo; el script se aplica manualmente si hay que revertir).
-- =====================================================================

-- ---------------------------------------------------------------------
-- fixed_assignments
-- ---------------------------------------------------------------------
ALTER TABLE dbo.fixed_assignments
    ADD resource_id   BIGINT NULL,
        resource_type VARCHAR(10) NOT NULL
            CONSTRAINT DF_fixed_assignments_resource_type DEFAULT 'PARKING';
GO
UPDATE dbo.fixed_assignments SET resource_id = parking_space_id;
GO
DROP INDEX UX_fixed_assignments_space_day_active    ON dbo.fixed_assignments;
DROP INDEX UX_fixed_assignments_employee_day_active ON dbo.fixed_assignments;
GO
ALTER TABLE dbo.fixed_assignments DROP CONSTRAINT FK_fixed_assignments_parking_spaces;
GO
ALTER TABLE dbo.fixed_assignments DROP COLUMN parking_space_id;
GO
ALTER TABLE dbo.fixed_assignments ALTER COLUMN resource_id BIGINT NOT NULL;
GO
-- Un recurso no puede estar asignado a dos empleados el mismo dia (filas activas).
CREATE UNIQUE INDEX UX_fixed_assignments_space_day_active
    ON dbo.fixed_assignments(resource_id, resource_type, day_of_week) WHERE active = 1;
GO
-- Un empleado no puede tener dos asignaciones fijas del mismo tipo el mismo dia
-- (filas activas). Con el discriminador, en el alcance ampliado un empleado podra
-- tener plaza y puesto el mismo dia; para el nucleo PARKING el efecto es identico.
CREATE UNIQUE INDEX UX_fixed_assignments_employee_day_active
    ON dbo.fixed_assignments(employee_id, resource_type, day_of_week) WHERE active = 1;
GO
ALTER TABLE dbo.fixed_assignments
    ADD CONSTRAINT FK_fixed_assignments_parking_spaces
        FOREIGN KEY (resource_id) REFERENCES dbo.parking_spaces(id);
GO
ALTER TABLE dbo.fixed_assignments
    ADD CONSTRAINT CK_fixed_assignments_resource_type
        CHECK (resource_type IN ('PARKING','DESK'));
GO

-- ---------------------------------------------------------------------
-- requests  (resource_id es NULL mientras PENDING; resource_type siempre presente)
-- ---------------------------------------------------------------------
ALTER TABLE dbo.requests
    ADD resource_id   BIGINT NULL,
        resource_type VARCHAR(10) NOT NULL
            CONSTRAINT DF_requests_resource_type DEFAULT 'PARKING';
GO
UPDATE dbo.requests SET resource_id = parking_space_id;
GO
DROP INDEX UX_requests_employee_date_pending ON dbo.requests;
DROP INDEX UX_requests_space_date_approved   ON dbo.requests;
GO
ALTER TABLE dbo.requests DROP CONSTRAINT FK_requests_parking_spaces;
GO
ALTER TABLE dbo.requests DROP COLUMN parking_space_id;
GO
-- Una sola solicitud PENDING por empleado, tipo y fecha (identico para PARKING).
CREATE UNIQUE INDEX UX_requests_employee_date_pending
    ON dbo.requests(employee_id, resource_type, requested_date) WHERE status = 'PENDING';
GO
-- Un recurso solo puede estar APPROVED una vez por fecha (red de concurrencia).
CREATE UNIQUE INDEX UX_requests_space_date_approved
    ON dbo.requests(resource_id, resource_type, requested_date) WHERE status = 'APPROVED';
GO
ALTER TABLE dbo.requests
    ADD CONSTRAINT FK_requests_parking_spaces
        FOREIGN KEY (resource_id) REFERENCES dbo.parking_spaces(id);
GO
ALTER TABLE dbo.requests
    ADD CONSTRAINT CK_requests_resource_type
        CHECK (resource_type IN ('PARKING','DESK'));
GO

-- ---------------------------------------------------------------------
-- releases
-- ---------------------------------------------------------------------
ALTER TABLE dbo.releases
    ADD resource_id   BIGINT NULL,
        resource_type VARCHAR(10) NOT NULL
            CONSTRAINT DF_releases_resource_type DEFAULT 'PARKING';
GO
UPDATE dbo.releases SET resource_id = parking_space_id;
GO
DROP INDEX UX_releases_space_date ON dbo.releases;
GO
ALTER TABLE dbo.releases DROP CONSTRAINT FK_releases_parking_spaces;
GO
ALTER TABLE dbo.releases DROP COLUMN parking_space_id;
GO
ALTER TABLE dbo.releases ALTER COLUMN resource_id BIGINT NOT NULL;
GO
-- Unicidad recurso+fecha (red de concurrencia): una sola liberacion por recurso y fecha.
CREATE UNIQUE INDEX UX_releases_space_date
    ON dbo.releases(resource_id, resource_type, release_date);
GO
ALTER TABLE dbo.releases
    ADD CONSTRAINT FK_releases_parking_spaces
        FOREIGN KEY (resource_id) REFERENCES dbo.parking_spaces(id);
GO
ALTER TABLE dbo.releases
    ADD CONSTRAINT CK_releases_resource_type
        CHECK (resource_type IN ('PARKING','DESK'));
GO

-- =====================================================================
-- ROLLBACK INVERSO (aplicar manualmente si hay que revertir V12).
-- Restaura parking_space_id desde resource_id donde resource_type='PARKING' y
-- deshace columnas, indices, FK y CHECK, dejando el esquema en el estado V9/V11.
-- =====================================================================
-- -- fixed_assignments
-- ALTER TABLE dbo.fixed_assignments ADD parking_space_id BIGINT NULL;
-- UPDATE dbo.fixed_assignments SET parking_space_id = resource_id WHERE resource_type = 'PARKING';
-- DROP INDEX UX_fixed_assignments_space_day_active    ON dbo.fixed_assignments;
-- DROP INDEX UX_fixed_assignments_employee_day_active ON dbo.fixed_assignments;
-- ALTER TABLE dbo.fixed_assignments DROP CONSTRAINT FK_fixed_assignments_parking_spaces;
-- ALTER TABLE dbo.fixed_assignments DROP CONSTRAINT CK_fixed_assignments_resource_type;
-- ALTER TABLE dbo.fixed_assignments DROP CONSTRAINT DF_fixed_assignments_resource_type;
-- ALTER TABLE dbo.fixed_assignments DROP COLUMN resource_id, resource_type;
-- ALTER TABLE dbo.fixed_assignments ALTER COLUMN parking_space_id BIGINT NOT NULL;
-- CREATE UNIQUE INDEX UX_fixed_assignments_space_day_active
--     ON dbo.fixed_assignments(parking_space_id, day_of_week) WHERE active = 1;
-- CREATE UNIQUE INDEX UX_fixed_assignments_employee_day_active
--     ON dbo.fixed_assignments(employee_id, day_of_week) WHERE active = 1;
-- ALTER TABLE dbo.fixed_assignments ADD CONSTRAINT FK_fixed_assignments_parking_spaces
--     FOREIGN KEY (parking_space_id) REFERENCES dbo.parking_spaces(id);
-- -- requests
-- ALTER TABLE dbo.requests ADD parking_space_id BIGINT NULL;
-- UPDATE dbo.requests SET parking_space_id = resource_id WHERE resource_type = 'PARKING';
-- DROP INDEX UX_requests_employee_date_pending ON dbo.requests;
-- DROP INDEX UX_requests_space_date_approved   ON dbo.requests;
-- ALTER TABLE dbo.requests DROP CONSTRAINT FK_requests_parking_spaces;
-- ALTER TABLE dbo.requests DROP CONSTRAINT CK_requests_resource_type;
-- ALTER TABLE dbo.requests DROP CONSTRAINT DF_requests_resource_type;
-- ALTER TABLE dbo.requests DROP COLUMN resource_id, resource_type;
-- CREATE UNIQUE INDEX UX_requests_employee_date_pending
--     ON dbo.requests(employee_id, requested_date) WHERE status = 'PENDING';
-- CREATE UNIQUE INDEX UX_requests_space_date_approved
--     ON dbo.requests(parking_space_id, requested_date) WHERE status = 'APPROVED';
-- ALTER TABLE dbo.requests ADD CONSTRAINT FK_requests_parking_spaces
--     FOREIGN KEY (parking_space_id) REFERENCES dbo.parking_spaces(id);
-- -- releases
-- ALTER TABLE dbo.releases ADD parking_space_id BIGINT NULL;
-- UPDATE dbo.releases SET parking_space_id = resource_id WHERE resource_type = 'PARKING';
-- DROP INDEX UX_releases_space_date ON dbo.releases;
-- ALTER TABLE dbo.releases DROP CONSTRAINT FK_releases_parking_spaces;
-- ALTER TABLE dbo.releases DROP CONSTRAINT CK_releases_resource_type;
-- ALTER TABLE dbo.releases DROP CONSTRAINT DF_releases_resource_type;
-- ALTER TABLE dbo.releases DROP COLUMN resource_id, resource_type;
-- ALTER TABLE dbo.releases ALTER COLUMN parking_space_id BIGINT NOT NULL;
-- CREATE UNIQUE INDEX UX_releases_space_date
--     ON dbo.releases(parking_space_id, release_date);
-- ALTER TABLE dbo.releases ADD CONSTRAINT FK_releases_parking_spaces
--     FOREIGN KEY (parking_space_id) REFERENCES dbo.parking_spaces(id);
