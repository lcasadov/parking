-- =====================================================================
-- V28 — Direccion postal del parking (change reservas-employee-admin-reassign).
-- Anade la columna `parking_address` (texto NULLABLE, sin default) a la
-- tabla de fila unica `system_settings`. La configura el ADMIN en Ajustes
-- y la usa el empleado en "Mi Semana" para el boton "Ir al parking", que
-- abre Google Maps con esa direccion. NULL = sin configurar (el boton no se
-- muestra o queda deshabilitado en el frontend).
--
-- Es aditiva y retrocompatible: la fila unica existente queda con
-- parking_address = NULL hasta que el ADMIN la establezca.
-- =====================================================================

ALTER TABLE dbo.system_settings
    ADD parking_address VARCHAR(500) NULL;
GO
