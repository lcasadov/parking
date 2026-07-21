# requests Specification

## Purpose
TBD - created by archiving change init-requests. Update Purpose after archive.
## Requirements
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
**El sistema DEBE (MUST) permitir al administrador rechazar una solicitud indicando un `rejection_reason_code` del catálogo, exigiendo `rejection_reason` (≥5 caracteres) cuando el código es `OTHER`. El rechazo DEBE (MUST) admitirse tanto desde `PENDING` como desde `APPROVED` (solicitud auto-aprobada en modo `AUTOMATIC`); al rechazar una solicitud `APPROVED` el recurso asignado DEBE quedar liberado y disponible de nuevo para esa fecha.**

#### Scenario: Rechazo de solicitud PENDING con motivo del catálogo
- **GIVEN** un `Employee` con rol `ADMIN` y una solicitud en estado `PENDING`
- **WHEN** envía `POST /requests/{id}/reject` con `rejectionReasonCode = NO_AVAILABILITY`
- **THEN** el sistema responde 200 con la solicitud en estado `REJECTED`, `resolved_by_id` y `resolved_at` informados
- **AND** dispara la notificación de rechazo al empleado

#### Scenario: Rechazo OTHER sin texto libre
- **GIVEN** un `Employee` con rol `ADMIN` y una solicitud rechazable
- **WHEN** envía `POST /requests/{id}/reject` con `rejectionReasonCode = OTHER` y `rejectionReason` ausente o de menos de 5 caracteres
- **THEN** el sistema responde 400 con `error` de validación y `fields` indicando `rejectionReason`
- **AND** la solicitud no cambia de estado

#### Scenario: Rechazo posterior de una solicitud auto-aprobada libera el recurso
- **GIVEN** un `Employee` con rol `ADMIN` y una solicitud auto-aprobada en estado `APPROVED` con una plaza asignada para `requested_date`
- **WHEN** envía `POST /requests/{id}/reject` con un `rejectionReasonCode` válido
- **THEN** el sistema responde 200 con la solicitud en estado `REJECTED`, `resolved_by_id` y `resolved_at` informados
- **AND** la plaza deja de figurar como `APPROVED` y vuelve a estar disponible para `requested_date`
- **AND** registra la acción en auditoría y dispara la notificación de rechazo

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

### Requirement: Auto-asignación de plaza por categoría y planta en modo AUTOMATIC
**Cuando `approvalMode = AUTOMATIC` y un empleado solicita una PLAZA, el sistema DEBE (MUST) asignar automáticamente una plaza LIBRE para la fecha eligiéndola por la categoría del empleado y la planta de la plaza (planta = número de plaza / 1000), y la solicitud DEBE nacer en estado `APPROVED` con la plaza asignada. Las categorías altas hasta Director nivel 2 (CEO, Consejo, Director N1, Director N2) DEBEN preferir las plantas más altas disponibles (probando de la más alta a la más baja); el resto de categorías (Gerente, Mando intermedio, Empleado) DEBEN preferir las plantas más bajas (de la más baja a la más alta), cayendo a la siguiente planta según el orden de preferencia cuando la preferida no tiene plazas libres.**

#### Scenario: Categoría alta recibe la planta más alta disponible
- **GIVEN** `approvalMode = AUTOMATIC`, un `Employee` de categoría `DIRECTOR_N2` y plazas libres en las plantas 5 y 1 para `requested_date`
- **WHEN** envía `POST /requests` de tipo `PARKING`
- **THEN** el sistema responde 201 con la solicitud en estado `APPROVED` y una plaza de la planta 5 asignada

#### Scenario: Categoría base recibe la planta más baja disponible
- **GIVEN** `approvalMode = AUTOMATIC`, un `Employee` de categoría `EMPLEADO` y plazas libres en las plantas 5 y 1 para `requested_date`
- **WHEN** envía `POST /requests` de tipo `PARKING`
- **THEN** el sistema responde 201 con la solicitud en estado `APPROVED` y una plaza de la planta 1 asignada

#### Scenario: Fallback a la siguiente planta según preferencia
- **GIVEN** `approvalMode = AUTOMATIC`, un `Employee` de categoría `DIRECTOR_N1` y sin plazas libres en la planta 5 pero con plazas libres en la planta 4 para `requested_date`
- **WHEN** envía `POST /requests` de tipo `PARKING`
- **THEN** el sistema responde 201 con la solicitud en estado `APPROVED` y una plaza de la planta 4 asignada

#### Scenario: Sin ninguna plaza libre no se puede auto-asignar
- **GIVEN** `approvalMode = AUTOMATIC`, un `Employee` de cualquier categoría y ninguna plaza libre en ninguna planta para `requested_date`
- **WHEN** envía `POST /requests` de tipo `PARKING`
- **THEN** el sistema responde 409 con `error = NO_AVAILABILITY`
- **AND** no crea ninguna solicitud

#### Scenario: Concurrencia sobre la misma plaza auto-asignada
- **GIVEN** `approvalMode = AUTOMATIC` y dos empleados cuyo algoritmo selecciona simultáneamente la misma plaza para la misma `requested_date`
- **WHEN** la segunda solicitud se confirma tras la primera
- **THEN** el sistema responde 409 a la segunda (colisión detectada por el índice único filtrado `APPROVED`)
- **AND** solo la primera solicitud queda `APPROVED` con esa plaza

