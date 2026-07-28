## MODIFIED Requirements

### Requirement: Vista del plano por fecha
**El sistema DEBE (MUST) devolver, para una fecha dada, el estado y la posición (`coord_x`/`coord_y`) de cada puesto, coloreado según su disponibilidad relativa al empleado autenticado. Un puesto con una reserva de visitante para esa fecha DEBE (MUST) aparecer ocupado (`ASSIGNED`), igual que si lo tuviera asignado un tercero, sin exponer que el ocupante es un visitante.**

#### Scenario: Consulta del plano para una fecha válida
- **GIVEN** un empleado autenticado y un conjunto de `Desk` activos con `coord_x`/`coord_y` definidos
- **WHEN** envía `GET /floor-plan?date={fecha}` para hoy o cualquier fecha futura (sin tope superior)
- **THEN** el sistema responde 200 con una lista de puestos, cada uno con `deskId`, `deskNumber`, `category`, `coordX`, `coordY` y `state`
- **AND** el `state` es uno de `FREE`, `ASSIGNED`, `REQUESTED`, `MINE`, `RELEASED` según asignaciones fijas, `Request`, `Release` y reservas de visitante para esa fecha
- **AND** los puestos `EXECUTIVE` incluyen el flag de categoría para su distinción visual

#### Scenario: Puesto con reserva de visitante aparece ocupado
- **GIVEN** un empleado autenticado y un `Desk` activo sin asignación fija ni solicitud, con una `VisitorReservation` (`resourceType = DESK`) para la fecha consultada
- **WHEN** envía `GET /floor-plan?date={fecha}`
- **THEN** ese puesto aparece con `state = ASSIGNED` (ocupado por un tercero), nunca `FREE` ni `MINE`
- **AND** la respuesta no revela que el ocupante es un visitante (mismo tratamiento que un tercero empleado)

#### Scenario: Fecha pasada rechazada
- **GIVEN** un empleado autenticado
- **WHEN** envía `GET /floor-plan?date={fecha}` con una fecha anterior a hoy
- **THEN** el sistema responde 400 con `error = OUTSIDE_REQUEST_WINDOW` y `fields` indicando `date`
- **AND** no devuelve estados de puestos

#### Scenario: El estado MINE no revela nombres ajenos
- **GIVEN** un empleado autenticado con un puesto asignado y otros puestos asignados a terceros para esa fecha
- **WHEN** consulta `GET /floor-plan?date={fecha}`
- **THEN** su propio puesto aparece con `state = MINE`
- **AND** los puestos de terceros aparecen como `ASSIGNED` sin identificar al titular
