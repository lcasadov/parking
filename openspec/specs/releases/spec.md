# releases Specification

## Purpose
TBD - created by archiving change init-releases. Update Purpose after archive.
## Requirements
### Requirement: Liberación voluntaria de recurso propio
**El sistema DEBE (MUST) permitir al dueño de una asignación fija activa liberar su recurso para una fecha presente o futura, creando una `Release` de tipo `VOLUNTARY`.**

#### Scenario: Liberación voluntaria válida
- **GIVEN** un `Employee` autenticado con una `FixedAssignment` activa para el día de la semana de `releaseDate`, siendo `releaseDate >= hoy`
- **WHEN** envía `POST /releases` con `{ releaseDate }` (y opcionalmente `parkingSpaceId`)
- **THEN** el sistema crea una `Release` con `type = VOLUNTARY`, `employee_id = released_by_id = <empleado>` y `reason = null`
- **AND** responde 201 con el `Release` creado
- **AND** el recurso queda disponible para `releaseDate` en el cálculo de disponibilidad

#### Scenario: Liberación voluntaria con fecha en el pasado
- **GIVEN** un `Employee` autenticado con asignación fija activa
- **WHEN** envía `POST /releases` con `releaseDate` anterior a hoy
- **THEN** el sistema responde 400 con `error` de validación de ventana y `fields` señalando `releaseDate`
- **AND** no crea ninguna `Release`

#### Scenario: Liberación voluntaria sin asignación fija para ese día
- **GIVEN** un `Employee` autenticado sin `FixedAssignment` activa para el día de la semana de `releaseDate`
- **WHEN** envía `POST /releases` con `{ releaseDate }`
- **THEN** el sistema responde 409 (no hay recurso fijo que liberar esa fecha)
- **AND** no crea ninguna `Release`

### Requirement: Listado y cancelación de liberaciones propias
**El sistema DEBE (MUST) permitir al empleado listar sus propias liberaciones y cancelar una liberación futura propia, sin exponer las de otros empleados.**

#### Scenario: Listar mis liberaciones
- **GIVEN** un `Employee` autenticado con liberaciones registradas a su nombre
- **WHEN** envía `GET /releases/mine`
- **THEN** el sistema responde 200 con una página que contiene únicamente las liberaciones cuyo `employee_id` es el del solicitante

#### Scenario: Cancelar una liberación futura propia
- **GIVEN** un `Employee` autenticado con una `Release` propia cuya `release_date >= hoy`
- **WHEN** envía `DELETE /releases/{id}`
- **THEN** el sistema anula la liberación y responde 204
- **AND** el recurso vuelve a contar como no disponible (asignado) para esa fecha

#### Scenario: Cancelar una liberación ajena (BOLA)
- **GIVEN** un `Employee` autenticado
- **WHEN** envía `DELETE /releases/{id}` de una `Release` cuyo `employee_id` no es el suyo
- **THEN** el sistema responde 403 sin revelar detalles del recurso
- **AND** no modifica la liberación

#### Scenario: Cancelar una liberación de fecha pasada
- **GIVEN** un `Employee` autenticado con una `Release` propia cuya `release_date < hoy`
- **WHEN** envía `DELETE /releases/{id}`
- **THEN** el sistema responde 409 (no se pueden anular liberaciones pasadas)
- **AND** no modifica la liberación

### Requirement: Liberación administrativa
**El sistema DEBE (MUST) permitir solo a un `ADMIN` liberar el recurso fijo de cualquier empleado para una fecha presente o futura, exigiendo `reason`, creando una `Release` de tipo `ADMINISTRATIVE`.**

#### Scenario: Liberación administrativa válida
- **GIVEN** un `ADMIN` autenticado y un `Employee` con asignación fija activa del recurso para el día de la semana de `releaseDate`, siendo `releaseDate >= hoy`
- **WHEN** envía `POST /releases/administrative` con `{ employeeId, parkingSpaceId, releaseDate, reason }`
- **THEN** el sistema crea una `Release` con `type = ADMINISTRATIVE`, `employee_id = <empleado>`, `released_by_id = <admin>` y `reason = <motivo>`
- **AND** responde 201 con el `Release` creado

#### Scenario: Liberación administrativa sin motivo
- **GIVEN** un `ADMIN` autenticado
- **WHEN** envía `POST /releases/administrative` sin `reason` (o `reason` vacío)
- **THEN** el sistema responde 400 con `error` de validación y `fields` señalando `reason`
- **AND** no crea ninguna `Release`

#### Scenario: Liberación administrativa por un empleado sin rol ADMIN
- **GIVEN** un `Employee` (rol `EMPLOYEE`) autenticado
- **WHEN** envía `POST /releases/administrative`
- **THEN** el sistema responde 403
- **AND** no crea ninguna `Release`

### Requirement: Unicidad y concurrencia de liberaciones
**El sistema DEBE (MUST) evitar liberaciones duplicadas del mismo recurso y fecha bajo concurrencia, respondiendo 409 ante el conflicto.**

#### Scenario: Liberación duplicada del mismo recurso y fecha
- **GIVEN** un recurso ya liberado (`Release` existente) para `releaseDate`
- **WHEN** se envía una segunda liberación (voluntaria o administrativa) del mismo recurso para la misma `releaseDate`
- **THEN** el sistema responde 409 (recurso ya liberado esa fecha)
- **AND** no crea una segunda `Release`

#### Scenario: Dos liberaciones concurrentes del mismo recurso y fecha
- **GIVEN** dos peticiones simultáneas que liberan el mismo recurso para la misma `releaseDate`
- **WHEN** ambas se procesan en paralelo
- **THEN** exactamente una crea la `Release` (201) y la otra responde 409
- **AND** no quedan filas duplicadas para ese recurso y fecha

