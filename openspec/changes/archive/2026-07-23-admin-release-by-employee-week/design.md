# Design — admin-release-by-employee-week

## Decisions
- **Selector de empleados para liberación**: endpoint de solo lectura accesible a ADMIN y AGENCIA,
  independiente del CRUD de empleados (que sigue ADMIN-only). Devuelve datos mínimos (id, nombre).
- **Ocupación por empleado y semana**: nuevo endpoint que, dado `employeeId` y el inicio de semana,
  devuelve las reservas del empleado (plaza + puesto) para los 7 días, cada una con su origen
  (`FIXED_ASSIGNMENT` o `REQUEST_APPROVED`), fecha, recurso (número/etiqueta) y `requestId` si es
  por solicitud. Reutiliza `AvailabilityService.occupancy*`.
- **Liberación del lote (frontend)**: por cada reserva marcada, el cliente llama al endpoint que
  corresponde con el MISMO motivo: `POST /releases/administrative` (fija) o
  `POST /requests/{id}/admin-cancel` (solicitud). Manejo de errores parciales con feedback por item.
- **RBAC**: AGENCIA = ADMIN solo en este flujo (selección + ocupación + liberación administrativa +
  admin-cancel). No accede al resto del panel admin.

## Risks
- Errores parciales en el lote: se reporta cuáles se liberaron y cuáles no; no es transaccional.
- Concurrencia: cada liberación mantiene sus 409 existentes (recurso ya liberado / estado no cancelable).
