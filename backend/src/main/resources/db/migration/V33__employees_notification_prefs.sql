-- =====================================================================
-- V33 — Preferencias de notificación POR EMPLEADO (change push-notifications).
-- Segunda capa sobre los flags globales: cada empleado puede tener el email y/o
-- el push desactivados (editable por el ADMIN en el formulario de empleado).
-- Entrega efectiva por canal = flag global AND flag del empleado (+ suscripción
-- para push). Por defecto 1 (activos) → retrocompatible.
-- =====================================================================
IF COL_LENGTH('dbo.employees', 'email_notifications_enabled') IS NULL
    ALTER TABLE dbo.employees ADD email_notifications_enabled BIT NOT NULL
        CONSTRAINT DF_employees_email_notif DEFAULT 1;
GO
IF COL_LENGTH('dbo.employees', 'push_notifications_enabled') IS NULL
    ALTER TABLE dbo.employees ADD push_notifications_enabled BIT NOT NULL
        CONSTRAINT DF_employees_push_notif DEFAULT 1;
GO
