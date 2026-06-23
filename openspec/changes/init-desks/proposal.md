# Proposal: init-desks

## Why
Inicializar la capability **desks** (alcance ampliado de puestos de oficina) en
OpenSpec: documentar como contrato verificable la gestión de los 65 puestos
numerados y su integración en el ciclo de asignación fija, solicitud, liberación
y disponibilidad reutilizando la abstracción `BookableResource`. Permite que un
empleado disponga simultáneamente de plaza de parking y puesto de oficina.

## What Changes
- Se añade la capability `desks` con sus Requirements y escenarios BDD.
- Nueva entidad `Desk` (`number` 1-65, `DeskCategory` `STANDARD`/`EXECUTIVE`, `coord_x`/`coord_y`, `active`).
- Se proponen los endpoints `/desks` (CRUD + activación). _[no en openapi.yaml todavía]_
- Asignación fija, solicitud, liberación y disponibilidad de puestos se sirven por los endpoints genéricos con `resourceType = DESK` (reutilización, sin endpoints nuevos por recurso).

## Capabilities
- `desks` (ADDED)

## Impact
- **Entidades**: nueva `Desk`; reutilización de `FixedAssignment`, `Request`, `Release` vía `resource_id`/`resource_type` (`generic-resource-refactor`).
- **Seguridad**: CRUD de puestos restringido a `ADMIN`; solicitud/liberación de puesto para `EMPLOYEE` (ver `docs/security-design.md`).
- **UI**: pantallas de gestión de puestos (admin) y solicitud unificada plaza/puesto (empleado); el plano visual se aborda en `floor-plan`.

## Out of scope
- Plano interactivo, editor de arrastre y solicitud desde el plano (`floor-plan`).
- Reserva de visitante en puestos (no existe).
- El refactor de recurso genérico en sí (`generic-resource-refactor`, prerrequisito).
- Definición formal de los endpoints `/desks` en `docs/openapi.yaml` (se añadirán al implementar).
