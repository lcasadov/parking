-- =====================================================================
-- V29 — Reservas en fin de semana (change reservas-employee-admin-reassign).
-- Anade la columna `weekend_reservable` (BIT NOT NULL, default 0) a la
-- tabla de fila unica `system_settings`. Cuando es 0 (por defecto,
-- retrocompatible), la creacion de solicitudes (POST /requests) para un
-- sabado o domingo se rechaza; cuando es 1, se permite. La configura el
-- ADMIN en Ajustes. Es aditiva: la fila unica existente queda con
-- weekend_reservable = 0 (comportamiento por defecto: sin fines de semana).
-- =====================================================================

ALTER TABLE dbo.system_settings
    ADD weekend_reservable BIT NOT NULL
        CONSTRAINT DF_system_settings_weekend_reservable DEFAULT 0;
GO
