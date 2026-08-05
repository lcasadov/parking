-- =====================================================================
-- V38 — Estado previo del vehículo, para poder restaurar un "pendiente de
-- borrado" (change employee-vehicle-self-service, Fase 2).
--
-- Cuando un empleado pide borrar un vehículo que estaba IN_PROGRESS o APPROVED,
-- no se borra: pasa a PENDING_DELETION y se guarda su estado previo aquí, de
-- modo que el administrador pueda restaurarlo a como estaba. Se limpia (NULL)
-- en cualquier otra transición.
-- =====================================================================

ALTER TABLE dbo.employee_vehicles
    ADD previous_status NVARCHAR(20) NULL;
GO