### Requirement: Auto-aprobación de solicitud de puesto en modo AUTOMATIC
**Cuando `approvalMode = AUTOMATIC` y un empleado solicita un PUESTO de oficina, el sistema DEBE (MUST) tomar el puesto ELEGIDO por el empleado (no hay asignación automática de puesto por planta) y crear la solicitud directamente en estado `APPROVED` con ese puesto, siempre que el puesto esté disponible para la fecha.**

#### Scenario: El puesto elegido se auto-aprueba
- **GIVEN** `approvalMode = AUTOMATIC`, un `Employee` autenticado y un puesto disponible para `requested_date`
- **WHEN** envía `POST /requests` de tipo `DESK` eligiendo ese puesto
- **THEN** el sistema responde 201 con la solicitud en estado `APPROVED` y el puesto elegido asignado

#### Scenario: El puesto elegido no está disponible
- **GIVEN** `approvalMode = AUTOMATIC`, un `Employee` autenticado y un puesto ya ocupado (`APPROVED` o reservado) para `requested_date`
- **WHEN** envía `POST /requests` de tipo `DESK` eligiendo ese puesto
- **THEN** el sistema responde 409 (sin disponibilidad)
- **AND** no crea ninguna solicitud

### Requirement: Elección de un puesto concreto en la solicitud unificada
El modal de solicitud unificada DEBE (MUST) permitir al empleado, cuando el recurso seleccionado incluye PUESTO, **elegir un puesto concreto** abriendo el plano de la planta como selector, y DEBE enviar el `resourceId` del puesto elegido en `POST /requests`. El modal DEBE (MUST) mostrar el **número** del puesto elegido (`Desk.number`), nunca su identificador interno. La elección de puesto es opcional: sin puesto elegido la solicitud se envía sin `resourceId` (comportamiento actual).

#### Scenario: El empleado abre el selector de puesto desde el modal
- **GIVEN** un empleado en el modal de solicitud con el recurso PUESTO seleccionado y una fecha dentro de la ventana
- **WHEN** pulsa el botón "Seleccionar puesto"
- **THEN** se abre el plano de la planta como selector para esa fecha
- **AND** el plano muestra los puestos con su disponibilidad para la fecha elegida

#### Scenario: Al elegir un puesto se muestra su número en el modal
- **GIVEN** el selector de plano abierto desde el modal de solicitud
- **WHEN** el empleado pincha un puesto libre en el plano
- **THEN** el selector se cierra y el modal muestra el **número** del puesto elegido (`Desk.number`)
- **AND** el modal no muestra en ningún momento el identificador interno del puesto

#### Scenario: El envío incluye el resourceId del puesto elegido
- **GIVEN** un empleado con un puesto elegido en el modal para una fecha dentro de la ventana
- **WHEN** confirma el envío de la solicitud
- **THEN** el sistema envía `POST /requests` con `resourceType = DESK` y `resourceId` igual al puesto elegido

#### Scenario: Solicitud de puesto sin elegir puesto concreto
- **GIVEN** un empleado con el recurso PUESTO seleccionado que no ha elegido ningún puesto en el plano
- **WHEN** confirma el envío de la solicitud
- **THEN** el sistema envía `POST /requests` con `resourceType = DESK` y sin `resourceId` (la solicitud nace `PENDING` en modo manual, como hasta ahora)

### Requirement: Auto-aprobación del puesto elegido en modo AUTOMATIC desde el modal
Cuando `approvalMode = AUTOMATIC` y la solicitud de PUESTO incluye un `resourceId`, el sistema DEBE (MUST) crear la solicitud directamente en estado `APPROVED` con ese puesto (siempre que esté disponible para la fecha), reflejando en la UI el feedback de asignación inmediata; en modo `MANUAL` el `resourceId` se ignora y la solicitud nace `PENDING`.

#### Scenario: Puesto elegido auto-aprobado en modo automático
- **GIVEN** `approvalMode = AUTOMATIC`, un empleado autenticado y un puesto disponible para `requested_date`
- **WHEN** envía `POST /requests` con `resourceType = DESK` y el `resourceId` del puesto elegido
- **THEN** el sistema responde 201 con la solicitud en estado `APPROVED` y el puesto elegido asignado

#### Scenario: Puesto elegido no disponible en modo automático
- **GIVEN** `approvalMode = AUTOMATIC` y un puesto ya ocupado (`ASSIGNED`, `REQUESTED` o `APPROVED`) para `requested_date`
- **WHEN** el empleado envía `POST /requests` con `resourceType = DESK` y el `resourceId` de ese puesto
- **THEN** el sistema responde 409 por disponibilidad y no crea la solicitud

#### Scenario: El resourceId se ignora en modo manual
- **GIVEN** `approvalMode = MANUAL` y un empleado que elige un puesto concreto en el modal
- **WHEN** envía `POST /requests` con `resourceType = DESK` y `resourceId`
- **THEN** el sistema responde 201 con la solicitud en estado `PENDING` y `resource_id = NULL` (el ADMIN resolverá la asignación)

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

