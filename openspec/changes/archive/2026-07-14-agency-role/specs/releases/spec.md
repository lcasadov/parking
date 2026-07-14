## MODIFIED Requirements

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

## ADDED Requirements

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
