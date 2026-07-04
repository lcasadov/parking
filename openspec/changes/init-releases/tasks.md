# Tasks: init-releases

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [x] 1.1 `should_create_voluntary_release_when_owner_and_future_date` (Req 1, caso feliz).
- [x] 1.2 `should_reject_with_400_when_release_date_in_past` (Req 1, ventana).
- [x] 1.3 `should_reject_with_409_when_no_fixed_assignment_for_that_day` (Req 1, conflicto).
- [x] 1.4 `should_list_only_own_releases_when_employee_requests_mine` (Req 2, listado propio).
- [x] 1.5 `should_cancel_release_when_future_and_own` (Req 2, cancelación válida).
- [x] 1.6 `should_reject_with_403_when_cancelling_release_of_another_employee` (Req 2, BOLA).
- [x] 1.7 `should_reject_with_409_when_cancelling_past_release` (Req 2, fecha pasada).
- [x] 1.8 `should_create_administrative_release_when_admin_and_reason_present` (Req 3, caso feliz).
- [x] 1.9 `should_reject_with_400_when_administrative_release_missing_reason` (Req 3, validación).
- [x] 1.10 `should_reject_with_403_when_non_admin_creates_administrative_release` (Req 3, rol).
- [x] 1.11 `should_reject_with_409_when_resource_already_released_for_date` (Req 4, duplicado).
- [x] 1.12 `should_allow_only_one_release_when_two_concurrent_for_same_resource_and_date` (Req 4, concurrencia).
- [x] 1.13 `@WebMvcTest` de los 4 endpoints (estados HTTP + forma de error).
- [x] 1.14 Cada scenario BDD del spec cubierto por ≥1 test (nombres `should..._when...`).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [x] 2.1 Entidad `Release` (`employee_id`, `released_by_id`, `release_date`, `type` enum `ReleaseType`, `reason`, `created_at`) + repositorio (puerto + adaptador JPA).
- [x] 2.2 Migración Flyway: tabla `releases` (FKs, `CK_releases_type`, índice `IX_releases_parking_space_id_date`).
- [x] 2.3 `ReleaseUseCase` (dominio): validación de ventana `release_date >= hoy` con `ClockPort`, resolución implícita de plaza fija, obligatoriedad de `reason` en `ADMINISTRATIVE`.
- [x] 2.4 Unicidad recurso+fecha (constraint/comprobación) → 409 ante duplicado y concurrencia.
- [x] 2.5 BOLA: verificar `release.employee_id == session.employee_id` en `cancelRelease` y `listMyReleases`.
- [x] 2.6 Controllers: `GET /releases/mine`, `POST /releases`, `DELETE /releases/{id}`, `POST /releases/administrative` (RBAC `ADMIN` en administrativa).
- [x] 2.7 Evento de auditoría (`audit-retention`) en liberación voluntaria, administrativa y cancelación.
- [x] 2.8 Manejo de errores uniforme (`ApiError { error, message, fields, timestamp }`): 400 ventana/`reason`, 403 BOLA/rol, 409 sin asignación/duplicado/pasado.

## 3. Refactor
- [x] 3.1 Con los tests en verde: extraer métodos (complejidad < 15), eliminar duplicación y aplicar `docs/SONAR-STANDARDS.md`, sin cambiar comportamiento.

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [x] 4.1 Botón "Liberar" sobre el recurso fijo propio + selector de fecha (≥ hoy) → `POST /releases`.
- [x] 4.2 Listado "Mis liberaciones" con paginación (`GET /releases/mine`) y acción de cancelar futura (`DELETE /releases/{id}`).
- [x] 4.3 Panel admin: liberación administrativa con empleado, recurso, fecha y `reason` obligatorio (`POST /releases/administrative`).
- [x] 4.4 Toasts de éxito/error; deshabilitar cancelar en liberaciones pasadas.

## Notas de implementación (backend, #36)
- **Índice de unicidad**: se implementa `UX_releases_space_date` UNIQUE(parking_space_id, release_date) en `V9__releases.sql`, que **supersede** al `IX_releases_parking_space_id_date` (no único) de la tarea 2.2 / data-model §Indexes: al ser la cancelación un **borrado físico** de la fila, un índice único simple (no filtrado) basta para 409 en duplicado y concurrencia, y sirve también el lookup de disponibilidad. Verificado contra SQL Server real (Testcontainers) incl. carrera con `CountDownLatch` (un 201, un 409).
- **Cancelación = borrado físico** de la fila futura (design §Decisions); `releases` no tiene estado de baja lógica, por eso una liberación cancelada no bloquea una nueva del mismo recurso/fecha.
- **Auditoría (2.7)**: `ReleaseAuditPort` + `LoggingReleaseAuditAdapter` (stub de log), disparado `AFTER_COMMIT` vía `ReleaseAuditEvent`/`ReleaseEventListener`. La persistencia en `audit_log` se consolida en **audit-retention (B10)**; aquí solo el puerto + stub (no persiste).
- **Discrepancia openapi**: `DELETE /releases/{id}` no lista `409` en `docs/openapi.yaml`, pero spec Req 2 y tarea 1.7 exigen 409 al cancelar una liberación pasada. Se implementa **409** (autoridad de comportamiento del spec/tests); pendiente añadir el `409` al contrato openapi.
