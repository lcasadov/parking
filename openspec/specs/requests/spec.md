# requests Specification

## Purpose
TBD - created by archiving change init-requests. Update Purpose after archive.
## Requirements
### Requirement: Creación de solicitud con ventana y unicidad
**El sistema DEBE (MUST) crear una solicitud en estado `PENDING` (con `parking_space_id = NULL`) para un empleado y una fecha dentro de la ventana hoy..hoy+14 días, garantizando una única solicitud `PENDING` por empleado y fecha.**

#### Scenario: Creación dentro de la ventana
- **GIVEN** un `Employee` autenticado con rol `EMPLOYEE` y sin solicitud `PENDING` para `requested_date`
- **WHEN** envía `POST /requests` con `requested_date` entre hoy y hoy+14 días
- **THEN** el sistema responde 201 con la solicitud en estado `PENDING` y `parking_space_id = NULL`
- **AND** registra `created_at` con la marca temporal actual

#### Scenario: Fecha fuera de la ventana
- **GIVEN** un `Employee` autenticado con rol `EMPLOYEE`
- **WHEN** envía `POST /requests` con `requested_date` anterior a hoy o posterior a hoy+14 días
- **THEN** el sistema responde 400 con `error = OUTSIDE_REQUEST_WINDOW` y `fields` indicando `requested_date`
- **AND** no crea ninguna solicitud

#### Scenario: Solicitud PENDING duplicada para la misma fecha
- **GIVEN** un `Employee` con una solicitud `PENDING` para `requested_date`
- **WHEN** envía `POST /requests` con la misma `requested_date`
- **THEN** el sistema responde 409 con `error = REQUEST_ALREADY_PENDING`
- **AND** no crea una segunda solicitud

### Requirement: Listados de solicitudes según rol
**El sistema DEBE (MUST) permitir al empleado listar únicamente sus propias solicitudes y al administrador listar las pendientes en orden FIFO y ver el detalle de cualquier solicitud.**

#### Scenario: El empleado lista solo sus solicitudes
- **GIVEN** un `Employee` autenticado con rol `EMPLOYEE` con solicitudes propias y ajenas en el sistema
- **WHEN** envía `GET /requests/mine`
- **THEN** el sistema responde 200 con una página que contiene solo las solicitudes cuyo `employee_id == session.employee_id`

#### Scenario: El administrador lista pendientes en orden FIFO
- **GIVEN** un `Employee` autenticado con rol `ADMIN` y varias solicitudes en estado `PENDING`
- **WHEN** envía `GET /requests/pending`
- **THEN** el sistema responde 200 con la página ordenada por `created_at ASC`

#### Scenario: Un empleado intenta listar las pendientes
- **GIVEN** un `Employee` autenticado con rol `EMPLOYEE`
- **WHEN** envía `GET /requests/pending`
- **THEN** el sistema responde 403 sin revelar datos de otras solicitudes

### Requirement: Cancelación por el dueño en estado PENDING
**El sistema DEBE (MUST) permitir al empleado cancelar su propia solicitud únicamente mientras está en estado `PENDING`, transicionándola a `CANCELLED`.**

#### Scenario: Cancelación de solicitud propia en PENDING
- **GIVEN** un `Employee` autenticado dueño de una solicitud en estado `PENDING`
- **WHEN** envía `POST /requests/{id}/cancel`
- **THEN** el sistema responde 200 con la solicitud en estado `CANCELLED`

#### Scenario: Cancelación de una solicitud ya resuelta
- **GIVEN** un `Employee` dueño de una solicitud en estado `APPROVED` (o `REJECTED`)
- **WHEN** envía `POST /requests/{id}/cancel`
- **THEN** el sistema responde 409 (transición no permitida desde estado terminal)
- **AND** no modifica la solicitud

#### Scenario: Cancelación de la solicitud de otro empleado
- **GIVEN** un `Employee` autenticado que NO es el dueño de la solicitud
- **WHEN** envía `POST /requests/{id}/cancel`
- **THEN** el sistema responde 403 (la comprobación de objeto `request.employee_id == session.employee_id` falla)

### Requirement: Aprobación con validación de disponibilidad y concurrencia
**El sistema DEBE (MUST) permitir al administrador aprobar una solicitud `PENDING` asignando una plaza disponible, validando la disponibilidad real para la fecha y resolviendo los conflictos de concurrencia entre administradores.**

#### Scenario: Aprobación con plaza disponible
- **GIVEN** un `Employee` autenticado con rol `ADMIN` y una solicitud en estado `PENDING`
- **WHEN** envía `POST /requests/{id}/approve` con `{ parkingSpaceId, approvalNote }` y la plaza está disponible para `requested_date`
- **THEN** el sistema responde 200 con la solicitud en estado `APPROVED`, `parking_space_id` asignado, `resolved_by_id` y `resolved_at` informados
- **AND** dispara la notificación de aprobación al empleado con `approval_note` en el email

