# Capability: desks

## Resumen
Gestión de los 65 puestos de oficina (`Desk`) como recursos reservables. Cubre el
CRUD de puestos (número 1-65, categoría `STANDARD`/`EXECUTIVE`, coordenadas relativas),
y la reutilización del ciclo de asignación fija, solicitud, liberación y disponibilidad
mediante la abstracción `BookableResource` (`ResourceType = DESK`). Un empleado puede
tener simultáneamente plaza fija (`PARKING`) y puesto fijo (`DESK`).

## Fase
🟢🔵 ambas (alcance ampliado de puestos; ciclo posterior al núcleo de parking).

## Reglas de negocio implicadas
(README §"Puestos de oficina" y §"Reglas específicas de puestos"; NO hay códigos RN-xx)
- Solo los 65 puestos numerados (1-65) son reservables; el resto del plano no se modela como recurso.
- Categoría `STANDARD`: reservable por cualquier empleado · Categoría `EXECUTIVE`: habitualmente asignada L-V a un directivo, distinguida visualmente, pero liberable exactamente igual que cualquier otro puesto.
- Un empleado puede tener asignación fija de plaza (`PARKING`) y de puesto (`DESK`) a la vez; son recursos distintos.
- El puesto se solicita para una **fecha concreta** (no día de semana) reutilizando `requests`; las solicitudes de plaza y puesto se aprueban/rechazan por separado.
- Disponibilidad de un puesto para fecha F = puesto activo + sin asignación fija ese día (o liberado) + sin `Request APPROVED` para esa fecha. Sin reserva de visitante en puestos.
- `coord_x`/`coord_y` se expresan como porcentaje (0-100) del ancho/alto de la imagen del plano.

## Entidades implicadas
- `Desk` (`number` 1-65, `DeskCategory` `STANDARD`/`EXECUTIVE`, `coord_x`, `coord_y`, `active`)
- `BookableResource` (abstracción; `ResourceType = DESK`) — ver `generic-resource-refactor`
- `FixedAssignment`, `Request`, `Release` (reutilizados vía `resource_id`/`resource_type`)
- `Employee`

## Endpoints
> Los endpoints `/desks` y de solicitud de puesto **no existen aún en `docs/openapi.yaml`**; se proponen aquí. _[no en openapi.yaml todavía]_
- GET /api/v1/desks (operationId: listDesks) _[no en openapi.yaml todavía]_
- POST /api/v1/desks (operationId: createDesk) _[no en openapi.yaml todavía]_
- GET /api/v1/desks/{id} (operationId: getDesk) _[no en openapi.yaml todavía]_
- PUT /api/v1/desks/{id} (operationId: updateDesk) _[no en openapi.yaml todavía]_
- PATCH /api/v1/desks/{id}/activation (operationId: setDeskActivation) _[no en openapi.yaml todavía]_
- Asignación fija, solicitud, liberación y disponibilidad de puestos se sirven por los endpoints genéricos de `fixed-assignments`, `requests`, `releases` y `availability-calendar` con `resourceType = DESK`.

## Permisos
| Rol | Permisos |
|---|---|
| ADMIN | CRUD de puestos (alta, edición, activación/desactivación), asignación fija de puesto, liberación administrativa, aprobación/rechazo de solicitudes de puesto |
| EMPLOYEE | Listar/consultar puestos, solicitar puesto para una fecha, liberar voluntariamente su puesto fijo, ver sus propias asignaciones/solicitudes |

## ADDED Requirements
### Requirement: Alta de puesto numerado
**El sistema DEBE permitir al `ADMIN` crear un `Desk` con `number` único en el rango 1-65 y una `DeskCategory` válida, e impedir números duplicados o fuera de rango.**

#### Scenario: Alta de puesto válido
- **GIVEN** un `ADMIN` autenticado
- **WHEN** envía `POST /desks` con `{ number: 12, category: STANDARD, coordX: 30.5, coordY: 47.0 }`
- **THEN** el sistema crea el `Desk` con `active = true`
- **AND** responde 201 con la representación del puesto

#### Scenario: Número de puesto duplicado
- **GIVEN** un `Desk` existente con `number = 12`
- **WHEN** un `ADMIN` envía `POST /desks` con `number = 12`
- **THEN** el sistema responde 409 con `error` de conflicto de unicidad y no crea un segundo puesto

#### Scenario: Número fuera del rango 1-65
- **GIVEN** un `ADMIN` autenticado
- **WHEN** envía `POST /desks` con `number = 70`
- **THEN** el sistema responde 400 con `fields` indicando que `number` está fuera de rango (1-65)

#### Scenario: Empleado intenta crear un puesto
- **GIVEN** un `EMPLOYEE` autenticado
- **WHEN** envía `POST /desks`
- **THEN** el sistema responde 403 y no crea ningún puesto

