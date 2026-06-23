-- =====================================================================
-- 00_create_database.sql — crea la base de datos `parking` en SQL Server.
-- Ejecutado automáticamente por el contenedor en el primer arranque
-- (montado en /docker-entrypoint-initdb.d). Idempotente.
-- El esquema (tablas) lo gestiona Flyway desde el backend, NO este script.
-- =====================================================================
IF DB_ID(N'parking') IS NULL
BEGIN
    CREATE DATABASE parking;
END;
GO
