# Tasks: init-visitors

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [x] 1.1 `should_createVisitor_when_nationalIdIsUnique` (camino feliz, 201 + audit).
- [x] 1.2 `should_return409_when_creatingVisitorWithDuplicateNationalId`.
- [x] 1.3 `should_return400_when_creatingVisitorWithoutRequiredFields`.
- [x] 1.4 `should_affectOnlyFutureReservations_when_updatingVisitorCard`.
- [x] 1.5 `should_createReservation_when_spaceIsAvailableForDate` (201 + plaza no disponible).
- [x] 1.6 `should_return409_when_creatingReservationOnAlreadyOccupiedSpace`.
- [x] 1.7 `should_return400_when_creatingReservationWithoutRequiredFields`.
- [x] 1.8 `should_cancelReservation_when_reservationIsInFuture` (204 + plaza liberada).
- [x] 1.9 `should_return400_when_cancelingPastReservation`.
- [x] 1.10 `should_return403_when_employeeAccessesVisitorsEndpoints`.
- [x] 1.11 `should_return401_when_requestHasNoSession`.
- [x] 1.12 `should_return409_when_twoConcurrentReservationsTargetSameSpaceAndDate` (concurrencia transaccional).
- [x] 1.13 Cada scenario BDD del spec cubierto por ≥1 test (nombres `should..._when...`).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [x] 2.1 Entidades `Visitor` y `VisitorReservation` + repositorios (puerto + adaptador JPA), según `docs/data-model.md` §3.6/§3.7.
- [x] 2.2 Migración Flyway (`V10__visitors.sql`): tablas `visitors`, `visitor_reservations`, FKs e índices (`UX_visitors_national_id`, `UX_visitor_reservations_space_date` unico —red de concurrencia y lookup de disponibilidad— e `IX_visitor_reservations_visitor_id`).
- [x] 2.3 `CreateVisitorUseCase`: validación de campos obligatorios y unicidad de `nationalId` (409 en colisión).
- [x] 2.4 `UpdateVisitorUseCase`: edición de ficha que afecta solo a futuras reservas (la reserva referencia al visitante por id, sin denormalizar sus campos → no retroactivo por diseño).
- [x] 2.5 `GetVisitorUseCase` / `ListVisitorsUseCase`: detalle, listado paginado y filtros por `q` (búsqueda libre sobre `nationalId`, `firstName`, `lastName`, `licensePlate`, según `QParam` de `docs/openapi.yaml`).
- [x] 2.6 `CreateVisitorReservationUseCase`: validación de disponibilidad de la plaza (transaccional) → 409 si ocupada; ocupa la plaza esa fecha.
- [x] 2.7 `CancelVisitorReservationUseCase`: anula solo reservas futuras (`ClockPort`) → 400 en pasadas; 404 en inexistente.
- [x] 2.8 `ListVisitorReservationsUseCase`: listado paginado con filtros (`date`, `parkingSpaceId`).
- [x] 2.9 Controllers REST de `visitors` y `visitor-reservations` (operationIds de `docs/openapi.yaml`).
- [x] 2.10 Autorización `ADMIN` en todos los endpoints (403 a `EMPLOYEE`).
- [x] 2.11 Auditoría de cada acción vía `VisitorAuditPort` + adaptador de log `AFTER_COMMIT` (persistencia en `audit_log` la consolida `audit-retention` B10).
- [x] 2.12 Manejo de errores uniforme (`ApiError { error, message, fields, timestamp }`).

## 3. Refactor
- [x] 3.1 Con los tests en verde: extraer métodos (complejidad < 15), eliminar duplicación y aplicar `docs/SONAR-STANDARDS.md`, sin cambiar comportamiento.

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [x] 4.1 Listado de visitantes con buscador (`nationalId`/nombre/matrícula) — solo ADMIN.
- [x] 4.2 Formulario de alta/edición de ficha de visitante (validación de campos + 409 de `nationalId`).
- [x] 4.3 Detalle de visitante.
- [x] 4.4 Creación de reserva: selección de visitante, plaza y fecha; manejo de 409 (plaza ocupada).
- [x] 4.5 Listado de reservas con acción de anular (solo futuras) y manejo de 400.
- [x] 4.6 Ocultar/denegar la sección a `EMPLOYEE` (RBAC en UI).
