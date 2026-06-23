# Tasks: init-releases

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [ ] 1.1 `should_create_voluntary_release_when_owner_and_future_date` (Req 1, caso feliz).
- [ ] 1.2 `should_reject_with_400_when_release_date_in_past` (Req 1, ventana).
- [ ] 1.3 `should_reject_with_409_when_no_fixed_assignment_for_that_day` (Req 1, conflicto).
- [ ] 1.4 `should_list_only_own_releases_when_employee_requests_mine` (Req 2, listado propio).
- [ ] 1.5 `should_cancel_release_when_future_and_own` (Req 2, cancelación válida).
- [ ] 1.6 `should_reject_with_403_when_cancelling_release_of_another_employee` (Req 2, BOLA).
- [ ] 1.7 `should_reject_with_409_when_cancelling_past_release` (Req 2, fecha pasada).
- [ ] 1.8 `should_create_administrative_release_when_admin_and_reason_present` (Req 3, caso feliz).
- [ ] 1.9 `should_reject_with_400_when_administrative_release_missing_reason` (Req 3, validación).
- [ ] 1.10 `should_reject_with_403_when_non_admin_creates_administrative_release` (Req 3, rol).
- [ ] 1.11 `should_reject_with_409_when_resource_already_released_for_date` (Req 4, duplicado).
- [ ] 1.12 `should_allow_only_one_release_when_two_concurrent_for_same_resource_and_date` (Req 4, concurrencia).
- [ ] 1.13 `@WebMvcTest` de los 4 endpoints (estados HTTP + forma de error).
- [ ] 1.14 Cada scenario BDD del spec cubierto por ≥1 test (nombres `should..._when...`).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [ ] 2.1 Entidad `Release` (`employee_id`, `released_by_id`, `release_date`, `type` enum `ReleaseType`, `reason`, `created_at`) + repositorio (puerto + adaptador JPA).
- [ ] 2.2 Migración Flyway: tabla `releases` (FKs, `CK_releases_type`, índice `IX_releases_parking_space_id_date`).
- [ ] 2.3 `ReleaseUseCase` (dominio): validación de ventana `release_date >= hoy` con `ClockPort`, resolución implícita de plaza fija, obligatoriedad de `reason` en `ADMINISTRATIVE`.
- [ ] 2.4 Unicidad recurso+fecha (constraint/comprobación) → 409 ante duplicado y concurrencia.
- [ ] 2.5 BOLA: verificar `release.employee_id == session.employee_id` en `cancelRelease` y `listMyReleases`.
- [ ] 2.6 Controllers: `GET /releases/mine`, `POST /releases`, `DELETE /releases/{id}`, `POST /releases/administrative` (RBAC `ADMIN` en administrativa).
- [ ] 2.7 Evento de auditoría (`audit-retention`) en liberación voluntaria, administrativa y cancelación.
- [ ] 2.8 Manejo de errores uniforme (`ApiError { error, message, fields, timestamp }`): 400 ventana/`reason`, 403 BOLA/rol, 409 sin asignación/duplicado/pasado.

## 3. Refactor
- [ ] 3.1 Con los tests en verde: extraer métodos (complejidad < 15), eliminar duplicación y aplicar `docs/SONAR-STANDARDS.md`, sin cambiar comportamiento.

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [ ] 4.1 Botón "Liberar" sobre el recurso fijo propio + selector de fecha (≥ hoy) → `POST /releases`.
- [ ] 4.2 Listado "Mis liberaciones" con paginación (`GET /releases/mine`) y acción de cancelar futura (`DELETE /releases/{id}`).
- [ ] 4.3 Panel admin: liberación administrativa con empleado, recurso, fecha y `reason` obligatorio (`POST /releases/administrative`).
- [ ] 4.4 Toasts de éxito/error; deshabilitar cancelar en liberaciones pasadas.
