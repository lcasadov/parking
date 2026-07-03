# Design: init-fixed-assignments

## Context
La asignación fija es un vínculo indefinido empleado↔plaza por día de la semana
(`day_of_week` 1-7), vigente hasta que el `ADMIN` la revoca. El historial debe
preservarse (auditoría y trazabilidad), por lo que la revocación es lógica, nunca
física. La unicidad de negocio (una plaza por día, un empleado por día) solo aplica
a las filas vigentes, no a las ya revocadas. Arquitectura hexagonal: el dominio
(`SetFixedAssignmentsUseCase`, `RevokeFixedAssignmentUseCase`) no depende de Spring.

## Goals
- Gestión `ADMIN` de asignaciones fijas con validación estricta de `day_of_week`.
- Consulta segura para el `EMPLOYEE` limitada a las propias (BOLA).
- Unicidad plaza/día y empleado/día garantizada incluso bajo concurrencia.
- Revocación lógica sin pérdida de historial ni efecto sobre días pasados o aprobaciones previas.

## Decisions
- **Índices únicos filtrados** (`WHERE active = 1`) `UX_fixed_assignments_space_day_active` y `UX_fixed_assignments_employee_day_active`: imponen la unicidad solo entre filas activas, permitiendo que el historial revocado coexista. Justificación: un `UNIQUE` clásico prohibiría también duplicados históricos (ver `docs/data-model.md` §"Why filtered, not constraints").
- **Concurrencia gestionada por la base de datos**: la doble inserción simultánea se resuelve dejando que el índice único filtrado lance la violación; el servicio traduce esa excepción a 409 `{ error, message, fields, timestamp }`. No se confía en un check-then-insert en memoria.
- **Revocación lógica**: `active=false`, `revoked_at = ahora`, `revoked_by_id = session.employeeId`. La fila permanece para auditoría (`audit-retention` no purga asignaciones activas, y las históricas se purgan por su propia política a 2 años).
- **`PUT` como reemplazo del conjunto de días**: `setEmployeeFixedAssignments` recibe `{ parkingSpaceId, daysOfWeek[] }`; los días retirados respecto al estado previo se revocan lógicamente, los nuevos se crean. Operación idempotente cuando el conjunto no cambia.
- **Sin efecto retroactivo**: los cambios aplican de cara al futuro; no se tocan días pasados ni `Request APPROVED` ya emitidas (responsabilidad de `requests`/`releases`).
- **Reloj inyectable** (`ClockPort`) para `created_at`/`revoked_at` y para tests deterministas.

## Risks
- **Race condition** en alta simultánea de la misma plaza/día → mitigada delegando la unicidad al índice filtrado y traduciendo la violación a 409.
- **BOLA**: un `EMPLOYEE` accediendo a `{employeeId}` ajeno → mitigado con verificación `employeeId == session.employeeId` en el caso de uso, no solo por rol.
- **Inconsistencia histórica** si se borrasen filas → mitigada con revocación lógica obligatoria.

## Migration Plan
- Flyway: tabla `fixed_assignments` con `CHECK (day_of_week BETWEEN 1 AND 7)`, FKs a `employees` (titular, `created_by`, `revoked_by`) y `parking_spaces`, e índices únicos filtrados y de soporte (`IX_fixed_assignments_employee_id_active`) según `docs/data-model.md` §3.3.
- Sin migración de datos (capability nueva).
- En Fase 2, `generic-resource-refactor` introduce `resource_id`; la migración de `parking_space_id → resource_id` se aborda en ese change, no aquí.
