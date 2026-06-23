# Capability: generic-resource-refactor

## Resumen
Refactor interno (sin cambio de comportamiento visible) que generaliza la
columna `parking_space_id` a `resource_id` con un discriminador `ResourceType`
(`PARKING`/`DESK`) sobre `Request`, `FixedAssignment`, `Release` y el cálculo de
disponibilidad, introduciendo la abstracción de dominio `BookableResource`.
Prerrequisito de la capability `desks`. No añade endpoints.

## Fase
🟢🔵 (refactor transversal sobre el núcleo de parking; habilita el alcance ampliado de puestos).

## Reglas de negocio implicadas
(README §"Recurso reservable — concepto unificado" y §"Reglas de negocio"; NO hay códigos RN-xx)
- La lógica de asignación fija, solicitud, liberación y cálculo de disponibilidad es **compartida** entre tipos de recurso y parametrizada por `ResourceType`. El refactor preserva esa lógica idéntica para `PARKING`.
- No hay cambio de comportamiento observable: los contratos HTTP, los códigos de error y los resultados de disponibilidad son los mismos antes y después del refactor.
- Toda fila reservable apunta hoy a `parking_spaces`; el refactor migra esas referencias a `resource_id` + `resource_type = 'PARKING'` sin perder datos.
- Las reglas de unicidad existentes (recurso/día activo, empleado/día activo, una solicitud `PENDING` por empleado/fecha) se mantienen, ahora expresadas sobre `resource_id`/`resource_type`.
- La reserva de visitante (`VisitorReservation`) sigue aplicando solo a `PARKING` (no se generaliza a recurso).

## Entidades implicadas
- `BookableResource` (abstracción de dominio; se materializa en `ParkingSpace` y, después, `Desk`)
- `ResourceType` (enum: `PARKING`, `DESK`)
- `ParkingSpace`
- `Request` (`parking_space_id` → `resource_id` + `resource_type`)
- `FixedAssignment` (`parking_space_id` → `resource_id` + `resource_type`)
- `Release` (`parking_space_id` → `resource_id` + `resource_type`)
- `VisitorReservation` (no se generaliza; permanece sobre `parking_spaces`) _[verificar con docs/data-model.md]_

## Endpoints
- (Ninguno nuevo.) Refactor interno: los endpoints existentes de `requests`, `fixed-assignments`, `releases` y `availability-calendar` conservan ruta, `operationId`, payload y semántica.

## Permisos
| Rol | Permisos |
|---|---|
| ADMIN | Sin cambios: gestiona asignaciones, liberaciones administrativas y resuelve solicitudes igual que antes. |
| EMPLOYEE | Sin cambios: crea/consulta solicitudes propias, libera lo propio y consulta disponibilidad igual que antes. |

## ADDED Requirements
### Requirement: Generalización del modelo a recurso reservable
**El sistema DEBE sustituir la referencia `parking_space_id` por `resource_id` + `resource_type` (`ResourceType`) en `Request`, `FixedAssignment` y `Release`, exponiendo la abstracción `BookableResource` en el dominio.**

#### Scenario: Una asignación fija de parking se modela como recurso PARKING
- **GIVEN** una `FixedAssignment` activa de una `ParkingSpace` para un `employee_id` y `day_of_week`
- **WHEN** el sistema persiste o lee esa asignación tras el refactor
- **THEN** la fila expone `resource_id` igual al identificador de la plaza
- **AND** `resource_type = 'PARKING'`
- **AND** la resolución de `BookableResource` devuelve la misma `ParkingSpace` que antes del refactor

#### Scenario: Una solicitud PENDING no referencia recurso hasta su aprobación
- **GIVEN** un `EMPLOYEE` autenticado dentro de la ventana de solicitud
- **WHEN** crea una `Request` mediante `createRequest`
- **THEN** la `Request` queda en estado `PENDING` con `resource_id = NULL`
- **AND** `resource_type = 'PARKING'` (tipo por defecto del núcleo de parking)
- **AND** el comportamiento observable es idéntico al previo al refactor

