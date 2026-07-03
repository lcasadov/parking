-- =====================================================================
-- V6 — Tabla parking_spaces (change funcional init-parking-spaces).
-- Recurso reservable vivo del nucleo de parking: un label unico y humano
-- (p. ej. P-08) con estado activa/inactiva. Esquema segun docs/data-model.md
-- §3.2. El indice UNIQUE UX_parking_spaces_label garantiza la unicidad del
-- label frente a concurrencia (segunda insercion -> 409 en la capa de app).
--
-- Nota de versionado: V5 esta ocupada por el seed de desarrollo
-- (db/seed/dev/V5__seed_dev_admin.sql, solo perfil des); esta migracion de
-- ESQUEMA usa V6 para no colisionar con esa version en el perfil des.
-- =====================================================================

CREATE TABLE dbo.parking_spaces (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    label       NVARCHAR(20) NOT NULL,
    active      BIT NOT NULL CONSTRAINT DF_parking_spaces_active DEFAULT 1,
    created_at  DATETIME2(3) NOT NULL CONSTRAINT DF_parking_spaces_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_parking_spaces PRIMARY KEY (id)
);
GO

CREATE UNIQUE INDEX UX_parking_spaces_label ON dbo.parking_spaces(label);
GO
