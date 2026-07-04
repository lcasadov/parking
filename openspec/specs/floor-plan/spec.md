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
