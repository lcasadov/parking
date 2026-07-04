# Capability: floor-plan

## Resumen
Plano interactivo de puestos de oficina: imagen de la planta con un marcador
por puesto (`Desk`), coloreado según el estado del puesto para una fecha
seleccionada. El empleado pincha un puesto libre para solicitarlo directamente;
el admin posiciona los marcadores con un editor de arrastre que persiste
`coord_x`/`coord_y` (% del ancho/alto de la imagen). El parking no tiene plano.

## Fase
🟢🔵 Ambas fases (alcance ampliado de puestos; ciclo posterior al núcleo de parking).

## Reglas de negocio implicadas
(README §"Puestos de oficina" → "Plano interactivo" / "Reglas específicas de puestos"; NO hay códigos RN-xx)
- Cada puesto se representa con un marcador posicionado por coordenadas relativas (`coord_x`, `coord_y`), porcentaje del ancho/alto de la imagen, de 0 a 100, independiente de la resolución.
- El marcador se colorea según el estado del puesto para la fecha seleccionada: libre, asignado, solicitado (pendiente), mi puesto, liberado y `EXECUTIVE` (distinción visual).
- Los puestos `EXECUTIVE` se distinguen visualmente en el plano pero siguen las mismas reglas de disponibilidad y liberación que los `STANDARD`.
- El empleado solo puede solicitar desde el plano puestos en estado libre para la fecha elegida.
- El admin posiciona los marcadores con un editor visual de arrastre; las coordenadas resultantes se persisten en BD.
- El parking no tiene plano: las plazas se gestionan como lista numerada.

## Entidades implicadas
- `Desk` (campos `coord_x`, `coord_y`, `category` con `DeskCategory` `STANDARD`/`EXECUTIVE`, `desk_number` 1-65, `active`)
- `FixedAssignment`, `Request`, `Release` (consultadas vía `BookableResource`/`ResourceType=DESK` para derivar el estado por fecha)

## Endpoints
> Los endpoints `/floor-plan` aún **no** están en `docs/openapi.yaml`; se proponen aquí. Base `/api/v1` (contexto `/parking-api`).

- GET /api/v1/floor-plan?date={ISO_DATE} — estado de todos los puestos para una fecha (operationId: `getFloorPlan`) _[no en openapi.yaml todavía]_
- PUT /api/v1/floor-plan/desks/{deskId}/position — persiste `coord_x`/`coord_y` de un puesto, solo `ADMIN` (operationId: `updateDeskPosition`) _[no en openapi.yaml todavía]_
- POST /api/v1/floor-plan/desks/{deskId}/request — solicita directamente un puesto libre para una fecha, `EMPLOYEE` (operationId: `requestDeskFromFloorPlan`) _[no en openapi.yaml todavía]_

## Permisos
| Rol | Permisos |
|---|---|
| ADMIN | Ver el plano por fecha; arrastrar marcadores y persistir `coord_x`/`coord_y`; (también puede solicitar como empleado si procede) |
| EMPLOYEE | Ver el plano por fecha; pinchar un puesto libre para solicitarlo; NO puede editar posiciones |

## ADDED Requirements
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

## Casos límite (edge cases)
- Puesto sin `coord_x`/`coord_y` definidos (recién creado): aparece en la respuesta del plano sin posición; el admin debe colocarlo con el editor antes de que sea visible en el plano.
- Puesto `inactive` (`active = false`): no aparece como reservable en el plano (o aparece atenuado, no clicable para solicitar).
- Liberación: un puesto fijo liberado para la fecha aparece con `state = RELEASED` (libre para solicitud) aunque tenga asignación fija ese día de la semana.
- Concurrencia: dos empleados pinchan el mismo puesto libre a la vez → solo uno obtiene `Request`; el otro recibe 409 de disponibilidad.
- El parking no expone `/floor-plan`: cualquier intento de obtener un plano de plazas de parking carece de endpoint.

## API Contract
Ver `docs/openapi.yaml` — tag `Floor Plan` para el contrato completo de los tres endpoints
(`getFloorPlan`, `requestDeskFromFloorPlan`, `updateDeskPosition`) y sus schemas
(`FloorPlanResponse`, `FloorPlanDesk`, `FloorPlanDeskState`, `FloorPlanRequestBody`,
`DeskRequestResponse`, `DeskPositionRequest`).

## Dependencias con otras capabilities
- Depende de `desks` (entidad `Desk`, sus coordenadas y categorías).
- Depende de `generic-resource-refactor` (`BookableResource`/`ResourceType=DESK`) para derivar disponibilidad.
- Reutiliza `requests` (creación de `Request` desde el plano), `releases` y `availability-calendar` (cálculo de estado por fecha).
- Depende de `auth-local`/`auth-sso` para autenticación y de la autorización por rol (`security-design.md`).
