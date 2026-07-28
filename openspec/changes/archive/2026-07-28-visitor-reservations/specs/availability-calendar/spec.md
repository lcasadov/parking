## MODIFIED Requirements

### Requirement: Cálculo de disponibilidad para una fecha
**El sistema DEBE (MUST) devolver los recursos disponibles para una fecha F aplicando las cuatro condiciones de disponibilidad (activo, sin asignación fija vigente o liberado, sin solicitud aprobada, y sin reserva de visitante) para AMBOS tipos de recurso (plaza y puesto).**

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
- **GIVEN** una `ParkingSpace` activa, libre de asignación y de solicitud, con una `VisitorReservation` (`resourceType = PARKING`) para esa plaza y fecha F
- **WHEN** se consulta `GET /availability?date=F`
- **THEN** el sistema responde 200 y la plaza NO figura en `availableResources`

#### Scenario: Puesto con reserva de visitante no está disponible
- **GIVEN** un `Desk` activo, libre de asignación y de solicitud, con una `VisitorReservation` (`resourceType = DESK`) para ese puesto y fecha F
- **WHEN** se consulta `GET /availability?date=F&resourceType=DESK`
- **THEN** el sistema responde 200 y el puesto NO figura en `availableResources`

### Requirement: Calendario semanal completo restringido a ADMIN
**El sistema DEBE (MUST) devolver al `ADMIN` un calendario semanal de todos los recursos con su estado por día y el titular u ocupante, y DEBE denegar el acceso a usuarios `EMPLOYEE`.**

#### Scenario: Admin consulta el calendario semanal
- **GIVEN** un `ADMIN` autenticado y un lunes `weekStart`
- **WHEN** envía `GET /calendar/admin?weekStart=L`
- **THEN** el sistema responde 200 con `rows` (un recurso por fila) y `cells` por día con `state` (`ASSIGNED`/`RELEASED`/`REQUEST_PENDING`/`REQUEST_APPROVED`/`VISITOR_RESERVATION`/`FREE`), `employeeId`, `employeeName` y `requestId` cuando apliquen

#### Scenario: Celda ocupada por una reserva de visitante
- **GIVEN** un `ADMIN` autenticado y un recurso (plaza o puesto) con una `VisitorReservation` para un día de la semana consultada
- **WHEN** envía `GET /calendar/admin?weekStart=L`
- **THEN** la celda de ese día responde con `state = VISITOR_RESERVATION`, `employeeId = null` y `employeeName` con el nombre del visitante
- **AND** esa celda se cuenta como ocupada (no como libre) y no ofrece acción de liberar

#### Scenario: Empleado intenta acceder al calendario admin
- **GIVEN** un usuario autenticado con rol `EMPLOYEE`
- **WHEN** envía `GET /calendar/admin?weekStart=L`
- **THEN** el sistema responde 403 con `{ error, message, fields, timestamp }`
- **AND** no revela información de otros empleados
