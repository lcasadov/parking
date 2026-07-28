-- V27: lista de espera de solicitudes (change waitlist-requests).
-- Marca si una solicitud PENDING esta "en lista de espera" para su (fecha, tipo de recurso):
-- en modo AUTOMATICO nace asi cuando el empleado opta por esperar en vez de recibir el 409
-- NO_AVAILABILITY; en modo MANUAL se computa al crear (no habia disponibilidad ese dia/tipo).
-- Retro-compatible, sin backfill: las solicitudes existentes quedan como no en espera (0).

ALTER TABLE dbo.requests ADD waitlisted BIT NOT NULL CONSTRAINT DF_requests_waitlisted DEFAULT 0;
GO
