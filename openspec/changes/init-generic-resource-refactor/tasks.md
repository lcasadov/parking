# Tasks: init-generic-resource-refactor

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [ ] 1.1 `should_model_fixed_assignment_as_PARKING_resource_when_assignment_is_persisted` (Req 1, scenario asignación fija PARKING).
- [ ] 1.2 `should_keep_resource_id_null_with_type_PARKING_when_request_is_pending` (Req 1, scenario solicitud PENDING).
- [ ] 1.3 `should_return_409_when_approving_request_without_availability` (Req 2, scenario 409 sin disponibilidad).
- [ ] 1.4 `should_return_409_when_second_pending_request_same_employee_and_date` (Req 2, scenario unicidad PENDING).
- [ ] 1.5 `should_return_same_available_resources_when_calculating_availability_after_refactor` (Req 2, scenario disponibilidad no-regresión; comparar resultado pre/post).
- [ ] 1.6 `should_port_existing_references_to_resource_id_with_type_PARKING_when_migration_runs` (Req 3, scenario migración Flyway; verificar conteos y valores).
- [ ] 1.7 `should_return_403_when_employee_resolves_others_request` (Req 3, scenario autorización intacta).
- [ ] 1.8 Suite de no-regresión: reejecutar verde la batería existente de `requests`/`fixed-assignments`/`releases`/`availability-calendar` sin modificar sus asserts.
- [ ] 1.9 `should_not_return_DESK_resources_when_querying_parking_core` (edge case: enum admite `DESK` pero sin filas en el núcleo).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [ ] 2.1 Enum de dominio `ResourceType` (`PARKING`, `DESK`) + mapeo JPA `@Enumerated(STRING)`.
- [ ] 2.2 Abstracción de dominio `BookableResource` + puerto `ResourceResolverPort` (resuelve `(resource_id, resource_type)` → recurso concreto).
- [ ] 2.3 Adaptador del resolver para `ParkingSpace` (`resource_type = 'PARKING'`).
- [ ] 2.4 Refactor de entidades `Request`, `FixedAssignment`, `Release`: sustituir `parking_space_id` por `resource_id` + `resource_type`; conservar valor por defecto `PARKING` en el núcleo de parking.
- [ ] 2.5 Refactor del cálculo de disponibilidad (`AvailabilityUseCase`) para filtrar por `resource_type`, preservando el resultado para `PARKING`.
- [ ] 2.6 Reexpresar reglas de unicidad sobre `resource_id`/`resource_type` (recurso/día activo, empleado/día activo, una `PENDING` por empleado/fecha[/tipo]).
- [ ] 2.7 Migración Flyway `V5__generic_resource_refactor.sql`: añadir columnas, portar datos (`resource_id = parking_space_id`, `resource_type = 'PARKING'`), recrear índices, gestionar FK. Script de rollback inverso.
- [ ] 2.8 Verificar que `ddl-auto=validate` arranca sin deriva entidad/DDL tras el refactor.

## 3. Refactor
- [ ] 3.1 Con los tests en verde: extraer métodos (complejidad < 15), eliminar duplicación y aplicar `docs/SONAR-STANDARDS.md`, sin cambiar comportamiento.

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [ ] 4.1 (No aplica — refactor interno sin cambio de comportamiento visible ni de contrato HTTP.)
