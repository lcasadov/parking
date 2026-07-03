# Tasks: init-employees

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [x] 1.1 `should_create_employee_when_login_and_email_are_unique`.
- [x] 1.2 `should_return_409_when_creating_employee_with_existing_login`.
- [x] 1.3 `should_return_409_when_creating_employee_with_existing_email`.
- [x] 1.4 `should_return_400_when_creating_employee_with_invalid_fields`.
- [x] 1.5 `should_update_employee_when_data_is_valid`.
- [x] 1.6 `should_return_409_when_updating_email_to_another_employee_email` (login es inmutable por contrato: `EmployeeUpdate` no incluye `login`; la colisión de unicidad editable aplica al `email`).
- [x] 1.7 `should_set_active_false_when_deactivating_employee`.
- [x] 1.8 `should_set_active_true_when_reactivating_employee`.
- [x] 1.9 `should_return_temp_password_and_set_must_change_when_resetting_in_phase1`.
- [x] 1.10 `should_send_email_and_set_must_change_when_resetting_in_phase2`.
- [x] 1.11 `should_return_403_when_employee_role_lists_employees`.
- [x] 1.12 `should_return_403_when_employee_role_resets_password`.
- [x] 1.13 Cada scenario BDD del spec cubierto por ≥1 test (`@WebMvcTest` para estados HTTP + RBAC, tests de caso de uso con repos mockeados, `EmployeeManagementIT` con Testcontainers para el índice único real).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [x] 2.1 Entidad `Employee` completa (todos los campos de `data-model.md` §3.1, ya presentes de auth-local; extendida con factoría `register` + setters) + `EmployeeRepository` con `existsByLogin`/`existsByEmail`/`existsByEmailAndIdNot` y `search` paginado por texto/estado. (V4 ya cubre columnas e índices únicos: no requiere migración nueva.)
- [x] 2.2 `EmployeeService.create`: validación de campos (`@Valid` en DTO), comprobación de unicidad `login`/`email`, alta con `active = true`/`enabled = true`.
- [x] 2.3 `EmployeeService.update`: edición con comprobación de colisión `email` excluyendo al propio empleado (`login` inmutable por contrato).
- [x] 2.4 `EmployeeService.deactivate` (`active = false`) y `reactivate` (`active = true`), idempotentes.
- [x] 2.5 `EmployeeService.resetPassword`: genera contraseña temporal (`TemporaryPasswordGenerator` con `SecureRandom`, válida contra `PasswordPolicy`), fija `password_must_change = true`; 🟢 Fase 1 devuelve temporal / 🔵 Fase 2 dispara `PasswordResetNotifier` (puerto; adaptador email de `notifications` fuera de alcance).
- [x] 2.6 Controllers REST: `listEmployees`, `createEmployee`, `updateEmployee`, `deactivateEmployee`, `reactivateEmployee`, `resetEmployeePassword`, `exportEmployees` con `@PreAuthorize("hasRole('ADMIN')")` a nivel de clase.
- [x] 2.7 Mapeo de violación de índice único `UX_employees_login`/`UX_employees_email` → 409 `ApiError { error, message, fields, timestamp }` (comprobación previa en el servicio + red dura `DataIntegrityViolationException` en `GlobalExceptionHandler`).
- [x] 2.8 Manejo de errores uniforme: 400 validación, 403 autorización, 404 no encontrado, 409 unicidad.

## 3. Refactor
- [x] 3.1 Con los tests en verde: métodos con complejidad < 15, constantes `static final` para literales (S1192), inyección por constructor (S6813), DTOs en la capa web (S4684), `.orElseThrow()` (S3655). Quality Gate verde: `mvn clean verify` BUILD SUCCESS, cobertura cumplida (`EmployeeController` 100%/100%, `EmployeeService` 98%/90% líneas/branches).

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [ ] 4.1 Vista de gestión de empleados (tabla paginada con búsqueda `q`), visible solo para `ADMIN`.
- [ ] 4.2 Formulario de alta/edición con validación de campos y feedback de 409 en `login`/`email`.
- [ ] 4.3 Acciones de baja, reactivación, reset de contraseña (🟢 modal mostrando la temporal una vez) y exportación CSV/XLSX.