#### Scenario: Aprobación con plaza no disponible
- **GIVEN** un `Employee` con rol `ADMIN` y una solicitud `PENDING`
- **WHEN** envía `POST /requests/{id}/approve` con una plaza que no está disponible para `requested_date`
- **THEN** el sistema responde 409 (sin disponibilidad)
- **AND** la solicitud permanece en `PENDING`

#### Scenario: Concurrencia entre administradores sobre la misma plaza
- **GIVEN** dos administradores aprobando simultáneamente solicitudes que reclaman la misma plaza para la misma `requested_date`
- **WHEN** la segunda aprobación se confirma tras la primera
- **THEN** el sistema responde 409 a la segunda (colisión detectada por el índice/restricción de disponibilidad)
- **AND** solo la primera queda `APPROVED`

### Requirement: Rechazo con catálogo de motivos
**El sistema DEBE (MUST) permitir al administrador rechazar una solicitud `PENDING` indicando un `rejection_reason_code` del catálogo, exigiendo `rejection_reason` (≥5 caracteres) cuando el código es `OTHER`.**

#### Scenario: Rechazo con motivo del catálogo
- **GIVEN** un `Employee` con rol `ADMIN` y una solicitud en estado `PENDING`
- **WHEN** envía `POST /requests/{id}/reject` con `rejectionReasonCode = NO_AVAILABILITY`
- **THEN** el sistema responde 200 con la solicitud en estado `REJECTED`, `resolved_by_id` y `resolved_at` informados
- **AND** dispara la notificación de rechazo al empleado

#### Scenario: Rechazo OTHER sin texto libre
- **GIVEN** un `Employee` con rol `ADMIN` y una solicitud `PENDING`
- **WHEN** envía `POST /requests/{id}/reject` con `rejectionReasonCode = OTHER` y `rejectionReason` ausente o de menos de 5 caracteres
- **THEN** el sistema responde 400 con `error` de validación y `fields` indicando `rejectionReason`
- **AND** la solicitud permanece en `PENDING`

### Requirement: Cobertura E2E de los flujos de solicitud y resolución
**Los flujos de solicitud de recurso por el empleado y de resolución por el administrador DEBEN (MUST) estar cubiertos por tests end-to-end (Playwright) que ejerciten la aplicación real (SPA + backend), incluyendo la vista de plano en viewport móvil.**

#### Scenario: E2E — empleado solicita plaza y puesto
- **GIVEN** un `EMPLOYEE` autenticado en la aplicación
- **WHEN** solicita una plaza y un puesto para la misma fecha
- **THEN** el test verifica que ambas solicitudes quedan registradas como pendientes del empleado

#### Scenario: E2E — admin aprueba y rechaza
- **GIVEN** un `ADMIN` autenticado con solicitudes pendientes
- **WHEN** aprueba una solicitud (asignando recurso) y rechaza otra
- **THEN** el test verifica que la aprobada queda `APPROVED` con recurso y la rechazada `REJECTED`

#### Scenario: E2E — solicitud desde el plano en móvil
- **GIVEN** un `EMPLOYEE` en viewport móvil con puestos libres para la fecha
- **WHEN** pulsa "Solicitar" en la lista "Disponibles para solicitar" del plano
- **THEN** el test verifica el feedback de éxito y la creación de la solicitud del puesto


### Requirement: Resolución de solicitudes según el tipo de recurso en la UI admin
**La bandeja de solicitudes pendientes DEBE (MUST) mostrar el tipo de recurso (plaza o puesto) de cada solicitud, y el modal de aprobación DEBE (MUST) adaptarse a ese tipo, ofreciendo la lista de recursos disponibles del tipo correcto para la fecha solicitada.**

#### Scenario: Aprobar una solicitud de puesto
- **GIVEN** un `ADMIN` con una solicitud pendiente de tipo `DESK`
- **WHEN** abre el modal de aprobación
- **THEN** el modal indica que es una solicitud de puesto y ofrece los **puestos disponibles** para la fecha (no plazas)
- **AND** al confirmar, asigna el puesto elegido y la solicitud queda `APPROVED`

#### Scenario: Aprobar una solicitud de plaza
- **GIVEN** un `ADMIN` con una solicitud pendiente de tipo `PARKING`
- **WHEN** abre el modal de aprobación
- **THEN** el modal ofrece las **plazas disponibles** para la fecha
- **AND** al confirmar, asigna la plaza y la solicitud queda `APPROVED`

#### Scenario: El tipo es visible en los listados
- **GIVEN** solicitudes de plaza y de puesto
- **WHEN** el admin ve la bandeja de pendientes (o el empleado ve "Mis solicitudes")
- **THEN** cada fila muestra si la solicitud es de plaza o de puesto
