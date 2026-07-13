# parking-spaces Specification

## Purpose
TBD - created by archiving change init-parking-spaces. Update Purpose after archive.
## Requirements
### Requirement: Alta de plaza con `label` único
**El sistema DEBE (MUST) permitir a un `ADMIN` crear una plaza indicando su `number` (entero del esquema 1000-based, ≥ 1000), del que se deriva la planta y el `label`; DEBE rechazar con 409 cualquier colisión de `number` o `label`, y con 400 un `number` inválido (ausente, no entero o < 1000).**

#### Scenario: Alta de plaza con número nuevo
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **AND** no existe ninguna plaza con `number = 1008`
- **WHEN** envía `POST /parking-spaces` con `{ number: 1008 }`
- **THEN** el sistema responde 201 con la plaza creada (`id`, `number = 1008`, `floor = 1`, `label` acorde a `1008`, `active = true`)
- **AND** persiste la plaza con `created_at` fijado

#### Scenario: Alta con número duplicado
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **AND** ya existe una plaza con `number = 1008`
- **WHEN** envía `POST /parking-spaces` con `{ number: 1008 }`
- **THEN** el sistema responde 409 con `{ error, message, fields, timestamp }`
- **AND** no crea una segunda plaza

#### Scenario: Alta con número inválido
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **WHEN** envía `POST /parking-spaces` con `number` ausente, no entero o menor que 1000
- **THEN** el sistema responde 400 con `error` de validación y `fields` indicando el campo `number`
- **AND** no crea ninguna plaza

### Requirement: Edición y estado activa/inactiva
**El sistema DEBE (MUST) permitir a un `ADMIN` modificar el `number` (y con él la planta y el `label` derivados) y el estado `active` de una plaza, manteniendo la unicidad del `number`/`label` y excluyendo las plazas inactivas de la disponibilidad. La planta es siempre derivada del `number` y de solo lectura.**

#### Scenario: Edición de número válida
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **AND** existe una plaza con `id = 5`, `number = 1008` (planta 1)
- **WHEN** envía `PUT /parking-spaces/5` con `{ number: 2009, active: true }`
- **THEN** el sistema responde 200 con la plaza actualizada (`number = 2009`, `floor = 2`, `label` acorde)

#### Scenario: Desactivar una plaza la excluye de disponibilidad
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **AND** existe una plaza activa con `id = 5`
- **WHEN** envía `PUT /parking-spaces/5` con `{ active: false }`
- **THEN** el sistema responde 200 con `active = false`
- **AND** la plaza no aparece como disponible para ninguna fecha futura

#### Scenario: Edición a un número ya usado por otra plaza
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **AND** existe una plaza con `id = 5` (`number = 1008`) y otra con `number = 1009`
- **WHEN** envía `PUT /parking-spaces/5` con `{ number: 1009 }`
- **THEN** el sistema responde 409 con `{ error, message, fields, timestamp }`
- **AND** no modifica el `number` de la plaza 5

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

### Requirement: Planta derivada del número de la plaza
**El sistema DEBE (MUST) derivar la planta (`floor`) de una plaza a partir de su `number` como la división entera `number / 1000` (esquema 1000-based), exponer `floor` como campo de solo lectura en el DTO/UI de plazas, y NO permitir fijar la planta de forma independiente al número.**

#### Scenario: Derivación de planta para número de planta 1
- **GIVEN** una plaza con `number = 1007`
- **WHEN** el sistema calcula su planta
- **THEN** `floor = 1` (`1007 / 1000`)
- **AND** el DTO de la plaza expone `floor = 1` como campo de solo lectura

#### Scenario: Derivación de planta para plantas superiores
- **GIVEN** plazas con `number = 2001`, `number = 3025` y `number = 12010`
- **WHEN** el sistema calcula su planta
- **THEN** obtiene `floor = 2`, `floor = 3` y `floor = 12` respectivamente

#### Scenario: La planta no es un campo de entrada
- **GIVEN** un usuario autenticado con rol `ADMIN`
- **WHEN** envía un alta o edición de plaza incluyendo un `floor` que no corresponde a `number / 1000`
- **THEN** el sistema ignora/rechaza el `floor` recibido y usa siempre el derivado del `number`
- **AND** la respuesta refleja `floor = number / 1000`

### Requirement: Filtrado y visualización de plazas por planta
**El sistema DEBE (MUST) permitir listar las plazas filtrando por planta y DEBE mostrar la planta de cada plaza en gestión, plano y listados.**

#### Scenario: Listar plazas de una planta concreta
- **GIVEN** plazas repartidas en las plantas 1 y 2
- **WHEN** un usuario autenticado solicita el listado de plazas filtrando por `floor = 2`
- **THEN** el sistema responde 200 con únicamente las plazas cuyo `number` está en el rango `[2000, 2999]` (`number / 1000 = 2`)

#### Scenario: El listado incluye la planta de cada plaza
- **GIVEN** un usuario autenticado que solicita el listado de plazas sin filtro
- **WHEN** el sistema responde con las plazas
- **THEN** cada plaza incluye su `number`, su `label` y su `floor` derivado

### Requirement: Renumeración migratoria preservando FKs por `id`
**El sistema DEBE (MUST) migrar las plazas existentes con `label` no numérico (`P-001…P-025`) a números del esquema 1000-based, actualizando `number` y `label` de cada plaza SIN cambiar su `id`, de modo que las asignaciones fijas, solicitudes, liberaciones y reservas de visitante que referencian la plaza por `id` (`resource_id` / `parking_space_id`) queden intactas.**

#### Scenario: Renumeración conserva el id y por tanto las referencias
- **GIVEN** una plaza con `id = 7` y `label = "P-007"` referenciada por una asignación fija activa, una solicitud aprobada y una reserva de visitante (todas por `id = 7`)
- **WHEN** la migración renumera la plaza a `number = 1007` (planta 1) y `label` acorde
- **THEN** la plaza conserva `id = 7`
- **AND** la asignación fija, la solicitud y la reserva de visitante siguen apuntando a `id = 7` sin cambios

#### Scenario: Reparto determinista por planta
- **GIVEN** 25 plazas existentes ordenadas por `id`
- **AND** un parámetro `spacesPerFloor` que define cuántas plazas caben por planta
- **WHEN** la migración las renumera
- **THEN** la fila `k`-ésima (0-based) recibe `floor = 1 + (k / spacesPerFloor)` y `number = floor * 1000 + (k % spacesPerFloor) + 1`
- **AND** todos los `number` resultantes son únicos y ≥ 1000

