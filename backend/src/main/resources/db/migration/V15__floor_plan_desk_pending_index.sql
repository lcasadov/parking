-- =====================================================================
-- V15 — Indice unico de solicitud de puesto pendiente por puesto y fecha
-- (capability init-floor-plan).
--
-- El plano interactivo permite solicitar un puesto pinchandolo: a diferencia
-- de la solicitud generica de /requests (que nace con resource_id = NULL y solo
-- se asigna al aprobar), la solicitud desde el plano fija el puesto pinchado
-- desde la creacion (resource_id = deskId, resource_type = 'DESK', PENDING) para
-- que ese puesto quede ocupado. Asi un segundo empleado que pinche el mismo
-- puesto libre para la misma fecha recibe un conflicto (409) y el plano lo pinta
-- como REQUESTED.
--
-- Concurrencia (segunda capa de la unicidad, design init-floor-plan §Decisions):
-- indice UNICO FILTRADO de SQL Server. Solo aplica a solicitudes PENDING de tipo
-- DESK con recurso concreto (resource_id IS NOT NULL): NO afecta a las PARKING ni
-- a las DESK genericas de /requests (resource_id NULL), que quedan fuera del
-- filtro. La 2a insercion concurrente sobre el mismo (resource_id, requested_date)
-- viola el indice -> DataIntegrityViolation -> 409 SPACE_NOT_AVAILABLE
-- (GlobalExceptionHandler), red dura frente a la carrera entre empleados.
--
-- Nota de versionado: V14 la ocupa el seed de desarrollo (db/seed/dev, solo perfil
-- des); en pre/pro (solo db/migration) hay un hueco V14 -> V15 permitido por Flyway.
-- =====================================================================

CREATE UNIQUE INDEX UX_requests_desk_date_pending
    ON dbo.requests(resource_id, requested_date)
    WHERE status = 'PENDING' AND resource_type = 'DESK' AND resource_id IS NOT NULL;
GO
