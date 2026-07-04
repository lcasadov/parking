# Proposal: init-generic-resource-refactor

## Why
El núcleo de parking modela toda referencia reservable con `parking_space_id`,
acoplado a `parking_spaces`. Para incorporar puestos de oficina (`desks`) sin
duplicar la lógica de asignación fija, solicitud, liberación y disponibilidad,
hay que generalizar esa referencia a `resource_id` + `resource_type`
(`ResourceType`: `PARKING`/`DESK`) e introducir la abstracción de dominio
`BookableResource`. Este refactor es **prerrequisito** de `desks` y, transitivamente,
de `floor-plan`. Se ejecuta **sin cambio de comportamiento visible**: los tests
existentes deben seguir verdes.

## What Changes
- Se añade la capability `generic-resource-refactor` con Requirements de
  generalización del modelo, no-regresión y migración de datos.
- Se sustituye `parking_space_id` por `resource_id` + `resource_type` en
  `Request`, `FixedAssignment` y `Release`.
- Se introduce el enum `ResourceType` (`PARKING`, `DESK`) y la abstracción de
  dominio `BookableResource`.
- Se generaliza el cálculo de disponibilidad para filtrar por `resource_type`,
  preservando el resultado para `PARKING`.
- Migración Flyway de datos: porta las referencias existentes a `resource_id`
  con `resource_type = 'PARKING'` y reexpresa los índices de unicidad sobre las
  nuevas columnas.
- **Sin endpoints nuevos**: ruta, `operationId`, payload y semántica de
  `requests`, `fixed-assignments`, `releases` y `availability-calendar` no cambian.

## Capabilities
- `generic-resource-refactor` (ADDED)

## Impact
- **Entidades**: `Request`, `FixedAssignment`, `Release` (columna y FK
  renombradas/portadas); nuevo enum `ResourceType`; abstracción `BookableResource`.
  `ParkingSpace` se mantiene; `VisitorReservation` **no** se generaliza.
- **Seguridad**: sin cambios de RBAC ni de comprobación de pertenencia de objeto
  (API1). Roles `ADMIN`/`EMPLOYEE` igual que antes.
- **UI**: ninguna (refactor interno; el comportamiento observable no cambia).
- **Migración**: Flyway de datos (ver `docs/data-model.md` §"requests resource discriminator").

## Out of scope
- La entidad `Desk` y sus endpoints (capability `desks`).
- El plano interactivo (`floor-plan`).
- Generalizar `VisitorReservation` a recurso genérico (las reservas de visitante
  siguen siendo solo de `PARKING`).
- Cualquier cambio de contrato HTTP o de mensajes de error.
