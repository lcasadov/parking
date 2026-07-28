-- =====================================================================
-- V30 — Coordenadas del parking (lat/lng) en system_settings.
-- Change admin-improvements (tarea 18): el admin fija el punto exacto del parking en
-- un mapa (Mapbox); el boton "Ir al parking" del empleado abre la navegacion a esas
-- coordenadas. Nullable: si no se han fijado, se cae a la direccion postal.
-- DECIMAL(9,6): precision ~0.1 m; rango suficiente para lat (-90..90) y lng (-180..180).
-- =====================================================================
IF COL_LENGTH('dbo.system_settings', 'parking_lat') IS NULL
    ALTER TABLE dbo.system_settings ADD parking_lat DECIMAL(9,6) NULL;
GO
IF COL_LENGTH('dbo.system_settings', 'parking_lng') IS NULL
    ALTER TABLE dbo.system_settings ADD parking_lng DECIMAL(9,6) NULL;
GO
