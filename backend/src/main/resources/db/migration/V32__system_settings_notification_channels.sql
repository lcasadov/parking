-- =====================================================================
-- V32 — Interruptores GLOBALES de canal de notificación (change push-notifications).
-- Dos flags independientes en la fila única de system_settings: si cada canal
-- (email / push) envía a nivel global. Por defecto 1 (activos) → retrocompatible
-- con el comportamiento actual (email activo).
-- =====================================================================
IF COL_LENGTH('dbo.system_settings', 'email_notifications_enabled') IS NULL
    ALTER TABLE dbo.system_settings ADD email_notifications_enabled BIT NOT NULL
        CONSTRAINT DF_system_settings_email_notif DEFAULT 1;
GO
IF COL_LENGTH('dbo.system_settings', 'push_notifications_enabled') IS NULL
    ALTER TABLE dbo.system_settings ADD push_notifications_enabled BIT NOT NULL
        CONSTRAINT DF_system_settings_push_notif DEFAULT 1;
GO
