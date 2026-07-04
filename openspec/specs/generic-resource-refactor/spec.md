# generic-resource-refactor Specification

## Purpose
TBD - created by archiving change init-generic-resource-refactor. Update Purpose after archive.
## Requirements
### Requirement: Generalización del modelo a recurso reservable
**El sistema DEBE (MUST) sustituir la referencia `parking_space_id` por `resource_id` + `resource_type` (`ResourceType`) en `Request`, `FixedAssignment` y `Release`, exponiendo la abstracción `BookableResource` en el dominio.**

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
**El sistema DEBE (MUST) preservar exactamente el comportamiento observable (códigos HTTP, forma de error `{ error, message, fields, timestamp }`, resultados de disponibilidad y reglas de unicidad) de `requests`, `fixed-assignments`, `releases` y `availability-calendar` para recursos `PARKING`.**

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
**El sistema DEBE (MUST) migrar mediante Flyway las referencias existentes `parking_space_id` a `resource_id` con `resource_type = 'PARKING'` preservando todos los datos, manteniendo las reglas de autorización por rol y de pertenencia de objeto sin cambios.**

#### Scenario: La migración Flyway porta las referencias existentes
- **GIVEN** filas existentes en `requests`, `fixed_assignments` y `releases` con `parking_space_id`
- **WHEN** se aplica la migración del refactor
- **THEN** cada fila conserva `resource_id` igual al `parking_space_id` original
- **AND** `resource_type = 'PARKING'` en todas las filas portadas
- **AND** las claves e índices de unicidad equivalentes quedan vigentes sobre las nuevas columnas
- **AND** los índices únicos filtrados CONSERVAN su nombre (`UX_fixed_assignments_space_day_active`, `UX_fixed_assignments_employee_day_active`, `UX_requests_employee_date_pending`, `UX_requests_space_date_approved`, `UX_releases_space_date`), de modo que la traducción de su violación al mismo `409`/forma de error se mantiene sin cambios (implementado en `V12__generic_resource_refactor.sql`)

#### Scenario: Un EMPLOYEE sigue sin poder resolver solicitudes ajenas
- **GIVEN** un `EMPLOYEE` autenticado y una `Request` `PENDING` de otro empleado
- **WHEN** intenta invocar `approveRequest` o `rejectRequest`
- **THEN** el sistema responde 403 igual que antes del refactor
- **AND** el estado de la `Request` no cambia

