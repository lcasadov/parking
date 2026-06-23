# Design: init-generic-resource-refactor

## Context
El modelo actual (`docs/data-model.md`) ata cada referencia reservable a
`parking_spaces` vía `parking_space_id` en `requests`, `fixed_assignments` y
`releases`. La nota de alcance del data-model anticipa este refactor: generalizar
`parking_space_id` a un `resource_id` polimórfico para soportar `desks`. La
arquitectura hexagonal permite introducir `BookableResource` como puerto de
dominio sin acoplar el cálculo de disponibilidad a una tabla concreta. El refactor
no debe alterar ningún comportamiento observable: misma API, mismos códigos de
error, mismos resultados de disponibilidad para `PARKING`.

## Goals
- Generalizar `parking_space_id → resource_id` + `resource_type` (`ResourceType`).
- Introducir `BookableResource` como abstracción de dominio compartida.
- Preservar al 100% el comportamiento observable (no-regresión verificable).
- Migrar los datos existentes sin pérdida y con rollback seguro.
- Dejar el modelo preparado para `desks` sin introducir aún filas `DESK`.

## Decisions
- **Discriminador explícito `resource_type`** (no herencia de tabla por tabla):
  añade `resource_type VARCHAR + CHECK (resource_type IN ('PARKING','DESK'))`
  junto a `resource_id`. Justificación: mantiene un único camino de consulta de
  disponibilidad parametrizado por tipo, evita JOINs polimórficos costosos y es
  el patrón que `data-model.md` §10.4 plantea como pendiente.
- **`BookableResource` como puerto de dominio**: un `ResourceResolverPort`
  resuelve `(resource_id, resource_type)` al recurso concreto (`ParkingSpace`
  hoy, `Desk` mañana). El dominio (`AvailabilityUseCase`, casos de uso de
  solicitud/asignación/liberación) deja de depender de `ParkingSpaceRepository`
  directamente. Justificación: cumple inversión de dependencias y habilita
  `desks` sin tocar la lógica compartida.
- **Renombrado de columna + FK portada en lugar de tabla nueva**: la migración
  renombra/añade `resource_id` (= `parking_space_id`) y `resource_type` =
  `'PARKING'`; conserva la FK a `parking_spaces` mientras solo exista `PARKING`.
  Justificación: menor superficie de cambio, datos portados in situ, reversible.
- **Índices de unicidad reexpresados**: `UX_fixed_assignments_*`,
  `UX_requests_employee_date_pending` se recrean incluyendo `resource_type`
  donde el alcance ampliado lo exige (clave = empleado + fecha + tipo). Para el
  núcleo `PARKING` el efecto observable es idéntico. _[verificar con docs/data-model.md §10.4]_
- **`VisitorReservation` no se generaliza**: las reservas de visitante siguen
  ligadas a `parking_spaces` (el README excluye visitantes de `DESK`).
- **Sin cambios de API**: ningún `operationId`, ruta ni payload cambia; el
  `resource_type` se infiere como `PARKING` en todo el núcleo de parking.

## Risks
- **Regresión silenciosa** en disponibilidad o unicidad → mitigado con suite de
  no-regresión que reejecuta los tests existentes verdes y compara resultados
  antes/después sobre datos idénticos.
- **Pérdida de datos en la migración** → mitigado con migración idempotente,
  verificación de conteos pre/post y rollback que restaura `parking_space_id`.
- **Deriva entidad/DDL** (`ddl-auto=validate`) → la migración y las entidades JPA
  deben renombrar la columna de forma coherente o el arranque falla (detección temprana).
- **Filas `DESK` huérfanas** antes de `desks` → el CHECK admite `DESK`, pero
  ninguna ruta del núcleo debe crear filas con ese tipo; cubierto por edge case.

## Migration Plan
- Flyway (`docs/data-model.md` §7): nueva migración `V5__generic_resource_refactor.sql`
  (número exacto a confirmar según el estado de migraciones). Pasos:
  1. Añadir `resource_id` y `resource_type` a `requests`, `fixed_assignments`,
     `releases` (con CHECK `IN ('PARKING','DESK')`).
  2. `UPDATE ... SET resource_id = parking_space_id, resource_type = 'PARKING'`.
  3. Recrear los índices de unicidad filtrados sobre las nuevas columnas
     (recurso/día activo, empleado/día activo, una `PENDING` por empleado/fecha[/tipo]).
  4. Mantener o reapuntar las FK; eliminar la columna `parking_space_id`
     antigua solo tras validar conteos (o dejarla deprecada una versión).
- **Sin migración de esquema de `desks`** (corresponde a la capability `desks`).
- Rollback: script inverso que restaura `parking_space_id` desde `resource_id`
  donde `resource_type = 'PARKING'`.
