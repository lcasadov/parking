# Capability: availability-calendar

## Resumen
Cálculo de disponibilidad de recursos para una fecha concreta y vistas de
calendario (consulta-only): disponibilidad puntual, calendario semanal completo
para el admin y "Mi Semana" personal del empleado. No muta estado: solo lee.

## Fase
🟢🔵 ambas fases (la lógica de disponibilidad es transversal y soporta el
alcance ampliado de puestos vía `BookableResource` sin cambiar su contrato).

## Reglas de negocio implicadas
(README §"Disponibilidad de un recurso en una fecha F" y §"Reglas de negocio"; NO hay códigos RN-xx)
- Un recurso está disponible para una fecha F si y solo si: está activo (`active = true`); **y** no tiene `FixedAssignment` activa con `day_of_week = dayOfWeek(F)` **o** sí la tiene pero existe un `Release` para ese recurso y fecha F; **y** no existe ninguna `Request` en estado `APPROVED` para ese recurso y fecha F; **y** (solo plazas de parking) no existe ninguna `VisitorReservation` para esa plaza y fecha F.
- El calendario semanal completo (todos los recursos, titulares y estados) solo es visible para `ADMIN`.
- "Mi Semana" no muestra nombres de otros empleados (privacidad): solo refleja los recursos propios del solicitante y los huecos libres genéricos.
- Se puede consultar disponibilidad cualquier día del año (no hay calendario laboral): sábados, domingos y festivos incluidos.

## Entidades implicadas
(consulta-only; no escribe ninguna)
- `ParkingSpace` (recursos activos a evaluar)
- `FixedAssignment` (asignaciones activas por `day_of_week`)
- `Release` (libera el recurso para una fecha)
- `Request` (estado `APPROVED` ocupa el recurso esa fecha)
- `VisitorReservation` (solo plazas: ocupa la plaza esa fecha)
- `Employee` (titular mostrado solo en la vista admin)

## Endpoints
- GET /api/v1/availability?date=F (operationId: getAvailability)
- GET /api/v1/calendar/admin?weekStart=L (operationId: getAdminCalendar)
- GET /api/v1/calendar/my-week?weekStart=L (operationId: getMyWeek)

## Permisos
| Rol | Permisos |
|---|---|
| ADMIN | Consultar `getAvailability`; ver `getAdminCalendar` (semanal completo con titulares); ver `getMyWeek` (sus propios recursos) |
| EMPLOYEE | Consultar `getAvailability`; ver `getMyWeek` (solo recursos propios, sin nombres ajenos); **no** puede acceder a `getAdminCalendar` (403) |

## ADDED Requirements
### Requirement: Cálculo de disponibilidad para una fecha
**El sistema DEBE (MUST) devolver los recursos disponibles para una fecha F aplicando las cuatro condiciones de disponibilidad (activo, sin asignación fija vigente o liberado, sin solicitud aprobada, y sin reserva de visitante para plazas).**

#### Scenario: Plaza libre aparece como disponible
- **GIVEN** una `ParkingSpace` con `active = true`, sin `FixedAssignment` activa para `dayOfWeek(F)`, sin `Request` `APPROVED` para esa plaza y fecha F, y sin `VisitorReservation` esa fecha
- **WHEN** un usuario autenticado envía `GET /availability?date=F`
- **THEN** el sistema responde 200 con `availableResources` incluyendo esa plaza (`parkingSpaceId`, `label`)
- **AND** no modifica ningún estado

#### Scenario: Plaza con asignación fija liberada vuelve a estar disponible
- **GIVEN** una `ParkingSpace` activa con `FixedAssignment` activa para `dayOfWeek(F)` y un `Release` para ese recurso y fecha F
- **WHEN** se consulta `GET /availability?date=F`
- **THEN** el sistema responde 200 y la plaza figura en `availableResources`

#### Scenario: Plaza ocupada por solicitud aprobada no está disponible
- **GIVEN** una `ParkingSpace` activa con una `Request` en estado `APPROVED` para esa plaza y fecha F
- **WHEN** se consulta `GET /availability?date=F`
- **THEN** el sistema responde 200 y la plaza NO figura en `availableResources`

#### Scenario: Plaza con reserva de visitante no está disponible
- **GIVEN** una `ParkingSpace` activa, libre de asignación y de solicitud, con una `VisitorReservation` para esa plaza y fecha F
- **WHEN** se consulta `GET /availability?date=F`
- **THEN** el sistema responde 200 y la plaza NO figura en `availableResources`

