-- =====================================================================
-- V13 — Tabla desks (capability init-desks).
--
-- Segundo tipo de recurso reservable (ResourceType.DESK). Hermana de
-- parking_spaces (no herencia de tabla unica): columnas propias
-- (number, category, coord_x, coord_y) sin nulos en parking_spaces.
-- Esquema segun docs/data-model.md §"Future desks" y openspec/changes/
-- init-desks/design.md.
--
-- Reglas de BD:
--   * number CHECK 1-65 + indice UNIQUE UX_desks_number: el conjunto
--     reservable es fijo y numerado (no se excede ni se duplica).
--   * category CHECK IN ('STANDARD','EXECUTIVE'): distincion visual, no
--     altera reglas de reserva.
--   * coord_x/coord_y DECIMAL(5,2) CHECK 0-100: porcentaje del ancho/alto
--     de la imagen del plano (independiente de la resolucion).
--
-- REFERENCIA POLIMORFICA: tras generic-resource-refactor (V12) las tablas
-- fixed_assignments, requests y releases referencian resource_id +
-- resource_type; resource_id apunta a parking_spaces (PARKING) o a desks
-- (DESK). Una FK a una sola tabla es incompatible con una referencia
-- polimorfica, por lo que se SUELTAN las FK a parking_spaces que V12 habia
-- repuesto. La integridad referencial del recurso pasa a la capa de
-- aplicacion (ResourceResolverPort#exists) para ambos tipos. Los indices
-- unicos filtrados (keyed on resource_type) NO cambian: PARKING y DESK
-- siguen siendo independientes y la traduccion a 409 se mantiene.
-- =====================================================================

CREATE TABLE dbo.desks (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    number      INT NOT NULL,
    category    VARCHAR(15) NOT NULL CONSTRAINT DF_desks_category DEFAULT 'STANDARD',
    coord_x     DECIMAL(5,2) NOT NULL CONSTRAINT DF_desks_coord_x DEFAULT 50,
    coord_y     DECIMAL(5,2) NOT NULL CONSTRAINT DF_desks_coord_y DEFAULT 50,
    active      BIT NOT NULL CONSTRAINT DF_desks_active DEFAULT 1,
    created_at  DATETIME2(3) NOT NULL CONSTRAINT DF_desks_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_desks PRIMARY KEY (id),
    CONSTRAINT CK_desks_number CHECK (number BETWEEN 1 AND 65),
    CONSTRAINT CK_desks_category CHECK (category IN ('STANDARD','EXECUTIVE')),
    CONSTRAINT CK_desks_coord_x CHECK (coord_x BETWEEN 0 AND 100),
    CONSTRAINT CK_desks_coord_y CHECK (coord_y BETWEEN 0 AND 100)
);
GO

CREATE UNIQUE INDEX UX_desks_number ON dbo.desks(number);
GO

-- Referencia polimorfica: soltar las FK a parking_spaces (resource_id ahora
-- puede apuntar a desks). La existencia del recurso la garantiza la capa de
-- aplicacion via ResourceResolverPort para PARKING y DESK.
ALTER TABLE dbo.fixed_assignments DROP CONSTRAINT FK_fixed_assignments_parking_spaces;
GO
ALTER TABLE dbo.requests DROP CONSTRAINT FK_requests_parking_spaces;
GO
ALTER TABLE dbo.releases DROP CONSTRAINT FK_releases_parking_spaces;
GO
