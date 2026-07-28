## MODIFIED Requirements

### Requirement: Calendario semanal completo restringido a ADMIN
**El sistema DEBE (MUST) devolver al `ADMIN` un calendario semanal de todos los recursos con su estado por día y el titular, cubriendo AMBOS tipos de recurso (plazas y puestos) según un parámetro `resourceType`, y DEBE denegar el acceso a usuarios `EMPLOYEE`.**

#### Scenario: Admin consulta el calendario semanal de plazas
- **GIVEN** un `ADMIN` autenticado y un lunes `weekStart`
- **WHEN** envía `GET /calendar/admin?weekStart=L&resourceType=PARKING` (o sin `resourceType`, con default PARKING)
- **THEN** el sistema responde 200 con `rows` (una plaza por fila) y `cells` por día con `state` (`ASSIGNED`/`RELEASED`/`REQUEST_PENDING`/`REQUEST_APPROVED`/`FREE`), `employeeId`, `employeeName` y `requestId` cuando apliquen

#### Scenario: Admin consulta el calendario semanal de puestos
- **GIVEN** un `ADMIN` autenticado y un lunes `weekStart`
- **WHEN** envía `GET /calendar/admin?weekStart=L&resourceType=DESK`
- **THEN** el sistema responde 200 con `rows` (un puesto por fila) y `cells` por día con el mismo conjunto de estados y titular

#### Scenario: Empleado intenta acceder al calendario admin
- **GIVEN** un usuario autenticado con rol `EMPLOYEE`
- **WHEN** envía `GET /calendar/admin?weekStart=L`
- **THEN** el sistema responde 403 con `{ error, message, fields, timestamp }`
- **AND** no revela información de otros empleados

## ADDED Requirements

### Requirement: Conmutador de tipo de recurso en la disponibilidad
La vista de disponibilidad DEBE (MUST) ofrecer un conmutador de tipo de recurso (plaza/puesto) y pasar `resourceType` a `GET /availability`, mostrando los recursos libres del tipo elegido para la fecha. Cada recurso listado DEBE (MUST) mostrarse por su **número/etiqueta** de negocio, nunca por su identificador interno de base de datos.

#### Scenario: El admin cambia a puestos en disponibilidad
- **GIVEN** un `ADMIN` en la vista de disponibilidad con una fecha F seleccionada
- **WHEN** conmuta el tipo de recurso a "Puesto"
- **THEN** la lista se recarga con `GET /availability?date=F&resourceType=DESK` y muestra los puestos libres esa fecha por su número

#### Scenario: La disponibilidad muestra la etiqueta de negocio
- **GIVEN** una lista de recursos disponibles para una fecha
- **WHEN** se renderiza cada fila
- **THEN** la fila identifica el recurso por su número/etiqueta (p. ej. "P-08" o "Puesto 12") y no por el id interno
