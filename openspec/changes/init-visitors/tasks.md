# Tasks: init-visitors

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [ ] 1.1 `should_createVisitor_when_nationalIdIsUnique` (camino feliz, 201 + audit).
- [ ] 1.2 `should_return409_when_creatingVisitorWithDuplicateNationalId`.
- [ ] 1.3 `should_return400_when_creatingVisitorWithoutRequiredFields`.
- [ ] 1.4 `should_affectOnlyFutureReservations_when_updatingVisitorCard`.
- [ ] 1.5 `should_createReservation_when_spaceIsAvailableForDate` (201 + plaza no disponible).
- [ ] 1.6 `should_return409_when_creatingReservationOnAlreadyOccupiedSpace`.
- [ ] 1.7 `should_return400_when_creatingReservationWithoutRequiredFields`.
- [ ] 1.8 `should_cancelReservation_when_reservationIsInFuture` (204 + plaza liberada).
- [ ] 1.9 `should_return400_when_cancelingPastReservation`.
- [ ] 1.10 `should_return403_when_employeeAccessesVisitorsEndpoints`.
- [ ] 1.11 `should_return401_when_requestHasNoSession`.
- [ ] 1.12 `should_return409_when_twoConcurrentReservationsTargetSameSpaceAndDate` (concurrencia transaccional).
- [ ] 1.13 Cada scenario BDD del spec cubierto por ≥1 test (nombres `should..._when...`).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [ ] 2.1 Entidades `Visitor` y `VisitorReservation` + repositorios (puerto + adaptador JPA), según `docs/data-model.md` §3.6/§3.7.
- [ ] 2.2 Migración Flyway: tablas `visitors`, `visitor_reservations`, FKs e índices (`UX_visitors_national_id`, `IX_visitor_reservations_parking_space_id_date`).
- [ ] 2.3 `CreateVisitorUseCase`: validación de campos obligatorios y unicidad de `nationalId` (409 en colisión).
- [ ] 2.4 `UpdateVisitorUseCase`: edición de ficha que afecta solo a futuras reservas.
- [ ] 2.5 `GetVisitorUseCase` / `ListVisitorsUseCase`: detalle, listado paginado y filtros (`nationalId`, `firstName`, `lastName`, `licensePlate`).
- [ ] 2.6 `CreateVisitorReservationUseCase`: validación de disponibilidad de la plaza (transaccional) → 409 si ocupada; ocupa la plaza esa fecha.
- [ ] 2.7 `CancelVisitorReservationUseCase`: anula solo reservas futuras (`ClockPort`) → 400 en pasadas; 404 en inexistente.
- [ ] 2.8 `ListVisitorReservationsUseCase`: listado paginado con filtros.
- [ ] 2.9 Controllers REST de `visitors` y `visitor-reservations` (operationIds de `docs/openapi.yaml`).
- [ ] 2.10 Autorización `ADMIN` en todos los endpoints (403 a `EMPLOYEE`).
- [ ] 2.11 Registro en `audit_log` de cada acción (create/update visitante, create/cancel reserva).
- [ ] 2.12 Manejo de errores uniforme (`ApiError { error, message, fields, timestamp }`).

## 3. Refactor
- [ ] 3.1 Con los tests en verde: extraer métodos (complejidad < 15), eliminar duplicación y aplicar `docs/SONAR-STANDARDS.md`, sin cambiar comportamiento.

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [ ] 4.1 Listado de visitantes con buscador (`nationalId`/nombre/matrícula) — solo ADMIN.
- [ ] 4.2 Formulario de alta/edición de ficha de visitante (validación de campos + 409 de `nationalId`).
- [ ] 4.3 Detalle de visitante.
- [ ] 4.4 Creación de reserva: selección de visitante, plaza y fecha; manejo de 409 (plaza ocupada).
- [ ] 4.5 Listado de reservas con acción de anular (solo futuras) y manejo de 400.
- [ ] 4.6 Ocultar/denegar la sección a `EMPLOYEE` (RBAC en UI).
