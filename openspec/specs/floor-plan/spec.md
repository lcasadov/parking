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
**El sistema DEBE (MUST) permitir al empleado solicitar un puesto pinchándolo en el plano solo si está libre para la fecha, generando un `Request` equivalente al del flujo normal.**

#### Scenario: Solicitud de un puesto libre
- **GIVEN** un empleado autenticado y un `Desk` con `state = FREE` para una fecha dentro de la ventana
- **WHEN** envía `POST /floor-plan/desks/{deskId}/request` con `{ date }`
- **THEN** el sistema crea un `Request` (`PENDING`, `resourceType = DESK`) para ese puesto y fecha
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