### Requirement: No-regresión del comportamiento observable
**El sistema DEBE preservar exactamente el comportamiento observable (códigos HTTP, forma de error `{ error, message, fields, timestamp }`, resultados de disponibilidad y reglas de unicidad) de `requests`, `fixed-assignments`, `releases` y `availability-calendar` para recursos `PARKING`.**

#### Scenario: La aprobación de una solicitud sin disponibilidad sigue devolviendo 409
- **GIVEN** una `Request` `PENDING` para una fecha en la que la plaza candidata ya no está disponible
- **WHEN** un `ADMIN` invoca `approveRequest`
- **THEN** el sistema responde 409 con la misma forma de error que antes del refactor
- **AND** la `Request` permanece `PENDING`
- **AND** no se persiste ningún `resource_id`

#### Scenario: La segunda solicitud PENDING del mismo empleado y fecha sigue devolviendo 409
- **GIVEN** un `EMPLOYEE` con una `Request` `PENDING` para `requested_date = D`
- **WHEN** crea otra `Request` para la misma `requested_date = D`
- **THEN** el sistema responde 409 (conflicto de unicidad de solicitud `PENDING`)
- **AND** el comportamiento es idéntico al previo al refactor, ahora evaluado sobre `resource_type = 'PARKING'`

#### Scenario: El cálculo de disponibilidad devuelve el mismo resultado que antes del refactor
- **GIVEN** una fecha `F` y un conjunto de plazas con asignaciones fijas, liberaciones, solicitudes `APPROVED` y reservas de visitante
- **WHEN** un usuario invoca `getAvailability` para `F`
- **THEN** el sistema devuelve el mismo conjunto de recursos disponibles que la implementación previa
- **AND** la consulta filtra por `resource_type = 'PARKING'` sin alterar el resultado

### Requirement: Migración de datos sin pérdida y autorización intacta
**El sistema DEBE migrar mediante Flyway las referencias existentes `parking_space_id` a `resource_id` con `resource_type = 'PARKING'` preservando todos los datos, manteniendo las reglas de autorización por rol y de pertenencia de objeto sin cambios.**

#### Scenario: La migración Flyway porta las referencias existentes
- **GIVEN** filas existentes en `requests`, `fixed_assignments` y `releases` con `parking_space_id`
- **WHEN** se aplica la migración del refactor
- **THEN** cada fila conserva `resource_id` igual al `parking_space_id` original
- **AND** `resource_type = 'PARKING'` en todas las filas portadas
- **AND** las claves e índices de unicidad equivalentes quedan vigentes sobre las nuevas columnas

#### Scenario: Un EMPLOYEE sigue sin poder resolver solicitudes ajenas
- **GIVEN** un `EMPLOYEE` autenticado y una `Request` `PENDING` de otro empleado
- **WHEN** intenta invocar `approveRequest` o `rejectRequest`
- **THEN** el sistema responde 403 igual que antes del refactor
- **AND** el estado de la `Request` no cambia

## Casos límite (edge cases)
- Filas `Request` en estado `PENDING` con `parking_space_id = NULL`: la migración mantiene `resource_id = NULL` y solo fija `resource_type = 'PARKING'`.
- Filas históricas (`REJECTED`/`CANCELLED`/revocadas) se migran igual que las vivas; el refactor no purga ni altera historia.
- `VisitorReservation` no se generaliza: sigue referenciando `parking_spaces`; cualquier intento de tratarla como recurso genérico es out of scope.
- Tras el refactor, `resource_type = 'DESK'` es un valor válido del enum pero **sin filas** hasta que llegue `desks`; ninguna consulta del núcleo de parking debe devolver recursos `DESK`.
- Reversión: si la migración falla, el rollback debe dejar el esquema y los datos en el estado `parking_space_id` previo (migración idempotente y verificable).

## Dependencias con otras capabilities
- Depende de `parking-spaces` (define la `ParkingSpace` que materializa `BookableResource` tipo `PARKING`).
- Refactoriza el modelo compartido de `requests`, `fixed-assignments`, `releases` y `availability-calendar` sin cambiar su contrato.
- Es **prerrequisito** de `desks` (que añade `ResourceType = 'DESK'`) y, transitivamente, de `floor-plan`.
- No afecta a `visitors`/`visitor_reservations` (siguen ligadas a `parking_spaces`).
