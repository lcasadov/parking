# Tasks: init-availability-calendar

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [ ] 1.1 `should_includeSpace_when_freeForDate` (Req 1, plaza libre disponible).
- [ ] 1.2 `should_includeSpace_when_fixedAssignmentReleasedForDate` (Req 1, asignación liberada vuelve disponible).
- [ ] 1.3 `should_excludeSpace_when_requestApprovedForDate` (Req 1, solicitud aprobada ocupa).
- [ ] 1.4 `should_excludeSpace_when_visitorReservationForDate` (Req 1, reserva de visitante ocupa).
- [ ] 1.5 `should_excludeSpace_when_inactive` (edge: recurso inactivo nunca disponible).
- [ ] 1.6 `should_excludeSpace_when_fixedAssignmentActiveWithoutRelease` (edge: asignación vigente sin release).
- [ ] 1.7 `should_returnBadRequest_when_dateMissingOrMalformed` (Req 2, validación `date`).
- [ ] 1.8 `should_returnBadRequest_when_weekStartInvalid` (Req 2, validación `weekStart`).
- [ ] 1.9 `should_returnAdminCalendar_when_callerIsAdmin` (Req 3, camino feliz admin).
- [ ] 1.10 `should_returnForbidden_when_employeeCallsAdminCalendar` (Req 3, 403 autorización).
- [ ] 1.11 `should_returnOwnWeekWithoutOtherNames_when_employeeCallsMyWeek` (Req 4, privacidad).
- [ ] 1.12 `should_returnUnauthorized_when_noSessionOnMyWeek` (Req 4, 401 sin sesión).
- [ ] 1.13 Cada scenario BDD del spec cubierto por ≥1 test (nombres `should..._when...`).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [ ] 2.1 `AvailabilityPort`/`AvailabilityService` (dominio): función de disponibilidad para fecha F aplicando las 4 condiciones (activo + sin `FixedAssignment` vigente o liberado + sin `Request` `APPROVED` + (solo plazas) sin `VisitorReservation`).
- [ ] 2.2 Repositorios de lectura (puerto + adaptador JPA) para cargar por rango: recursos activos, asignaciones por `day_of_week`, releases, requests `APPROVED` y reservas de visitante del intervalo.
- [ ] 2.3 Mapeo a estados de calendario: `CalendarCellState` (admin) y `MyWeekDayState` (mi-semana).
- [ ] 2.4 `getAvailability`: controller `GET /availability` + validación de `date` (400 si falta/format inválido).
- [ ] 2.5 `getAdminCalendar`: controller `GET /calendar/admin` restringido a `ADMIN` (403 para `EMPLOYEE`) + validación de `weekStart`.
- [ ] 2.6 `getMyWeek`: controller `GET /calendar/my-week`; filtra a recursos del solicitante y omite nombres de terceros; `weekStart` opcional → semana actual vía `ClockPort`.
- [ ] 2.7 Ensamblado por semana en bloque para evitar N+1 (una consulta por entidad y rango).
- [ ] 2.8 Manejo de errores uniforme (`ApiError { error, message, fields, timestamp }`).

## 3. Refactor
- [ ] 3.1 Con los tests en verde: extraer métodos (complejidad < 15), eliminar duplicación y aplicar `docs/SONAR-STANDARDS.md`, sin cambiar comportamiento.

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [ ] 4.1 Vista de disponibilidad por fecha (consume `getAvailability`).
- [ ] 4.2 Rejilla de calendario semanal admin (consume `getAdminCalendar`, gated por rol `ADMIN`).
- [ ] 4.3 Componente "Mi Semana" (consume `getMyWeek`; no renderiza nombres ajenos).
