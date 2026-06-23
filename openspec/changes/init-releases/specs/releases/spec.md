# Capability: releases

## Resumen
Liberación de un recurso con asignación fija para una fecha concreta, dejándolo
disponible ese día en el cálculo de disponibilidad. Liberación **voluntaria**
(`VOLUNTARY`, solo el dueño, fecha ≥ hoy) y **administrativa** (`ADMINISTRATIVE`,
solo `ADMIN`, con `reason` obligatorio). No envía email.

## Fase
🟢🔵 ambas (Fase 1 sobre plazas; en Fase 2/alcance ampliado se generaliza a `DESK` vía `BookableResource`).

## Reglas de negocio implicadas
(README §"Liberación (`Release`)" y §"Reglas de negocio"; NO hay códigos RN-xx)
- Liberación voluntaria: solo el dueño de la asignación fija puede liberar, y solo para fechas presentes o futuras (`release_date >= hoy`).
- Liberación administrativa: el `ADMIN` puede liberar el recurso fijo de cualquier empleado para cualquier fecha presente o futura, indicando `reason` obligatorio.
- Una fila de `Release` hace el recurso disponible para `release_date` en el cálculo de disponibilidad (combina con la asignación fija activa de ese día de la semana).
- La cancelación solo aplica a liberaciones futuras propias; no afecta a fechas pasadas.
- No se envía email ni al liberar voluntariamente ni al cancelar (capability `notifications` no participa).
- El acto se audita: liberación voluntaria (acción de empleado) y administrativa (acción de admin) vía `audit-retention`.

## Entidades implicadas
- `Release` (`employee_id` = dueño cuyo recurso se libera; `released_by_id` = quien ejecuta la liberación; `type`, `release_date`, `reason`)
- `FixedAssignment` (debe existir asignación fija activa del recurso para el día de la semana de `release_date`)
- `ParkingSpace` (recurso liberado; en alcance ampliado, `BookableResource` `PARKING`/`DESK`)
- `Employee` (dueño y ejecutor)

## Endpoints
- GET /api/v1/releases/mine (operationId: `listMyReleases`)
- POST /api/v1/releases (operationId: `createRelease`)
- DELETE /api/v1/releases/{id} (operationId: `cancelRelease`)
- POST /api/v1/releases/administrative (operationId: `createAdministrativeRelease`)

## Permisos
| Rol | Permisos |
|---|---|
| ADMIN | Liberación administrativa de cualquier empleado (`reason` obligatorio). _No_ usa los endpoints de liberación voluntaria salvo sobre asignaciones propias. |
| EMPLOYEE | Listar sus propias liberaciones, crear liberación voluntaria de su recurso fijo, cancelar una liberación futura propia. |

## ADDED Requirements
### Requirement: Liberación voluntaria de recurso propio
**El sistema DEBE permitir al dueño de una asignación fija activa liberar su recurso para una fecha presente o futura, creando una `Release` de tipo `VOLUNTARY`.**

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
**El sistema DEBE permitir al empleado listar sus propias liberaciones y cancelar una liberación futura propia, sin exponer las de otros empleados.**

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
**El sistema DEBE permitir solo a un `ADMIN` liberar el recurso fijo de cualquier empleado para una fecha presente o futura, exigiendo `reason`, creando una `Release` de tipo `ADMINISTRATIVE`.**

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
**El sistema DEBE evitar liberaciones duplicadas del mismo recurso y fecha bajo concurrencia, respondiendo 409 ante el conflicto.**

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

## Casos límite (edge cases)
- `parkingSpaceId` omitido en `createRelease`: el sistema resuelve la plaza fija del empleado para el día de la semana de `releaseDate`; si hay ambigüedad o no existe, responde 409.
- Liberación de hoy (`releaseDate == hoy`): permitida (presente cuenta como futuro inmediato).
- Si la asignación fija se revoca después de crear la liberación, la liberación existente persiste pero no añade disponibilidad útil (no hay asignación que liberar); se mantiene como histórico.
- `reason` solo es obligatorio para `ADMINISTRATIVE`; en `VOLUNTARY` es `null` (el esquema permite `NULL`, la obligatoriedad la valida la capa de servicio).
- Las liberaciones de fecha pasada se purgan automáticamente a 2 años vía `audit-retention` (`release_date < cutoffDate`).

## Dependencias con otras capabilities
- Depende de `auth-local`/`auth-sso` (autenticación de sesión y rol).
- Depende de `fixed-assignments` (debe existir asignación fija activa del recurso para el día de la semana).
- Alimenta `availability-calendar` (una `Release` hace el recurso disponible para `release_date`).
- Genera eventos de `audit-retention` (liberación voluntaria/administrativa y cancelación).
- No invoca `notifications` (sin email en liberación ni cancelación).
- En el alcance ampliado, `generic-resource-refactor` generaliza `parking_space_id → resource_id` para liberar también `DESK`.
