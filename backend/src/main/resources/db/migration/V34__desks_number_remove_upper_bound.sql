-- =====================================================================
-- V34 — Quita el tope superior (65) del numero de puesto.
-- El numero de puesto deja de estar acotado a 1-65: ahora solo debe ser un
-- entero positivo (>= 1) y unico (indice UX_desks_number). Se reemplaza el
-- CHECK antiguo (CK_desks_number CHECK number BETWEEN 1 AND 65) por uno que
-- solo exige number >= 1. Estilo identico a V21__agency_role.sql (DROP + ADD).
-- Alineado con la validacion del DTO DeskCreateRequest (@Min(1), sin @Max).
-- =====================================================================

ALTER TABLE dbo.desks DROP CONSTRAINT CK_desks_number;
GO

ALTER TABLE dbo.desks
    ADD CONSTRAINT CK_desks_number CHECK (number >= 1);
GO
