# Tasks: init-desks

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [ ] 1.1 `should_create_desk_when_number_in_range_and_unique`
- [ ] 1.2 `should_return_409_when_creating_desk_with_duplicate_number`
- [ ] 1.3 `should_return_400_when_desk_number_out_of_range`
- [ ] 1.4 `should_return_403_when_employee_creates_desk`
- [ ] 1.5 `should_update_category_when_admin_sets_executive`
- [ ] 1.6 `should_remove_desk_from_availability_when_deactivated`
- [ ] 1.7 `should_create_desk_fixed_assignment_when_employee_has_parking_assignment`
- [ ] 1.8 `should_return_409_when_assigning_same_desk_to_two_employees_same_day`
- [ ] 1.9 `should_create_pending_request_when_desk_available_in_window`
- [ ] 1.10 `should_make_desk_available_when_executive_releases_it`
- [ ] 1.11 `should_return_409_when_approving_second_request_for_already_approved_desk`
- [ ] 1.12 Cada scenario BDD del spec cubierto por ≥1 test (nombres `should..._when...`).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [ ] 2.1 Entidad `Desk` (`number` 1-65, `DeskCategory` `STANDARD`/`EXECUTIVE`, `coord_x`, `coord_y`, `active`) + repositorio (puerto + adaptador JPA).
- [ ] 2.2 Migración Flyway: tabla `desks` con CHECK `number` 1-65 e índice único en `number`; seed opcional de los 65 puestos.
- [ ] 2.3 Casos de uso CRUD: crear (unicidad + rango), editar categoría/coordenadas, activar/desactivar.
- [ ] 2.4 Validación de coordenadas (0-100) y categoría enum.
- [ ] 2.5 Controllers `/desks`: `listDesks`, `createDesk`, `getDesk`, `updateDesk`, `setDeskActivation` (proponer en `docs/openapi.yaml`).
- [ ] 2.6 Integración con `BookableResource` (`resourceType = DESK`) en asignación fija, solicitud, liberación y disponibilidad.
- [ ] 2.7 Permitir `FixedAssignment` `DESK` coexistente con `PARKING` para el mismo empleado/día (unicidad por `resource_type`).
- [ ] 2.8 Excluir `VisitorReservation` del cálculo de disponibilidad de `DESK`.
- [ ] 2.9 Manejo de errores uniforme (`ApiError { error, message, fields, timestamp }`): 409 unicidad/disponibilidad, 400 rango/coordenadas, 403 autorización.

## 3. Refactor
- [ ] 3.1 Con los tests en verde: extraer métodos (complejidad < 15), eliminar duplicación y aplicar `docs/SONAR-STANDARDS.md`, sin cambiar comportamiento.

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [ ] 4.1 Pantalla admin de gestión de puestos (listado + alta/edición + activación, selector de categoría).
- [ ] 4.2 Integrar puestos en la solicitud unificada (plaza y/o puesto para la misma fecha).
- [ ] 4.3 Distinción visual de `EXECUTIVE` en listados (el plano completo llega en `floor-plan`).
