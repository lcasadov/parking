# Design: init-requests

## Context
La solicitud puntual es el flujo más concurrido: varios empleados compiten por
plazas escasas y varios administradores resuelven en paralelo. El diseño debe
ser determinista (FIFO informativo), seguro (pertenencia a nivel de objeto) y
robusto frente a concurrencia (dos admins no pueden asignar la misma plaza la
misma fecha). Arquitectura hexagonal: los casos de uso
(`CreateRequestUseCase`, `ApproveRequestUseCase`, ...) no dependen de Spring.

## Goals
- Crear solicitudes solo dentro de la ventana hoy..hoy+14 y con unicidad
  `PENDING` por empleado/fecha.
- Resolver (aprobar/rechazar) de forma trazable, atribuible y notificable.
- Garantizar que la aprobación nunca asigne una plaza ya ocupada esa fecha,
  incluso bajo concurrencia entre administradores.

## Decisions
- **Ventana temporal en el dominio**: la validación hoy..hoy+14 vive en el caso
  de uso con un `ClockPort` inyectable (testeable), no en la base de datos. Error
  `OUTSIDE_REQUEST_WINDOW` (400) con `fields = requested_date`. Extremos inclusive.
- **Unicidad `PENDING`**: garantizada por el índice único filtrado
  `UX_requests_employee_date_pending` (`WHERE status = 'PENDING'`); la violación
  se traduce a 409 `REQUEST_ALREADY_PENDING`. No se confía solo en una lectura
  previa (race condition), el índice es la autoridad.
- **Estado inicial**: `PENDING` con `parking_space_id = NULL`; la plaza se asigna
  únicamente al aprobar.
- **Máquina de estados explícita**: transiciones permitidas solo desde `PENDING`;
  cualquier intento desde estado terminal → 409.
- **Aprobación atómica**: la comprobación de disponibilidad y la asignación de
  plaza ocurren en la misma transacción; el conflicto de concurrencia (dos admins,
  misma plaza/fecha) se detecta por restricción/índice de disponibilidad → 409,
  sin lectura-y-luego-escritura ingenua.
- **`approval_note` en el email**: la nota libre del administrador se persiste y
  viaja en la notificación de aprobación (mockup `05-modal-aprobar-solicitud`).
- **Catálogo de rechazo**: `rejection_reason_code` ∈ `{NO_AVAILABILITY,
  OUTSIDE_POLICY, OTHER}`; `rejection_reason` (texto libre) obligatorio (≥5) solo
  si `OTHER`. Toda solicitud rechazada lleva código.
- **FIFO**: `listPendingRequests` ordena por `created_at ASC` (orden informativo,
  no reserva), apoyado en `IX_requests_status_created_at`.
- **BOLA**: `listMyRequests` y `cancelRequest` verifican pertenencia a nivel de
  objeto además del rol.

## Risks
- **Concurrencia de aprobación** → mitigada con validación de disponibilidad
  dentro de la transacción y restricción que rechaza la segunda asignación (409).
- **Race en creación duplicada** → mitigada por el índice único filtrado, no por
  comprobación previa.
- **Catálogo de motivos provisional** → `rejection_reason_code` es un conjunto
  inicial pendiente de confirmar con negocio _[verificar con docs/data-model.md]_.
- **Fallo de notificación** → no debe revertir la resolución; el reintento lo
  gestiona `notifications` (`AFTER_COMMIT`).

## Migration Plan
- Flyway: tabla `requests` con `CK_requests_status`, `CK_requests_rejection_reason_code`,
  FKs a `employees` y `parking_spaces`, índice único filtrado
  `UX_requests_employee_date_pending` e índices `IX_requests_status_created_at` e
  `IX_requests_employee_id_requested_date` (ver `docs/data-model.md` §3.5 y §"Índices").
- Sin migración de datos (capability nueva).
