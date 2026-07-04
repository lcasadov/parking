# Capability: requests

## Resumen
Solicitud puntual de plaza de aparcamiento por parte de un empleado para una
fecha concreta, con ciclo de vida de aprobación/rechazo gestionado por el
administrador. Cubre creación con ventana temporal y unicidad, listados
(propios y pendientes FIFO), cancelación por el empleado y resolución
(aprobar/rechazar) por el administrador.

## Fase
🟢🔵 Ambas fases (comportamiento idéntico; en la fase ampliada de puestos la clave de unicidad incorpora el tipo de recurso, ver `generic-resource-refactor`).

## Reglas de negocio implicadas
(README §"Reglas de negocio" + `docs/data-model.md` §3.5; NO hay códigos RN-xx)
- Ventana de solicitud: `requested_date` debe estar entre hoy y hoy+14 días naturales → 400 `OUTSIDE_REQUEST_WINDOW`.
- Una sola solicitud `PENDING` por empleado y fecha (índice único filtrado `UX_requests_employee_date_pending`) → 409 `REQUEST_ALREADY_PENDING`; las filas `REJECTED`/`CANCELLED` no bloquean nuevas solicitudes.
- Estado inicial `PENDING` con `parking_space_id = NULL`.
- Máquina de estados: `PENDING` → `APPROVED` | `REJECTED` | `CANCELLED`. No hay transiciones desde estados terminales.
- Aprobación: el administrador asigna `parking_space_id`; el sistema valida disponibilidad real de la plaza esa fecha → 409 si no disponible o por colisión de concurrencia entre administradores. `approval_note` (opcional) viaja en el email de aprobación.
- Rechazo: requiere `rejection_reason_code` del catálogo (`NO_AVAILABILITY` / `OUTSIDE_POLICY` / `OTHER`); `rejection_reason` (texto libre) es obligatorio (≥5 caracteres) cuando el código es `OTHER`, opcional en otro caso.
- Cancelación: solo el dueño de la solicitud, y solo mientras está en `PENDING`.
- Listado de pendientes en orden FIFO (`created_at ASC`).
- Comprobación de pertenencia a nivel de objeto en listados/cancelación propios (`request.employee_id == session.employee_id`).

## Entidades implicadas
- Request (`requests`: `employee_id`, `requested_date`, `status`, `parking_space_id`, `approval_note`, `rejection_reason_code`, `rejection_reason`, `resolved_by_id`, `resolved_at`, `created_at`)
- Employee (solicitante y resolutor)
- ParkingSpace (plaza asignada al aprobar)
- FixedAssignment, Release, VisitorReservation (consultadas para el cálculo de disponibilidad al aprobar)

## Endpoints
- POST /api/v1/requests (operationId: `createRequest`) — EMPLOYEE
- GET /api/v1/requests/mine (operationId: `listMyRequests`) — EMPLOYEE
- GET /api/v1/requests/pending (operationId: `listPendingRequests`) — ADMIN
- GET /api/v1/requests/{id} (operationId: `getRequest`) — ADMIN
- POST /api/v1/requests/{id}/cancel (operationId: `cancelRequest`) — EMPLOYEE
- POST /api/v1/requests/{id}/approve (operationId: `approveRequest`) — ADMIN
- POST /api/v1/requests/{id}/reject (operationId: `rejectRequest`) — ADMIN

## Permisos
| Rol | Permisos |
|---|---|
| ADMIN | Listar solicitudes pendientes (FIFO), ver detalle de cualquier solicitud, aprobar (asignando plaza), rechazar (con motivo). |
| EMPLOYEE | Crear solicitud propia, listar solo las propias, cancelar la propia solicitud mientras está en `PENDING`. |

## ADDED Requirements
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

## Casos límite (edge cases)
- Aprobar o rechazar una solicitud que ya no está en `PENDING` (cancelada o resuelta por otro admin): 409 (transición no permitida).
- `requested_date` exactamente hoy o exactamente hoy+14: dentro de la ventana (extremos inclusive).
- Una solicitud `REJECTED` o `CANCELLED` para una fecha no impide crear una nueva solicitud `PENDING` para esa misma fecha (el índice único solo aplica a `PENDING`).
- `getRequest` sobre un `id` inexistente: 404.
- El catálogo de `rejection_reason_code` es un conjunto inicial pendiente de confirmar con negocio _[verificar con docs/data-model.md]_.

## Dependencias con otras capabilities
- Depende de `auth-local` / `auth-sso` para autenticación y de RBAC (`ADMIN`/`EMPLOYEE`).
- Consulta `availability-calendar` (y por tanto `fixed-assignments`, `releases`, `visitors`) para validar disponibilidad de la plaza al aprobar.
- Dispara `notifications` (nueva solicitud → admins; aprobada/rechazada → empleado).
- Sus operaciones quedan registradas en `audit-retention`.
- En la fase ampliada, `generic-resource-refactor` generaliza `parking_space_id → resource_id` e incorpora el tipo de recurso a la clave de unicidad.
