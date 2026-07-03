# parking-spaces Specification

## Purpose
TBD - created by archiving change init-parking-spaces. Update Purpose after archive.
## Requirements
### Requirement: Alta de plaza con `label` único
**El sistema DEBE (MUST) permitir a un `ADMIN` crear una plaza con un `label` único, rechazando con 409 cualquier colisión de `label`.**

#### Scenario: Alta de plaza con label nuevo
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **AND** no existe ninguna plaza con `label = "P-08"`
- **WHEN** envía `POST /parking-spaces` con `{ label: "P-08" }`
- **THEN** el sistema responde 201 con la plaza creada (`id`, `label = "P-08"`, `active = true`)
- **AND** persiste la plaza con `created_at` fijado

#### Scenario: Alta con label duplicado
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **AND** ya existe una plaza con `label = "P-08"`
- **WHEN** envía `POST /parking-spaces` con `{ label: "P-08" }`
- **THEN** el sistema responde 409 con `{ error, message, fields, timestamp }`
- **AND** no crea una segunda plaza

#### Scenario: Alta con label vacío o inválido
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **WHEN** envía `POST /parking-spaces` con `label` vacío o que excede la longitud permitida
- **THEN** el sistema responde 400 con `error` de validación y `fields` indicando el campo `label`
- **AND** no crea ninguna plaza

### Requirement: Edición y estado activa/inactiva
**El sistema DEBE (MUST) permitir a un `ADMIN` modificar el `label` y el estado `active` de una plaza, manteniendo la unicidad del `label` y excluyendo las plazas inactivas de la disponibilidad.**

#### Scenario: Edición de label válida
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **AND** existe una plaza con `id = 5` y `label = "P-08"`
- **WHEN** envía `PUT /parking-spaces/5` con `{ label: "P-09", active: true }`
- **THEN** el sistema responde 200 con la plaza actualizada (`label = "P-09"`)

#### Scenario: Desactivar una plaza la excluye de disponibilidad
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **AND** existe una plaza activa con `id = 5`
- **WHEN** envía `PUT /parking-spaces/5` con `{ active: false }`
- **THEN** el sistema responde 200 con `active = false`
- **AND** la plaza no aparece como disponible para ninguna fecha futura

#### Scenario: Edición a un label ya usado por otra plaza
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **AND** existe una plaza con `id = 5` (`label = "P-08"`) y otra con `label = "P-09"`
- **WHEN** envía `PUT /parking-spaces/5` con `{ label: "P-09" }`
- **THEN** el sistema responde 409 con `{ error, message, fields, timestamp }`
- **AND** no modifica el `label` de la plaza 5

#### Scenario: Edición de plaza inexistente
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **WHEN** envía `PUT /parking-spaces/999` para un `id` que no existe
- **THEN** el sistema responde 404 sin modificar nada

### Requirement: Configuración masiva del total y autorización
**El sistema DEBE (MUST) permitir a un `ADMIN` ajustar el número total de plazas del parque y DEBE denegar con 403 cualquier operación de gestión solicitada por un `EMPLOYEE`.**

#### Scenario: Configurar el total de plazas
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **WHEN** envía `POST /parking-spaces/configure` con `{ total: 50 }`
- **THEN** el sistema responde 200 con el listado de plazas resultante del ajuste
- **AND** no destruye el histórico de las plazas preexistentes

#### Scenario: Configurar con total inválido
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **WHEN** envía `POST /parking-spaces/configure` con `{ total: -3 }`
- **THEN** el sistema responde 400 con `error` de validación y `fields` indicando `total`

#### Scenario: Empleado intenta gestionar plazas
- **GIVEN** un usuario autenticado con rol `EMPLOYEE`
- **WHEN** envía `POST /parking-spaces` (o `PUT /parking-spaces/{id}` o `POST /parking-spaces/configure`)
- **THEN** el sistema responde 403 sin crear, modificar ni configurar plazas

