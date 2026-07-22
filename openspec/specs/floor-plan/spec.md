# floor-plan Specification

## Purpose
TBD - created by archiving change init-floor-plan. Update Purpose after archive.
## Requirements
### Requirement: Vista del plano por fecha
**El sistema DEBE (MUST) devolver, para una fecha dada, el estado y la posición (`coord_x`/`coord_y`) de cada puesto, coloreado según su disponibilidad relativa al empleado autenticado.**

#### Scenario: Consulta del plano para una fecha válida
- **GIVEN** un empleado autenticado y un conjunto de `Desk` activos con `coord_x`/`coord_y` definidos
- **WHEN** envía `GET /floor-plan?date={fecha}` dentro de la ventana de solicitud (hoy..+14d)
- **THEN** el sistema responde 200 con una lista de puestos, cada uno con `deskId`, `deskNumber`, `category`, `coordX`, `coordY` y `state`
- **AND** el `state` es uno de `FREE`, `ASSIGNED`, `REQUESTED`, `MINE`, `RELEASED` según asignaciones fijas, `Request` y `Release` para esa fecha
- **AND** los puestos `EXECUTIVE` incluyen el flag de categoría para su distinción visual

#### Scenario: Fecha fuera de la ventana de solicitud
- **GIVEN** un empleado autenticado
- **WHEN** envía `GET /floor-plan?date={fecha}` con una fecha pasada o posterior a hoy+14d
- **THEN** el sistema responde 400 con `error = OUTSIDE_REQUEST_WINDOW` y `fields` indicando `date`
- **AND** no devuelve estados de puestos

#### Scenario: El estado MINE no revela nombres ajenos
- **GIVEN** un empleado autenticado con un puesto asignado y otros puestos asignados a terceros para esa fecha
- **WHEN** consulta `GET /floor-plan?date={fecha}`
- **THEN** su propio puesto aparece con `state = MINE`
- **AND** los puestos de terceros aparecen como `ASSIGNED` sin identificar al titular

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

### Requirement: Edición de posiciones de puestos (editor de arrastre)
**El sistema DEBE (MUST) permitir al `ADMIN` persistir las coordenadas relativas de un puesto, validando que están en el rango 0-100, y rechazar la operación a un `EMPLOYEE`.**

#### Scenario: Admin reposiciona un puesto
- **GIVEN** un usuario `ADMIN` autenticado y un `Desk` existente
- **WHEN** envía `PUT /floor-plan/desks/{deskId}/position` con `{ coordX, coordY }` dentro de 0-100
- **THEN** el sistema persiste `coord_x`/`coord_y` en el `Desk`
- **AND** responde 204

#### Scenario: Coordenadas fuera de rango
- **GIVEN** un usuario `ADMIN` autenticado
- **WHEN** envía `PUT /floor-plan/desks/{deskId}/position` con `coordX = 150` (o un valor negativo)
- **THEN** el sistema responde 400 con `error` de validación y `fields` indicando `coordX`/`coordY`
- **AND** no modifica el puesto

#### Scenario: Empleado intenta editar posiciones
- **GIVEN** un usuario `EMPLOYEE` autenticado
- **WHEN** envía `PUT /floor-plan/desks/{deskId}/position`
- **THEN** el sistema responde 403 (autorización denegada)
- **AND** no modifica el puesto

### Requirement: Presentación del plano acorde al design-system
**La vista del plano DEBE (MUST) renderizar cada puesto en su coordenada propia (sin apilamiento por defecto), usar únicamente colores del design-system (sin hex sueltos) y pesos tipográficos 400/500 (nunca 700), y distinguir los puestos `EXECUTIVE` con anillo ámbar y el símbolo ◆.**

#### Scenario: Puestos renderizados sin apilamiento
- **GIVEN** un conjunto de puestos con coordenadas distintas
- **WHEN** el usuario abre el plano para una fecha válida
- **THEN** cada puesto se dibuja en su propia posición `(coordX, coordY)` como porcentaje del plano
- **AND** ningún grupo de puestos se superpone por compartir la coordenada central por defecto

