# requests

## MODIFIED Requirements

### Requirement: Creación de solicitud con ventana y unicidad
**El sistema DEBE (MUST) crear una solicitud para un empleado y una fecha DESDE HOY EN ADELANTE (hoy o cualquier fecha futura, sin límite superior; NO se permiten fechas anteriores a hoy), garantizando una única solicitud `PENDING` por empleado, tipo de recurso y fecha, y DEBE ramificar el estado inicial según el parámetro global `approvalMode`: en modo `MANUAL` la solicitud nace `PENDING` (con `resource_id = NULL`); en modo `AUTOMATIC` la solicitud nace `APPROVED` con recurso asignado (plaza auto-asignada o puesto elegido).**

#### Scenario: Creación para cualquier fecha futura en modo MANUAL
- **GIVEN** el parámetro global `approvalMode = MANUAL` y un `Employee` autenticado con rol `EMPLOYEE` sin solicitud `PENDING` para `requested_date`
- **WHEN** envía `POST /requests` con `requested_date` igual a hoy o a cualquier fecha futura (sin límite superior)
- **THEN** el sistema responde 201 con la solicitud en estado `PENDING` y `resource_id = NULL`
- **AND** registra `created_at` con la marca temporal actual

#### Scenario: Fecha pasada rechazada
- **GIVEN** un `Employee` autenticado con rol `EMPLOYEE`
- **WHEN** envía `POST /requests` con `requested_date` anterior a hoy
- **THEN** el sistema responde 400 con `error = OUTSIDE_REQUEST_WINDOW` y `fields` indicando `requested_date`
- **AND** no crea ninguna solicitud

#### Scenario: Solicitud PENDING duplicada para la misma fecha
- **GIVEN** un `Employee` con una solicitud `PENDING` para `requested_date` y el mismo tipo de recurso
- **WHEN** envía `POST /requests` con la misma `requested_date` y tipo
- **THEN** el sistema responde 409 con `error = REQUEST_ALREADY_PENDING`
- **AND** no crea una segunda solicitud
