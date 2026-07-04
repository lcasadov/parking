-- =====================================================================
-- V9 — Tabla releases (change funcional init-releases).
-- Liberacion de un recurso con asignacion fija para una fecha concreta, dejandolo
-- disponible ese dia en el calculo de disponibilidad. Esquema segun
-- docs/data-model.md §3.4.
--
-- Distingue employee_id (dueno cuyo recurso se libera) de released_by_id (quien
-- ejecuta): en VOLUNTARY coinciden; en ADMINISTRATIVE el ejecutor es un ADMIN y
-- reason es obligatorio (regla de servicio; el esquema permite NULL para VOLUNTARY).
--
-- La cancelacion es un BORRADO FISICO de la fila futura (design §Decisions:
-- releases es historico purgable, sin estado de baja logica). Por eso la unicidad
-- recurso+fecha se garantiza con un indice UNICO SIMPLE (no filtrado):
--   * UX_releases_space_date: un recurso solo puede tener UNA liberacion por fecha.
--     El duplicado secuencial y la carrera de dos liberaciones concurrentes sobre el
--     mismo recurso/fecha violan el indice -> DataIntegrityViolation -> 409 (red dura
--     frente a concurrencia). Una fila cancelada se borra fisicamente, de modo que
--     no bloquea una nueva liberacion del mismo recurso/fecha.
-- Este indice unico SUPERSEDE al IX_releases_parking_space_id_date (no unico) que
-- data-model.md §Indexes proponia para el lookup de disponibilidad: el indice unico
-- sirve tambien esa consulta (parking_space_id, release_date).
-- =====================================================================

CREATE TABLE dbo.releases (
    id                BIGINT IDENTITY(1,1) NOT NULL,
    parking_space_id  BIGINT NOT NULL,
    employee_id       BIGINT NOT NULL,
    release_date      DATE NOT NULL,
    type              VARCHAR(15) NOT NULL,
    reason            NVARCHAR(500) NULL,
    released_by_id    BIGINT NOT NULL,
    created_at        DATETIME2(3) NOT NULL CONSTRAINT DF_releases_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_releases PRIMARY KEY (id),
    CONSTRAINT CK_releases_type CHECK (type IN ('VOLUNTARY','ADMINISTRATIVE')),
    CONSTRAINT FK_releases_parking_spaces FOREIGN KEY (parking_space_id) REFERENCES dbo.parking_spaces(id),
    CONSTRAINT FK_releases_employee       FOREIGN KEY (employee_id)      REFERENCES dbo.employees(id),
    CONSTRAINT FK_releases_released_by     FOREIGN KEY (released_by_id)   REFERENCES dbo.employees(id)
);
GO

-- Unicidad recurso+fecha (red de concurrencia): una sola liberacion por recurso y fecha.
-- Sirve tambien el lookup de disponibilidad (parking_space_id, release_date).
CREATE UNIQUE INDEX UX_releases_space_date
    ON dbo.releases(parking_space_id, release_date);
GO

-- Listado "mis liberaciones" del empleado (WHERE employee_id=? ORDER BY id).
CREATE INDEX IX_releases_employee_id
    ON dbo.releases(employee_id);
GO
