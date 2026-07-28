-- V26: reenvio de aviso de una solicitud pendiente estancada (change request-resend-notice).
-- Registra el instante del ultimo reenvio del empleado al ADMIN, de modo que el servicio pueda
-- exigir un periodo minimo (24h) entre la creacion (o el ultimo reenvio) y el siguiente. NULL
-- mientras la solicitud nunca se haya reenviado (comportamiento por defecto retrocompatible).

ALTER TABLE dbo.requests ADD last_reminded_at DATETIME2(3) NULL;
GO
