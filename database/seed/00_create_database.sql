-- =====================================================================
-- 00_create_database.sql — crea la base de datos `parking` en SQL Server.
-- Ejecutado automáticamente por el servicio `db-init` de docker-compose
-- (sqlcmd de la propia imagen mssql/server) cuando SQL Server está sano. Idempotente.
-- El esquema (tablas) lo gestiona Flyway desde el backend, NO este script.
-- =====================================================================
IF DB_ID(N'parking') IS NULL
BEGIN
    CREATE DATABASE parking;
END;
GO
