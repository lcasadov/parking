# Capability: parking-spaces

## Resumen
Gestión administrativa de las plazas de parking físicas (`ParkingSpace`),
identificadas por un `label` único y humano (p. ej. `P-08`), con estado
activa/inactiva. Cubre el CRUD restringido a `ADMIN` y la configuración
masiva del número total de plazas.

## Fase
🟢🔵 ambas (núcleo de parking; idéntico comportamiento en Fase 1 y Fase 2).

## Reglas de negocio implicadas
(README §"Reglas de negocio" y §"Plazas"; NO hay códigos RN-xx)
- El `label` de una plaza es único y de cara al usuario; un alta o edición que colisione con otro `label` se rechaza.
- Una plaza tiene estado activa/inactiva; las plazas inactivas nunca están disponibles para ninguna fecha (no entran en disponibilidad ni en asignaciones nuevas).
- La gestión de plazas (alta, edición, configuración del total) es exclusiva de `ADMIN`; el `EMPLOYEE` solo percibe plazas a través de la disponibilidad, no del catálogo.
- La configuración masiva ajusta el número total de plazas del parque (alta/ajuste del total), sin destruir el histórico asociado a plazas existentes.

## Entidades implicadas
- `ParkingSpace` (`id`, `label`, `active`, `created_at`)

## Endpoints
- GET /api/v1/parking-spaces (operationId: listParkingSpaces) — paginado, filtro opcional `active`
- POST /api/v1/parking-spaces (operationId: createParkingSpace)
- PUT /api/v1/parking-spaces/{id} (operationId: updateParkingSpace)
- POST /api/v1/parking-spaces/configure (operationId: configureParkingSpaces)

## Permisos
| Rol | Permisos |
|---|---|
| ADMIN | Listar, crear, modificar plazas; configurar el total; activar/desactivar |
| EMPLOYEE | Sin acceso al catálogo de plazas (solo percibe huecos libres vía `availability-calendar`) |

## ADDED Requirements
### Requirement: Alta de plaza con `label` único
**El sistema DEBE permitir a un `ADMIN` crear una plaza con un `label` único, rechazando con 409 cualquier colisión de `label`.**

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
**El sistema DEBE permitir a un `ADMIN` modificar el `label` y el estado `active` de una plaza, manteniendo la unicidad del `label` y excluyendo las plazas inactivas de la disponibilidad.**

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
**El sistema DEBE permitir a un `ADMIN` ajustar el número total de plazas del parque y DEBE denegar con 403 cualquier operación de gestión solicitada por un `EMPLOYEE`.**

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

## Casos límite (edge cases)
- Reducir `total` por debajo del número de plazas existentes: la política de qué plazas se desactivan/eliminan se decide en `configureParkingSpaces` _[verificar con docs/data-model.md — el modelo solo describe alta/ajuste del total]_; las plazas con histórico vinculado (asignaciones, solicitudes, reservas) no deben perder ese histórico.
- Desactivar una plaza que tiene asignaciones fijas o solicitudes futuras: la plaza deja de estar disponible, pero los efectos sobre `fixed-assignments`/`requests` se tratan en esas capabilities.
- El `label` distingue por igualdad exacta; el tratamiento de espacios/mayúsculas en la comparación de unicidad se delega a la normalización del campo _[verificar con docs/data-model.md]_.
- `listParkingSpaces` admite el filtro `active`; sin filtro devuelve todas (activas e inactivas) paginadas.

## Dependencias con otras capabilities
- Depende de `auth-local` / `auth-sso` para autenticar y de la autorización por rol `ADMIN`.
- Es referenciada por `fixed-assignments`, `releases`, `requests`, `visitors` y `availability-calendar`, que apuntan a `parking_space_id`.
- En el alcance ampliado, `generic-resource-refactor` generaliza `ParkingSpace` bajo `BookableResource` (`ResourceType = PARKING`).