### Requirement: Edición y activación/desactivación de puesto
**El sistema DEBE permitir al `ADMIN` editar la categoría y coordenadas de un puesto y activarlo/desactivarlo, sin que un puesto inactivo sea reservable.**

#### Scenario: Cambio de categoría a EXECUTIVE
- **GIVEN** un `Desk` `STANDARD` existente
- **WHEN** un `ADMIN` envía `PUT /desks/{id}` con `category = EXECUTIVE`
- **THEN** el sistema actualiza la categoría
- **AND** responde 200 con el puesto actualizado

#### Scenario: Desactivar puesto lo retira de disponibilidad
- **GIVEN** un `Desk` activo sin asignaciones ni solicitudes para una fecha F
- **WHEN** un `ADMIN` envía `PATCH /desks/{id}/activation` con `active = false`
- **THEN** el sistema marca el puesto `active = false`
- **AND** el puesto deja de aparecer como disponible en el cálculo de disponibilidad para F

### Requirement: Asignación fija de puesto independiente de la de plaza
**El sistema DEBE permitir asignar fijamente un puesto (`DESK`) a un empleado que ya tenga una plaza fija (`PARKING`), tratándolos como recursos distintos, y mantener la unicidad recurso/día y empleado/día por tipo de recurso.**

#### Scenario: Empleado con plaza fija recibe además puesto fijo
- **GIVEN** un `Employee` con una `FixedAssignment` activa de tipo `PARKING` para el lunes
- **WHEN** un `ADMIN` crea una `FixedAssignment` de tipo `DESK` para el mismo empleado y el mismo lunes
- **THEN** el sistema crea la asignación de puesto sin conflicto con la de plaza
- **AND** responde 201

#### Scenario: Dos empleados sobre el mismo puesto el mismo día
- **GIVEN** una `FixedAssignment` activa de `DESK` para el puesto 5 el martes
- **WHEN** un `ADMIN` intenta asignar fijamente el puesto 5 a otro empleado el martes
- **THEN** el sistema responde 409 por unicidad recurso/día

### Requirement: Solicitud y disponibilidad de puesto, EXECUTIVE liberable
**El sistema DEBE permitir a un empleado solicitar un puesto para una fecha dentro de la ventana hoy..+14d, calcular su disponibilidad como cualquier recurso y permitir liberar un puesto `EXECUTIVE` igual que uno `STANDARD`.**

#### Scenario: Solicitud de puesto disponible
- **GIVEN** un `EMPLOYEE` autenticado y un `Desk` activo sin asignación ni solicitud aprobada para la fecha F dentro de la ventana
- **WHEN** envía una solicitud (`Request`) de tipo `DESK` para la fecha F
- **THEN** el sistema crea la `Request` en estado `PENDING`
- **AND** responde 201

#### Scenario: Puesto EXECUTIVE liberado queda disponible
- **GIVEN** un `Desk` `EXECUTIVE` con asignación fija L-V a un directivo
- **WHEN** el directivo crea una liberación voluntaria (`Release`) de ese puesto para una fecha futura F
- **THEN** el sistema marca el puesto como disponible para F en el cálculo de disponibilidad
- **AND** otro empleado puede solicitarlo para F

#### Scenario: Solicitud de puesto ya asignado ese día
- **GIVEN** un `Desk` con `Request APPROVED` para la fecha F
- **WHEN** un `ADMIN` intenta aprobar otra solicitud del mismo puesto para F
- **THEN** el sistema responde 409 por falta de disponibilidad y no aprueba la segunda

## Casos límite (edge cases)
- Intentar crear más de 65 puestos: cada `number` está acotado a 1-65, por lo que el conjunto reservable nunca excede 65.
- Desactivar un puesto con asignaciones fijas activas: la edición no borra las asignaciones, pero el puesto inactivo no aparece como disponible; las asignaciones quedan inertes mientras esté inactivo _[verificar con README.md]_.
- Un puesto `EXECUTIVE` sigue todas las reglas de liberación de `STANDARD`; la categoría solo afecta a la distinción visual del plano, no a las reglas de reserva.
- No existe reserva de visitante en puestos: el cálculo de disponibilidad de `DESK` no considera `VisitorReservation`.
- Coordenadas fuera de 0-100 → 400 de validación; el posicionamiento fino se gestiona en `floor-plan`.

## Dependencias con otras capabilities
- Depende de `generic-resource-refactor` (abstracción `BookableResource`, `resource_id`/`resource_type`).
- Reutiliza `fixed-assignments`, `requests`, `releases` y `availability-calendar` con `resourceType = DESK`.
- Depende de `auth-local`/`auth-sso` para autenticación y de RBAC (`security-design.md`).
- Es prerrequisito de `floor-plan` (que consume `coord_x`/`coord_y`).
