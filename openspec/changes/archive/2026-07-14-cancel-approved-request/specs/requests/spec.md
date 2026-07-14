## MODIFIED Requirements

### Requirement: Cancelación por el dueño en estado PENDING
**El sistema DEBE (MUST) permitir al empleado cancelar su propia solicitud transicionándola a `CANCELLED` en dos casos: (a) mientras está en estado `PENDING`, para cualquier fecha; y (b) mientras está en estado `APPROVED` cuya `requested_date` es futura (hoy o posterior), en cuyo caso la cancelación DEBE liberar el recurso asignado (plaza/puesto), que vuelve a estar disponible para esa fecha. El sistema NO DEBE (MUST NOT) permitir cancelar una solicitud `APPROVED` con `requested_date` pasada, ni una solicitud en estado terminal (`REJECTED`/`CANCELLED`), ni una solicitud ajena. La cancelación de una solicitud `APPROVED` DEBE quedar registrada en auditoría como liberación del recurso por el empleado.**

#### Scenario: Cancelación de solicitud propia en PENDING
- **GIVEN** un `Employee` autenticado dueño de una solicitud en estado `PENDING`
- **WHEN** envía `POST /requests/{id}/cancel`
- **THEN** el sistema responde 200 con la solicitud en estado `CANCELLED`

#### Scenario: Cancelación de solicitud propia APROBADA con fecha futura libera el recurso
- **GIVEN** un `Employee` autenticado dueño de una solicitud en estado `APPROVED` con un recurso asignado y `requested_date` igual a hoy o posterior
- **WHEN** envía `POST /requests/{id}/cancel`
- **THEN** el sistema responde 200 con la solicitud en estado `CANCELLED`
- **AND** el recurso (plaza/puesto) deja de figurar como `APPROVED` y vuelve a estar disponible para `requested_date`
- **AND** registra la cancelación en auditoría como liberación del recurso por el empleado

#### Scenario: No se puede cancelar una solicitud APROBADA con fecha pasada
- **GIVEN** un `Employee` autenticado dueño de una solicitud en estado `APPROVED` cuya `requested_date` es anterior a hoy
- **WHEN** envía `POST /requests/{id}/cancel`
- **THEN** el sistema responde 409 (transición no permitida sobre una fecha pasada)
- **AND** no modifica la solicitud ni la disponibilidad del recurso

#### Scenario: Cancelación de una solicitud en estado terminal
- **GIVEN** un `Employee` dueño de una solicitud en estado `REJECTED` (o `CANCELLED`)
- **WHEN** envía `POST /requests/{id}/cancel`
- **THEN** el sistema responde 409 (transición no permitida desde estado terminal)
- **AND** no modifica la solicitud

#### Scenario: Cancelación de la solicitud de otro empleado
- **GIVEN** un `Employee` autenticado que NO es el dueño de la solicitud
- **WHEN** envía `POST /requests/{id}/cancel`
- **THEN** el sistema responde 403 (la comprobación de objeto `request.employee_id == session.employee_id` falla)

## ADDED Requirements

### Requirement: Botón Cancelar disponible para solicitudes aprobadas con fecha futura en "Mis solicitudes"
**En la vista "Mis solicitudes" del empleado, el botón **Cancelar** DEBE (MUST) mostrarse para las solicitudes en estado `PENDING` y también para las solicitudes en estado `APPROVED` cuya `requested_date` es futura (hoy o posterior). El botón NO DEBE (MUST NOT) mostrarse para solicitudes `APPROVED` con `requested_date` pasada, ni para solicitudes en estado `REJECTED` o `CANCELLED`.**

#### Scenario: El empleado ve Cancelar en una solicitud aprobada futura
- **GIVEN** un `Employee` en "Mis solicitudes" con una solicitud en estado `APPROVED` y `requested_date` igual a hoy o posterior
- **WHEN** se renderiza la fila de esa solicitud
- **THEN** la fila muestra el botón **Cancelar** habilitado

#### Scenario: El empleado no ve Cancelar en una solicitud aprobada pasada
- **GIVEN** un `Employee` en "Mis solicitudes" con una solicitud en estado `APPROVED` y `requested_date` anterior a hoy
- **WHEN** se renderiza la fila de esa solicitud
- **THEN** la fila NO muestra el botón **Cancelar**

#### Scenario: El empleado no ve Cancelar en solicitudes resueltas terminalmente
- **GIVEN** un `Employee` en "Mis solicitudes" con solicitudes en estado `REJECTED` y `CANCELLED`
- **WHEN** se renderizan esas filas
- **THEN** ninguna de esas filas muestra el botón **Cancelar**
