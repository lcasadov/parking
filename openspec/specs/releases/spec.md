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
**El sistema DEBE (MUST) permitir a un `ADMIN` o a un `AGENCIA` liberar el recurso fijo (plaza de garaje o puesto de oficina) de cualquier empleado para una fecha presente o futura, exigiendo `reason`, creando una `Release` de tipo `ADMINISTRATIVE`. Cualquier otro rol (incluido `EMPLOYEE`) DEBE recibir 403.**

#### Scenario: Liberación administrativa válida por ADMIN
- **GIVEN** un `ADMIN` autenticado y un `Employee` con asignación fija activa del recurso para el día de la semana de `releaseDate`, siendo `releaseDate >= hoy`
- **WHEN** envía `POST /releases/administrative` con `{ employeeId, parkingSpaceId, releaseDate, reason }`
- **THEN** el sistema crea una `Release` con `type = ADMINISTRATIVE`, `employee_id = <empleado>`, `released_by_id = <admin>` y `reason = <motivo>`
- **AND** responde 201 con el `Release` creado

#### Scenario: Liberación administrativa válida por AGENCIA
- **GIVEN** un usuario `AGENCIA` autenticado y un `Employee` con asignación fija activa del recurso para el día de la semana de `releaseDate`, siendo `releaseDate >= hoy`
- **WHEN** envía `POST /releases/administrative` con `{ employeeId, parkingSpaceId, releaseDate, reason }`
- **THEN** el sistema crea una `Release` con `type = ADMINISTRATIVE`, `employee_id = <empleado>`, `released_by_id = <usuario agencia>` y `reason = <motivo>`
- **AND** responde 201 con el `Release` creado

#### Scenario: Liberación administrativa sin motivo
- **GIVEN** un `ADMIN` o `AGENCIA` autenticado
- **WHEN** envía `POST /releases/administrative` sin `reason` (o `reason` vacío)
- **THEN** el sistema responde 400 con `error` de validación y `fields` señalando `reason`
- **AND** no crea ninguna `Release`

#### Scenario: Liberación administrativa por un empleado sin rol autorizado
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

### Requirement: AGENCIA limitada a la liberación administrativa en el área de releases
**El sistema DEBE (MUST) restringir al rol `AGENCIA`, dentro del área de liberaciones, exclusivamente al endpoint de liberación administrativa; el rol NO DEBE poder crear liberaciones voluntarias, ni listar o cancelar liberaciones del portal de empleado (fail-closed).**

#### Scenario: AGENCIA no puede crear una liberación voluntaria
- **GIVEN** un usuario `AGENCIA` autenticado
- **WHEN** envía `POST /releases` (liberación voluntaria de recurso propio)
- **THEN** el sistema responde 403
- **AND** no crea ninguna `Release`

#### Scenario: AGENCIA no puede listar liberaciones del portal de empleado
- **GIVEN** un usuario `AGENCIA` autenticado
- **WHEN** envía `GET /releases/mine`
- **THEN** el sistema responde 403
- **AND** no devuelve ninguna liberación

#### Scenario: AGENCIA no puede cancelar una liberación del portal de empleado
- **GIVEN** un usuario `AGENCIA` autenticado
- **WHEN** envía `DELETE /releases/{id}`
- **THEN** el sistema responde 403
- **AND** no modifica ninguna `Release`

### Requirement: Selección de empleado y ocupación semanal para liberación (ADMIN/AGENCIA)
**El sistema DEBE (MUST) ofrecer a un `ADMIN` o `AGENCIA`, para el flujo de liberación administrativa: (a) un listado de empleados seleccionables (datos mínimos: id y nombre) y (b) la ocupación de un empleado para una semana dada, devolviendo por cada día las reservas del empleado en plaza y en puesto, con su origen (`FIXED_ASSIGNMENT` o `REQUEST_APPROVED`), la fecha, el recurso (número/etiqueta) y el `requestId` cuando la ocupación proviene de una solicitud aprobada. Cualquier otro rol (incluido `EMPLOYEE`) DEBE (MUST) recibir 403.**

#### Scenario: Ocupación semanal de un empleado para ADMIN
- **GIVEN** un `ADMIN` autenticado, un `Employee` con una plaza por asignación fija y un puesto por solicitud aprobada dentro de la semana consultada
- **WHEN** solicita la ocupación semanal de ese empleado indicando el inicio de semana
- **THEN** el sistema responde 200 con las reservas de la semana (plaza y puesto), cada una con su fecha, recurso y origen
- **AND** las reservas por solicitud incluyen su `requestId`

#### Scenario: Ocupación semanal de un empleado para AGENCIA
- **GIVEN** un usuario `AGENCIA` autenticado
- **WHEN** solicita el listado de empleados seleccionables y la ocupación semanal de un empleado
- **THEN** el sistema responde 200 en ambos casos con los datos correspondientes

#### Scenario: Un EMPLOYEE no puede consultar la ocupación de otros
- **GIVEN** un `Employee` (rol `EMPLOYEE`) autenticado
- **WHEN** solicita el listado de empleados seleccionables o la ocupación semanal de un empleado
- **THEN** el sistema responde 403
- **AND** no devuelve datos

