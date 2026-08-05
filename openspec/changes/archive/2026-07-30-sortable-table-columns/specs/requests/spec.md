## MODIFIED Requirements

### Requirement: Listados de solicitudes según rol
**El sistema DEBE (MUST) permitir al empleado listar únicamente sus propias solicitudes y al administrador listar las pendientes y las solicitudes por estado, con un orden por defecto estable y admitiendo ordenación por columnas permitidas (`requestedDate`, `createdAt`) vía el `Sort` del `Pageable`.**

#### Scenario: El empleado lista solo sus solicitudes
- **GIVEN** un `Employee` autenticado con rol `EMPLOYEE` con solicitudes propias y ajenas en el sistema
- **WHEN** envía `GET /requests/mine`
- **THEN** el sistema responde 200 con una página que contiene solo las solicitudes cuyo `employee_id == session.employee_id`

#### Scenario: El administrador lista pendientes en orden FIFO
- **GIVEN** un `Employee` autenticado con rol `ADMIN` y varias solicitudes en estado `PENDING`
- **WHEN** envía `GET /requests/pending` sin parámetro `sort`
- **THEN** el sistema responde 200 con la página ordenada por `created_at ASC` (FIFO por defecto)

#### Scenario: El administrador ordena las pendientes por fecha solicitada
- **GIVEN** un `Employee` autenticado con rol `ADMIN` y varias solicitudes en estado `PENDING`
- **WHEN** envía `GET /requests/pending?sort=requestedDate,desc`
- **THEN** el sistema responde 200 con la página ordenada por `requested_date DESC`

#### Scenario: El administrador ordena la lista por estado por fecha de creación
- **GIVEN** un `Employee` autenticado con rol `ADMIN`
- **WHEN** envía `GET /requests?status=APPROVED&sort=createdAt,asc`
- **THEN** el sistema responde 200 con la página de solicitudes `APPROVED` ordenada por `created_at ASC`

#### Scenario: Un campo de orden no permitido cae al orden por defecto
- **GIVEN** un `Employee` autenticado con rol `ADMIN`
- **WHEN** envía `GET /requests/pending?sort=details,asc` (campo fuera de la whitelist)
- **THEN** el sistema responde 200 aplicando el orden por defecto (`created_at ASC`) e ignora el criterio no permitido

#### Scenario: Un empleado intenta listar las pendientes
- **GIVEN** un `Employee` autenticado con rol `EMPLOYEE`
- **WHEN** envía `GET /requests/pending`
- **THEN** el sistema responde 403 sin revelar datos de otras solicitudes
