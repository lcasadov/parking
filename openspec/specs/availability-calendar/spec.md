# availability-calendar Specification

## Purpose
TBD - created by archiving change init-availability-calendar. Update Purpose after archive.
## Requirements
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

