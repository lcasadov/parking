# Tasks: init-employees

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [ ] 1.1 `should_create_employee_when_login_and_email_are_unique`.
- [ ] 1.2 `should_return_409_when_creating_employee_with_existing_login`.
- [ ] 1.3 `should_return_409_when_creating_employee_with_existing_email`.
- [ ] 1.4 `should_return_400_when_creating_employee_with_invalid_fields`.
- [ ] 1.5 `should_update_employee_when_data_is_valid`.
- [ ] 1.6 `should_return_409_when_updating_login_to_another_employee_login`.
- [ ] 1.7 `should_set_active_false_when_deactivating_employee`.
- [ ] 1.8 `should_set_active_true_when_reactivating_employee`.
- [ ] 1.9 `should_return_temp_password_and_set_must_change_when_resetting_in_phase1`.
- [ ] 1.10 `should_send_email_and_set_must_change_when_resetting_in_phase2`.
- [ ] 1.11 `should_return_403_when_employee_role_lists_employees`.
- [ ] 1.12 `should_return_403_when_employee_role_resets_password`.
- [ ] 1.13 Cada scenario BDD del spec cubierto por ≥1 test (`@WebMvcTest` para estados HTTP, tests de caso de uso con repos mockeados).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [ ] 2.1 Entidad `Employee` completa (todos los campos de `data-model.md` §3.1) + `EmployeeRepository` (puerto + adaptador JPA) con búsqueda por `login` y `email`.
- [ ] 2.2 Caso de uso `CreateEmployeeUseCase`: validación de campos, comprobación de unicidad `login`/`email`, alta con `active = true`/`enabled = true`.
- [ ] 2.3 Caso de uso `UpdateEmployeeUseCase`: edición con comprobación de colisión `login`/`email` excluyendo al propio empleado.
- [ ] 2.4 Casos de uso `DeactivateEmployeeUseCase` (`active = false`) y `ReactivateEmployeeUseCase` (`active = true`), idempotentes.
- [ ] 2.5 Caso de uso `ResetEmployeePasswordUseCase`: genera contraseña temporal (reutiliza `PasswordPolicy`), fija `password_must_change = true`; 🟢 devuelve temporal / 🔵 dispara email vía `notifications`.
- [ ] 2.6 Controllers REST: `listEmployees`, `createEmployee`, `updateEmployee`, `deactivateEmployee`, `reactivateEmployee`, `resetEmployeePassword`, `exportEmployees` con `@PreAuthorize("hasRole('ADMIN')")`.
- [ ] 2.7 Mapeo de violación de índice único `UX_employees_login`/`UX_employees_email` → 409 `ApiError { error, message, fields, timestamp }`.
- [ ] 2.8 Manejo de errores uniforme: 400 validación, 403 autorización, 404 no encontrado, 409 unicidad.

## 3. Refactor
- [ ] 3.1 Con los tests en verde: extraer métodos (complejidad < 15), eliminar duplicación y aplicar `docs/SONAR-STANDARDS.md`, sin cambiar comportamiento.

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [x] 4.1 Vista de gestión de empleados (tabla paginada con búsqueda `q`), visible solo para `ADMIN`.
- [x] 4.2 Formulario de alta/edición con validación de campos y feedback de 409 en `login`/`email`.
- [x] 4.3 Acciones de baja, reactivación, reset de contraseña (🟢 modal mostrando la temporal una vez) y exportación CSV/XLSX.
