# Tasks: init-fixed-assignments

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [x] 1.1 `should_create_fixed_assignments_when_admin_sets_valid_days` (Requirement 1, alta válida).
- [x] 1.2 `should_return_400_when_day_of_week_out_of_range_or_empty` (Requirement 1, validación).
- [x] 1.3 `should_return_409_when_space_already_assigned_to_other_employee_on_same_day` (Requirement 2).
- [x] 1.4 `should_return_409_when_employee_already_has_resource_on_same_day` (Requirement 2).
- [x] 1.5 `should_keep_single_active_row_when_concurrent_puts_target_same_space_and_day` (Requirement 2, concurrencia → 409).
- [x] 1.6 `should_logically_revoke_when_admin_deletes_active_assignment` (Requirement 3, revocación + intacto histórico/aprobaciones).
- [x] 1.7 `should_return_404_when_revoking_employee_without_active_assignment` (Requirement 3).
- [x] 1.8 `should_return_own_assignments_when_employee_queries_self` (Requirement 4, camino feliz).
- [x] 1.9 `should_return_403_when_employee_queries_other_employee_assignments` (Requirement 4, BOLA).
- [x] 1.10 `should_return_403_when_employee_lists_all_assignments` (Requirement 4, autorización).
- [x] 1.11 `should_be_idempotent_when_put_repeats_same_space_and_days` (edge case idempotencia).
- [x] 1.12 `@WebMvcTest` de los endpoints: estados HTTP, RBAC y forma de error `{ error, message, fields, timestamp }`.

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [x] 2.1 Entidad `FixedAssignment` (`parking_space_id`, `employee_id`, `day_of_week`, `active`, `created_by_id`, `created_at`, `revoked_by_id`, `revoked_at`) + repositorio (puerto + adaptador JPA).
- [x] 2.2 Migración Flyway: tabla `fixed_assignments`, CHECK `day_of_week BETWEEN 1 AND 7`, FKs e índices únicos filtrados (`UX_fixed_assignments_space_day_active`, `UX_fixed_assignments_employee_day_active`) + `IX_fixed_assignments_employee_id_active`.
- [x] 2.3 `SetFixedAssignmentsUseCase`: validar `daysOfWeek` (1-7, no vacía), reemplazar el conjunto (alta de nuevos días, revocación lógica de los retirados), idempotencia. _(Implementado como `FixedAssignmentService.setAssignments`, alineado con la estructura de módulos existente employee/parkingspace.)_
- [x] 2.4 Traducción de violación de índice único filtrado → 409 `{ error, message, fields, timestamp }` (unicidad plaza/día y empleado/día).
- [x] 2.5 `RevokeFixedAssignmentUseCase`: marcar `active=false`, `revoked_at`, `revoked_by_id`; 404 si no hay asignación activa. _(`FixedAssignmentService.revoke`.)_
- [x] 2.6 `GetEmployeeFixedAssignmentsUseCase`: verificación de pertenencia (BOLA) `employeeId == session.employeeId` para `EMPLOYEE`; ADMIN sin restricción. _(`FixedAssignmentService.getEmployeeAssignments`.)_
- [x] 2.7 Controllers: `GET /fixed-assignments` (ADMIN, paginado), `GET|PUT|DELETE /fixed-assignments/employee/{employeeId}`.
- [x] 2.8 RBAC: `listFixedAssignments`/`setEmployeeFixedAssignments`/`revokeEmployeeFixedAssignment` solo ADMIN; `getEmployeeFixedAssignments` ADMIN o propias.
- [x] 2.9 `ClockPort` para `created_at`/`revoked_at`.

## 3. Refactor
- [x] 3.1 Con los tests en verde: extraer métodos (complejidad < 15), eliminar duplicación y aplicar `docs/SONAR-STANDARDS.md`, sin cambiar comportamiento.

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [x] 4.1 Vista ADMIN: gestión de asignaciones fijas por empleado (selector de plaza + días de la semana, guardar vía `setEmployeeFixedAssignments`).
- [x] 4.2 Acción ADMIN de revocación (`revokeEmployeeFixedAssignment`) con confirmación.
- [x] 4.3 Lista ADMIN de todas las asignaciones activas (`listFixedAssignments`, paginada).
- [x] 4.4 Vista EMPLOYEE de solo lectura "mis asignaciones fijas" (`getEmployeeFixedAssignments` con su propio id).
- [x] 4.5 Manejo de errores 400 (días inválidos) y 409 (conflicto de unicidad) con toasts.
