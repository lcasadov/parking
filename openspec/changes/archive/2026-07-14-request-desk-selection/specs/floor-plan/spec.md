## MODIFIED Requirements

### Requirement: Solicitud directa desde el plano
**El sistema DEBE (MUST) permitir al empleado solicitar un puesto pinchándolo en el plano solo si está libre para la fecha, generando un `Request` equivalente al del flujo normal. El estado inicial del `Request` DEBE (MUST) ramificar según el parámetro global `approvalMode`: en modo `MANUAL` nace `PENDING`; en modo `AUTOMATIC` el puesto pinchado se auto-aprueba y el `Request` nace `APPROVED` con ese puesto, de forma coherente con `POST /requests`.**

#### Scenario: Solicitud de un puesto libre en modo MANUAL
- **GIVEN** `approvalMode = MANUAL`, un empleado autenticado y un `Desk` con `state = FREE` para una fecha dentro de la ventana
- **WHEN** envía `POST /floor-plan/desks/{deskId}/request` con `{ date }`
- **THEN** el sistema crea un `Request` (`PENDING`, `resourceType = DESK`) para ese puesto y fecha
- **AND** responde 201 con el `requestId` y el nuevo estado del puesto

#### Scenario: Solicitud de un puesto libre en modo AUTOMATIC se auto-aprueba
- **GIVEN** `approvalMode = AUTOMATIC`, un empleado autenticado y un `Desk` con `state = FREE` para una fecha dentro de la ventana
- **WHEN** envía `POST /floor-plan/desks/{deskId}/request` con `{ date }`
- **THEN** el sistema crea un `Request` (`resourceType = DESK`) para ese puesto y fecha en estado `APPROVED` con el puesto asignado
- **AND** responde 201 con el `requestId` y el nuevo estado del puesto

#### Scenario: Solicitud de un puesto no libre
- **GIVEN** un empleado autenticado y un `Desk` que ya está `ASSIGNED` o `REQUESTED` para la fecha
- **WHEN** envía `POST /floor-plan/desks/{deskId}/request`
- **THEN** el sistema responde 409 con `error` de disponibilidad (puesto no libre)
- **AND** no crea ningún `Request`

#### Scenario: Solicitud duplicada pendiente para la misma fecha
- **GIVEN** un empleado con un `Request` `PENDING` de puesto para esa misma fecha
- **WHEN** envía `POST /floor-plan/desks/{deskId}/request` para esa fecha
- **THEN** el sistema responde 409 con `error = REQUEST_ALREADY_PENDING`
- **AND** no crea un segundo `Request`

#### Scenario: Concurrencia sobre el mismo puesto auto-aprobado
- **GIVEN** `approvalMode = AUTOMATIC` y dos empleados que pinchan simultáneamente el mismo `Desk` libre para la misma fecha
- **WHEN** ambos envían `POST /floor-plan/desks/{deskId}/request`
- **THEN** el sistema responde 409 a la segunda (colisión detectada por el índice único filtrado `APPROVED`)
- **AND** solo la primera solicitud queda `APPROVED` con ese puesto

## ADDED Requirements

### Requirement: Plano como selector de puesto en la solicitud
El plano DEBE (MUST) poder abrirse en modo **selector** desde el modal de solicitud, para una fecha dada, de modo que al pinchar un puesto libre devuelva el puesto elegido (identificador y **número**) al modal en lugar de crear una solicitud, y cierre la ventana del plano. En modo selector, pinchar un puesto NO DEBE (MUST NOT) invocar `POST /floor-plan/desks/{deskId}/request` (la creación de la solicitud la realiza el modal al enviar).

#### Scenario: Elegir un puesto libre en modo selector
- **GIVEN** el plano abierto en modo selector para una fecha, desde el modal de solicitud
- **WHEN** el empleado pincha un puesto con `state = FREE`
- **THEN** el plano devuelve al modal el identificador y el **número** del puesto elegido
- **AND** la ventana del plano se cierra
- **AND** no se envía ninguna petición de creación de solicitud desde el plano

#### Scenario: Un puesto no libre no es seleccionable
- **GIVEN** el plano en modo selector con puestos en estado `ASSIGNED`/`REQUESTED`/`MINE`
- **WHEN** el empleado intenta pinchar uno de esos puestos
- **THEN** el puesto no responde a la selección (solo los `FREE` son elegibles)

### Requirement: Feedback visual de la selección de puesto en el plano
El plano en modo selector DEBE (MUST) marcar el puesto seleccionado con un **color distinto** (estado visual `SELECTED`, con token del design-system, sin depender solo del color) y DEBE (MUST) mostrar un **mensaje de confirmación** con el número del puesto, de forma que la selección sea perceptible visualmente y por lectores de pantalla.

#### Scenario: El puesto seleccionado cambia de color
- **GIVEN** el plano en modo selector
- **WHEN** el empleado selecciona un puesto libre
- **THEN** el marcador de ese puesto se pinta con el color de selección (distinto del color de su estado)
- **AND** el marcador expone su condición de seleccionado de forma accesible (`aria-pressed`/`aria-selected` y sufijo textual en su etiqueta)

#### Scenario: Mensaje de confirmación con el número del puesto
- **GIVEN** el plano en modo selector
- **WHEN** el empleado selecciona un puesto libre número N
- **THEN** el plano muestra un mensaje de confirmación (región `role="status"`) que indica que el puesto N ha sido seleccionado

#### Scenario: El estado de selección es solo de interfaz
- **GIVEN** un empleado que selecciona un puesto en el plano
- **WHEN** el backend devuelve el estado de los puestos para la fecha
- **THEN** el puesto sigue reportándose con su estado de dominio (`FREE`) y `SELECTED` no aparece como estado devuelto por el servidor
