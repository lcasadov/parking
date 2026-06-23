# Tasks: init-requests

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [ ] 1.1 `should_create_pending_request_when_date_within_window` (Req 1, caso feliz).
- [ ] 1.2 `should_return_400_outside_window_when_date_before_today_or_after_14_days` (Req 1, error de ventana).
- [ ] 1.3 `should_return_409_already_pending_when_duplicate_request_for_same_date` (Req 1, unicidad).
- [ ] 1.4 `should_list_only_own_requests_when_employee_lists_mine` (Req 2, BOLA).
- [ ] 1.5 `should_list_pending_in_fifo_order_when_admin_lists_pending` (Req 2, FIFO).
- [ ] 1.6 `should_return_403_when_employee_lists_pending` (Req 2, autorización).
- [ ] 1.7 `should_cancel_request_when_owner_cancels_pending` (Req 3, caso feliz).
- [ ] 1.8 `should_return_409_when_cancel_already_resolved_request` (Req 3, estado terminal).
- [ ] 1.9 `should_return_403_when_cancel_request_of_another_employee` (Req 3, BOLA).
- [ ] 1.10 `should_approve_request_when_space_available` (Req 4, caso feliz + nota en email).
- [ ] 1.11 `should_return_409_when_approve_space_not_available` (Req 4, disponibilidad).
- [ ] 1.12 `should_return_409_when_two_admins_approve_same_space_same_date` (Req 4, concurrencia).
- [ ] 1.13 `should_reject_request_when_reason_code_from_catalog` (Req 5, caso feliz).
- [ ] 1.14 `should_return_400_when_reject_other_without_free_text` (Req 5, validación `OTHER`).
- [ ] 1.15 `@WebMvcTest` de los 7 endpoints (estados HTTP + RBAC `@PreAuthorize`).
- [ ] 1.16 Cada scenario BDD del spec cubierto por ≥1 test (nombres `should..._when...`).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [ ] 2.1 Entidad `Request` (`requests`) + repositorio (puerto `RequestRepository` + adaptador JPA), con campos de estado, resolución y catálogo de rechazo.
- [ ] 2.2 Migración Flyway de la tabla `requests`: constraints (`CK_requests_status`, `CK_requests_rejection_reason_code`), FKs, índice único filtrado `UX_requests_employee_date_pending` e índices de rendimiento.
- [ ] 2.3 `CreateRequestUseCase`: validación de ventana hoy..hoy+14 (con `ClockPort`), unicidad `PENDING` (índice → 409 `REQUEST_ALREADY_PENDING`), estado inicial `PENDING` con `parking_space_id = NULL`.
- [ ] 2.4 `ListMyRequestsUseCase` (filtro por `employee_id` de sesión, paginado) y `ListPendingRequestsUseCase` (FIFO `created_at ASC`, paginado).
- [ ] 2.5 `GetRequestUseCase` (ADMIN) con 404 si no existe.
- [ ] 2.6 `CancelRequestUseCase`: comprobación de pertenencia (BOLA) + máquina de estados (solo desde `PENDING` → `CANCELLED`; 409 si terminal).
- [ ] 2.7 `ApproveRequestUseCase`: validación de disponibilidad vía `availability-calendar` dentro de la transacción, asignación de `parking_space_id`, `resolved_by_id`/`resolved_at`, `approval_note`; 409 por no disponibilidad o concurrencia.
- [ ] 2.8 `RejectRequestUseCase`: catálogo `rejection_reason_code`, obligatoriedad de `rejection_reason` (≥5) si `OTHER`, `resolved_by_id`/`resolved_at`.
- [ ] 2.9 Disparo de eventos de dominio hacia `notifications` (nueva solicitud → admins; aprobada/rechazada → empleado), `AFTER_COMMIT`.
- [ ] 2.10 Controllers `@PreAuthorize` para los 7 endpoints (RBAC `ADMIN`/`EMPLOYEE`) + manejo de errores uniforme `ApiError { error, message, fields, timestamp }`.

## 3. Refactor
- [ ] 3.1 Con los tests en verde: extraer métodos (complejidad < 15), eliminar duplicación y aplicar `docs/SONAR-STANDARDS.md`, sin cambiar comportamiento.

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [ ] 4.1 Pantalla "Mis solicitudes" (EMPLOYEE): listado paginado propio + crear solicitud (selector de fecha con ventana hoy..hoy+14) + cancelar en `PENDING`.
- [ ] 4.2 Bandeja de solicitudes pendientes (ADMIN) en orden FIFO.
- [ ] 4.3 Modal aprobar (selector de plaza + `approvalNote`) y modal rechazar (catálogo de motivos + texto libre obligatorio si `OTHER`).
- [ ] 4.4 Manejo de errores de la UI: 400 ventana/validación, 409 unicidad/disponibilidad/concurrencia, 403 autorización; toasts de resultado.