### Requirement: Validación del parámetro de fecha
**El sistema DEBE (MUST) validar el parámetro `date` (y `weekStart`) y responder 400 con la forma de error estándar cuando falte o tenga formato inválido.**

#### Scenario: Fecha ausente o con formato inválido
- **GIVEN** un usuario autenticado
- **WHEN** envía `GET /availability` sin `date`, o con `date` no parseable como `YYYY-MM-DD`
- **THEN** el sistema responde 400 con `{ error, message, fields, timestamp }` señalando `date` en `fields`
- **AND** no devuelve resultados

#### Scenario: weekStart inválido en calendario admin
- **GIVEN** un `ADMIN` autenticado
- **WHEN** envía `GET /calendar/admin?weekStart=<valor no fecha>`
- **THEN** el sistema responde 400 con `{ error, message, fields, timestamp }` señalando `weekStart` en `fields`

### Requirement: Calendario semanal completo restringido a ADMIN
**El sistema DEBE (MUST) devolver al `ADMIN` un calendario semanal de todos los recursos con su estado por día y el titular, y DEBE denegar el acceso a usuarios `EMPLOYEE`.**

#### Scenario: Admin consulta el calendario semanal
- **GIVEN** un `ADMIN` autenticado y un lunes `weekStart`
- **WHEN** envía `GET /calendar/admin?weekStart=L`
- **THEN** el sistema responde 200 con `rows` (un recurso por fila) y `cells` por día con `state` (`ASSIGNED`/`RELEASED`/`REQUEST_PENDING`/`REQUEST_APPROVED`/`FREE`), `employeeId`, `employeeName` y `requestId` cuando apliquen

#### Scenario: Empleado intenta acceder al calendario admin
- **GIVEN** un usuario autenticado con rol `EMPLOYEE`
- **WHEN** envía `GET /calendar/admin?weekStart=L`
- **THEN** el sistema responde 403 con `{ error, message, fields, timestamp }`
- **AND** no revela información de otros empleados

### Requirement: "Mi Semana" sin exposición de nombres ajenos
**El sistema DEBE (MUST) devolver a `EMPLOYEE` y `ADMIN` su propia semana con el estado diario de sus recursos y los huecos libres, sin mostrar nombres de otros empleados.**

#### Scenario: Empleado consulta su semana
- **GIVEN** un `EMPLOYEE` autenticado con una `FixedAssignment` activa para varios `day_of_week`
- **WHEN** envía `GET /calendar/my-week` (con o sin `weekStart`)
- **THEN** el sistema responde 200 con un día por fecha y `state` (`ASSIGNED`/`RELEASED`/`REQUEST_PENDING`/`FREE`) referido únicamente a sus propios recursos
- **AND** la respuesta no contiene `employeeName` ni identificadores de otros empleados

#### Scenario: Acceso sin sesión
- **GIVEN** una petición sin sesión válida
- **WHEN** se envía `GET /calendar/my-week`
- **THEN** el sistema responde 401 sin filtrar datos de disponibilidad

## Casos límite (edge cases)
- Recurso con `active = false`: nunca aparece como disponible aunque no tenga asignación, solicitud ni reserva.
- Asignación fija activa para `dayOfWeek(F)` SIN `Release`: el recurso no está disponible para F aunque no haya solicitud ni reserva.
- `Release` existente pero ya cubierto por una `Request` `APPROVED` distinta: el recurso sigue NO disponible (la solicitud aprobada prevalece).
- `VisitorReservation` solo afecta a plazas de parking; los puestos (`DESK`, alcance ampliado) no admiten reservas de visitante, por lo que esa condición no aplica a ellos.
- Solicitudes en estado `PENDING`, `REJECTED` o `CANCELLED` NO ocupan el recurso: solo `APPROVED` cuenta como no disponible.
- `weekStart` que no sea lunes: el sistema usa el lunes de esa semana como inicio (normalización) _[verificar con docs/openapi.yaml]_; `getMyWeek` sin `weekStart` usa la semana actual.
- Consultas de fechas pasadas: permitidas (vista histórica); no se recalcula ni muta nada.

## Dependencias con otras capabilities
- Depende de `auth-local`/`auth-sso` para autenticar y resolver el rol del solicitante.
- Lee el estado producido por `fixed-assignments`, `releases`, `requests` y `visitors` (consulta-only; no las modifica).
- Es consultada por `requests` al validar disponibilidad en la aprobación (lógica compartida de disponibilidad).
- Con `generic-resource-refactor` la disponibilidad se generaliza a `BookableResource` (`PARKING`/`DESK`) sin cambiar el contrato de estos endpoints.