#### Scenario: Marcador con estilo del design-system
- **GIVEN** un puesto en cualquier estado (`FREE`, `ASSIGNED`, `REQUESTED`, `MINE`, `RELEASED`)
- **WHEN** se renderiza su marcador
- **THEN** el color de fondo y texto provienen de la paleta pastel semántica del design-system
- **AND** un puesto `EXECUTIVE` muestra el anillo ámbar y el símbolo ◆ en marcador y leyenda

### Requirement: Controles del plano (fecha, filtros, zoom, panel lateral)
**El plano DEBE (MUST) ofrecer navegación de fecha (día anterior/siguiente y "Hoy") dentro de la ventana de reserva, filtros por estado con contadores, controles de zoom, y un panel lateral con la lista de puestos buscable.**

#### Scenario: Navegación de fecha recarga el plano
- **GIVEN** el plano abierto para una fecha
- **WHEN** el usuario pulsa "día siguiente" (dentro de la ventana hoy..+14d)
- **THEN** el plano recarga los estados de los puestos para la nueva fecha

#### Scenario: Filtro por estado con contador
- **GIVEN** el plano con puestos en varios estados
- **WHEN** el usuario activa el chip de un estado (p. ej. "Libre")
- **THEN** el plano resalta/filtra los puestos de ese estado
- **AND** cada chip muestra el número de puestos en ese estado

#### Scenario: Buscar un puesto en el panel lateral
- **GIVEN** el panel lateral con la lista de puestos
- **WHEN** el usuario escribe un número de puesto en la búsqueda
- **THEN** la lista se filtra a los puestos coincidentes

### Requirement: Plano usable en móvil
**En viewport móvil el plano DEBE (MUST) permitir interacción táctil (pan/zoom) y ofrecer una lista "Disponibles para solicitar" con un botón de solicitud por fila, de modo que el empleado pueda solicitar un puesto sin depender de pulsar marcadores diminutos.**

#### Scenario: Solicitud desde la lista móvil
- **GIVEN** un empleado en viewport móvil con puestos libres para la fecha
- **WHEN** pulsa "Solicitar" en la fila de un puesto libre de la lista
- **THEN** se crea la solicitud del puesto para esa fecha (mismo flujo que pulsar el marcador)
- **AND** recibe feedback de éxito o de conflicto

#### Scenario: Reposicionar un puesto por gesto táctil (admin)
- **GIVEN** un `ADMIN` en modo edición en viewport móvil
- **WHEN** arrastra un marcador con un gesto táctil
- **THEN** la nueva posición se persiste igual que con el ratón

### Requirement: Disponibilidad en la solicitud unificada
**El modal de solicitud unificada DEBE (MUST) mostrar, para cada recurso seleccionable (plaza y puesto), un indicador de disponibilidad para la fecha elegida antes de enviar.**

#### Scenario: Indicador de disponibilidad por recurso
- **GIVEN** un empleado en el modal de solicitud con una fecha elegida
- **WHEN** el modal consulta la disponibilidad de plaza y de puesto para esa fecha
- **THEN** muestra junto a cada opción un banner informativo con la disponibilidad
- **AND** el envío sigue permitido (el banner es informativo, la validación final la hace el backend)

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

### Requirement: Presentación del plano del día
El plano DEBE (MUST) mostrar la planta real con marcadores de puesto coloreados por estado,
usando las coordenadas y datos existentes.

#### Scenario: Marcadores sobre la planta
- **WHEN** se carga el plano de un día
- **THEN** cada puesto se dibuja sobre la imagen real en su coordenada (%)
- **AND** su color refleja el estado (ocupado/libre/liberado/solicitado)

#### Scenario: Ocupación del día
- **WHEN** se carga el plano
- **THEN** el panel lateral muestra los contadores por estado y el listado de puestos
- **AND** los datos provienen de los endpoints existentes

