-- V25: reservas de visitante genéricas (plaza o puesto).
-- Generaliza `parking_space_id` -> `resource_type` + `resource_id` (mismo patrón de recurso
-- genérico que V12 aplicó a `requests`), para que un visitante pueda reservar también un
-- PUESTO y que esa reserva cuente en la disponibilidad/ocupación del recurso correspondiente.
-- `resource_id` es polimórfico (plaza o puesto), por lo que no lleva FK (igual que `requests`).

ALTER TABLE visitor_reservations
    ADD resource_type VARCHAR(16) NOT NULL
        CONSTRAINT DF_visitor_reservations_resource_type DEFAULT 'PARKING';
GO

ALTER TABLE visitor_reservations ADD resource_id BIGINT NULL;
GO

UPDATE visitor_reservations SET resource_id = parking_space_id;
GO

ALTER TABLE visitor_reservations ALTER COLUMN resource_id BIGINT NOT NULL;
GO

-- La plaza deja de ser específica: se retira su FK, su índice único y la columna.
ALTER TABLE visitor_reservations DROP CONSTRAINT FK_visitor_reservations_parking_spaces;
GO

DROP INDEX UX_visitor_reservations_space_date ON visitor_reservations;
GO

ALTER TABLE visitor_reservations DROP COLUMN parking_space_id;
GO

-- Unicidad recurso+fecha: un recurso (plaza o puesto) no se reserva dos veces el mismo día.
CREATE UNIQUE INDEX UX_visitor_reservations_resource_date
    ON visitor_reservations (resource_type, resource_id, reservation_date);
GO
