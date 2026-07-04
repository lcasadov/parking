# Tasks: init-availability-calendar

> **Orden TDD estricto (Red → Green → Refactor).** Primero se escriben los tests (deben fallar), luego la implementación mínima para que pasen, luego el refactor. Ninguna tarea de implementación se aborda sin un test rojo previo. (Coherente con las oleadas 2-3 del flujo `apply` y `docs/TESTING-STRATEGY.md`.)

## 1. Tests primero — RED (deben fallar antes de implementar)
- [x] 1.1 `should_includeSpace_when_freeForDate` (Req 1, plaza libre disponible).
- [x] 1.2 `should_includeSpace_when_fixedAssignmentReleasedForDate` (Req 1, asignación liberada vuelve disponible).
- [x] 1.3 `should_excludeSpace_when_requestApprovedForDate` (Req 1, solicitud aprobada ocupa).
- [x] 1.4 `should_excludeSpace_when_visitorReservationForDate` (Req 1, reserva de visitante ocupa).
- [x] 1.5 `should_excludeSpace_when_inactive` (edge: recurso inactivo nunca disponible).
- [x] 1.6 `should_excludeSpace_when_fixedAssignmentActiveWithoutRelease` (edge: asignación vigente sin release).
- [x] 1.7 `should_returnBadRequest_when_dateMissingOrMalformed` (Req 2, validación `date`).
- [x] 1.8 `should_returnBadRequest_when_weekStartInvalid` (Req 2, validación `weekStart`).
- [x] 1.9 `should_returnAdminCalendar_when_callerIsAdmin` (Req 3, camino feliz admin).
- [x] 1.10 `should_returnForbidden_when_employeeCallsAdminCalendar` (Req 3, 403 autorización).
- [x] 1.11 `should_returnOwnWeekWithoutOtherNames_when_employeeCallsMyWeek` (Req 4, privacidad).
- [x] 1.12 `should_returnUnauthorized_when_noSessionOnMyWeek` (Req 4, 401 sin sesión).
- [x] 1.13 Cada scenario BDD del spec cubierto por ≥1 test (nombres `should..._when...`). Además: edge de release cubierta por solicitud aprobada, normalización de `weekStart` a lunes, semana actual vía `ClockPort`, y aserción de ausencia de N+1 (nº de consultas constante frente al nº de plazas, vía `Statistics` de Hibernate).

## 2. Implementación — GREEN (lo mínimo para que los tests pasen)
- [x] 2.1 `AvailabilityService` (dominio): función de disponibilidad para fecha F aplicando las 4 condiciones (activo + sin `FixedAssignment` vigente o liberado + sin `Request` `APPROVED` + (solo plazas) sin `VisitorReservation`). Idéntica —mismo mapeo `getDayOfWeek().getValue()` 1=Lun..7=Dom— a la comprobación en línea de `VisitorReservationService#create` / `RequestService#approve`.
- [x] 2.2 Métodos de lectura por rango (aditivos, sin tocar los existentes) en `FixedAssignmentRepository` (`findByParkingSpaceIdInAndActiveTrue`), `ReleaseRepository` (`findByReleaseDateBetween`, `findByEmployeeIdAndReleaseDateBetween`), `RequestRepository` (`findByStatusAndRequestedDateBetween`, `findByEmployeeIdAndRequestedDateBetween`) y `VisitorReservationRepository` (`findByReservationDateBetween`).
- [x] 2.3 Mapeo a estados de calendario: `CalendarCellState` (admin) y `MyWeekDayState` (mi-semana).
- [x] 2.4 `getAvailability`: controller `GET /availability` + validación de `date` (400 si falta/format inválido).
- [x] 2.5 `getAdminCalendar`: controller `GET /calendar/admin` restringido a `ADMIN` (403 para `EMPLOYEE`) + validación de `weekStart`.
- [x] 2.6 `getMyWeek`: controller `GET /calendar/my-week`; filtra a recursos del solicitante y omite nombres de terceros; `weekStart` opcional → semana actual vía `ClockPort`.
- [x] 2.7 Ensamblado por semana en bloque para evitar N+1 (una consulta por entidad y rango; verificado con conteo de sentencias Hibernate).
- [x] 2.8 Manejo de errores uniforme (`ApiError { error, message, fields, timestamp }`); nuevos handlers para `MissingServletRequestParameterException` y `MethodArgumentTypeMismatchException` en `GlobalExceptionHandler`.

## 3. Refactor
- [x] 3.1 Con los tests en verde: métodos pequeños (`cell`, `myWeekDay`, helpers de carga; complejidad < 15), literales en constantes (S1192), inyección por constructor (S6813), sin cambiar comportamiento. Cobertura del servicio 100 % líneas / 97 % ramas.

## 4. Frontend — mismo ciclo test-first (Vitest + RTL → implementación)
- [ ] 4.1 Vista de disponibilidad por fecha (consume `getAvailability`).
- [ ] 4.2 Rejilla de calendario semanal admin (consume `getAdminCalendar`, gated por rol `ADMIN`).
- [ ] 4.3 Componente "Mi Semana" (consume `getMyWeek`; no renderiza nombres ajenos).
