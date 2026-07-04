-- =====================================================================
-- V10 — Tablas visitors y visitor_reservations (change funcional init-visitors).
-- Ficha reutilizable de un visitante externo (sin cuenta ni acceso a la app) y su
-- reserva puntual de plaza para una fecha concreta, gestionadas solo por el ADMIN.
-- Esquema segun docs/data-model.md §3.6/§3.7.
--
-- national_id (DNI/pasaporte) es la clave natural unica de la ficha para reusarla
-- entre visitas:
--   * UX_visitors_national_id: una ficha por national_id. El duplicado secuencial y la
--     carrera de dos altas concurrentes con el mismo national_id violan el indice ->
--     DataIntegrityViolation -> 409 (red dura frente a concurrencia).
--
-- Una visitor_reservation ocupa la plaza para reservation_date en el calculo de
-- disponibilidad (cuenta como plaza NO disponible; solo plazas, nunca puestos). La
-- anulacion es un BORRADO FISICO de la fila futura (historico purgable a 2 anos por
-- audit-retention B10), de modo que una fila anulada no bloquea una nueva reserva del
-- mismo recurso/fecha. Por eso la unicidad recurso+fecha se garantiza con un indice
-- UNICO SIMPLE (no filtrado), analogo a UX_releases_space_date:
--   * UX_visitor_reservations_space_date: una sola reserva de visitante por plaza y
--     fecha. La 2a reserva concurrente sobre la misma plaza/fecha viola el indice ->
--     DataIntegrityViolation -> 409. Sirve tambien el lookup de disponibilidad
--     (parking_space_id, reservation_date). Consolidara availability-calendar (B7).
-- =====================================================================

CREATE TABLE dbo.visitors (
    id             BIGINT IDENTITY(1,1) NOT NULL,
    first_name     NVARCHAR(100) NOT NULL,
    last_name      NVARCHAR(150) NOT NULL,
    national_id    NVARCHAR(20) NOT NULL,
    license_plate  NVARCHAR(15) NULL,
    company        NVARCHAR(150) NULL,
    usual_reason   NVARCHAR(255) NULL,
    created_by_id  BIGINT NOT NULL,
    created_at     DATETIME2(3) NOT NULL CONSTRAINT DF_visitors_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_visitors PRIMARY KEY (id),
    CONSTRAINT FK_visitors_created_by FOREIGN KEY (created_by_id) REFERENCES dbo.employees(id)
);
GO

-- Unicidad de la clave natural national_id (reuso de ficha entre visitas; red de concurrencia).
CREATE UNIQUE INDEX UX_visitors_national_id ON dbo.visitors(national_id);
GO

CREATE TABLE dbo.visitor_reservations (
    id                BIGINT IDENTITY(1,1) NOT NULL,
    visitor_id        BIGINT NOT NULL,
    parking_space_id  BIGINT NOT NULL,
    reservation_date  DATE NOT NULL,
    notes             NVARCHAR(500) NULL,
    created_by_id     BIGINT NOT NULL,
    created_at        DATETIME2(3) NOT NULL CONSTRAINT DF_visitor_reservations_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_visitor_reservations PRIMARY KEY (id),
    CONSTRAINT FK_visitor_reservations_visitors       FOREIGN KEY (visitor_id)       REFERENCES dbo.visitors(id),
    CONSTRAINT FK_visitor_reservations_parking_spaces FOREIGN KEY (parking_space_id) REFERENCES dbo.parking_spaces(id),
    CONSTRAINT FK_visitor_reservations_created_by     FOREIGN KEY (created_by_id)    REFERENCES dbo.employees(id)
);
GO

-- Unicidad plaza+fecha (red de concurrencia): una sola reserva de visitante por plaza y
-- fecha. Sirve tambien el lookup de disponibilidad (parking_space_id, reservation_date).
CREATE UNIQUE INDEX UX_visitor_reservations_space_date
    ON dbo.visitor_reservations(parking_space_id, reservation_date);
GO

-- Listado de reservas de un visitante (WHERE visitor_id=?).
CREATE INDEX IX_visitor_reservations_visitor_id
    ON dbo.visitor_reservations(visitor_id);
GO
