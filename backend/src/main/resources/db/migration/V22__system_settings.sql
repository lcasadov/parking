-- =====================================================================
-- V22 — Ajuste global de sistema (change request-auto-assignment).
-- Tabla `system_settings` de FILA UNICA (patron singleton): un unico
-- parametro global `approval_mode` (MANUAL | AUTOMATIC) editable por el
-- ADMIN en caliente, con trazabilidad de quien lo cambio y cuando.
--
-- La unicidad de la fila la garantiza la PK constante + CHECK (id = 1):
-- no puede existir ninguna otra fila. El seed inserta la fila con el modo
-- por defecto MANUAL (retrocompatible: el comportamiento actual del alta de
-- solicitudes no cambia hasta que el ADMIN conmute a AUTOMATIC).
--
-- Nota de versionado: V16-V18 estan reservadas por seeds de desarrollo
-- (db/seed/dev, solo perfil des) y V19-V21 por migraciones de esquema ya
-- mergeadas (planta, categoria, rol de agencia); esta migracion usa V22
-- para no colisionar con ninguna de ellas.
-- =====================================================================

CREATE TABLE dbo.system_settings (
    id            TINYINT      NOT NULL CONSTRAINT PK_system_settings PRIMARY KEY,
    approval_mode VARCHAR(10)  NOT NULL CONSTRAINT DF_system_settings_mode DEFAULT 'MANUAL',
    updated_by_id BIGINT       NULL,
    updated_at    DATETIME2(3) NULL,
    CONSTRAINT CK_system_settings_singleton CHECK (id = 1),
    CONSTRAINT CK_system_settings_mode CHECK (approval_mode IN ('MANUAL','AUTOMATIC')),
    CONSTRAINT FK_system_settings_updated_by
        FOREIGN KEY (updated_by_id) REFERENCES dbo.employees(id)
);
GO

-- Seed de la fila unica con el modo por defecto MANUAL (retrocompatible).
INSERT INTO dbo.system_settings (id, approval_mode) VALUES (1, 'MANUAL');
GO
