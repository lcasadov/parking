# Design — release-occupied-resource

## Context
Dos mecanismos de liberación coexisten: `Release` (asignaciones fijas, recurrentes) y la
cancelación de solicitud aprobada (`RequestService.cancel`, libera el recurso de una fecha).
Hoy solo el empleado puede cancelar su solicitud; el admin no tiene forma de liberar un recurso
ocupado por la solicitud aprobada de otro empleado.

## Decisions
- **Liberar un recurso ocupado por solicitud = cancelar la solicitud** (no se crea `Release`).
  Consistente con la semántica existente de `cancel-approved-request`.
- **Nueva cancelación administrativa**: endpoint `POST /requests/{id}/admin-cancel` (solo ADMIN),
  con motivo obligatorio, para una solicitud `APPROVED` de fecha futura; transiciona a `CANCELLED`,
  libera el recurso y audita la acción (reutiliza la lógica de liberación por cancelación del empleado).
  La solicitud `PENDING` de otro se resuelve con `reject` (ya existe); admin-cancel es para APPROVED.
- **Frontend**: la acción "Liberar" elige el mecanismo según el origen del recurso del día
  (fija → Release; solicitud → cancelar). Reutiliza flujos existentes; el admin usa el nuevo endpoint.

## Risks / Migration
- BOLA/RBAC: `admin-cancel` solo `ADMIN`; verificación de estado (APPROVED + futura) en el servicio.
- Idempotencia/concurrencia: si la solicitud ya no está APPROVED (cancelada/pasada), responde 409/400.
- Sin migración de datos.
