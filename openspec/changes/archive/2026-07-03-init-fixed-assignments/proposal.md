# Proposal: init-fixed-assignments

## Why
Inicializar la capability **fixed-assignments** en OpenSpec: dejar documentado,
como contrato verificable antes de implementar, cómo el `ADMIN` asigna de forma
fija una plaza de parking a un empleado por día de la semana, cómo el `EMPLOYEE`
consulta las propias y cómo se revoca lógicamente. Es la base sobre la que el
cálculo de disponibilidad, las liberaciones y las solicitudes operan.

## What Changes
- Se añade la capability `fixed-assignments` con sus Requirements y escenarios BDD.
- Endpoints (ver `docs/openapi.yaml`):
  - `GET /fixed-assignments` (`listFixedAssignments`, ADMIN).
  - `GET /fixed-assignments/employee/{employeeId}` (`getEmployeeFixedAssignments`, ADMIN o EMPLOYEE propias).
  - `PUT /fixed-assignments/employee/{employeeId}` (`setEmployeeFixedAssignments`, ADMIN).
  - `DELETE /fixed-assignments/employee/{employeeId}` (`revokeEmployeeFixedAssignment`, ADMIN).
- Unicidad —entre filas activas— de plaza/día y empleado/día mediante índices únicos filtrados → 409.
- `day_of_week` 1-7. Revocación lógica (`active=false` + `revoked_at`/`revoked_by_id`).

## Capabilities
- `fixed-assignments` (ADDED)

## Impact
- **Entidades**: `FixedAssignment` (nueva); referencias a `Employee` y `ParkingSpace`.
- **Seguridad**: solo `ADMIN` gestiona; verificación de pertenencia (BOLA) en la consulta del `EMPLOYEE` (ver `docs/security-design.md`).
- **UI**: pantalla de administración de asignaciones fijas por empleado y vista de lectura "mis asignaciones" para el empleado.

## Out of scope
- Liberaciones puntuales del recurso asignado (las aporta `releases`).
- Solicitudes por fecha concreta (las aporta `requests`).
- Cálculo de disponibilidad (lo aporta `availability-calendar`).
- Asignación fija de puestos `DESK` y abstracción `BookableResource` (Fase 2: `generic-resource-refactor` + `desks`).
- Reserva de visitantes (no aplica a asignaciones fijas).
