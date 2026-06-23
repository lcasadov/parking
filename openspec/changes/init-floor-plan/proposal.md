# Proposal: init-floor-plan

## Why
Inicializar la capability **floor-plan** (alcance ampliado de puestos) en
OpenSpec: documentar como contrato verificable el plano interactivo de los 65
puestos de oficina —imagen de planta con un marcador por puesto coloreado por
estado para una fecha—, la solicitud directa desde el plano y el editor de
posiciones del admin. Es la cara visual del modelo de puestos y el principal
canal de reserva para el empleado.

## What Changes
- Se añade la capability `floor-plan` con sus Requirements y escenarios BDD.
- Endpoints **propuestos** (aún no en `docs/openapi.yaml`):
  - `GET /floor-plan?date={ISO_DATE}` (operationId `getFloorPlan`) _[no en openapi.yaml todavía]_
  - `PUT /floor-plan/desks/{deskId}/position` (operationId `updateDeskPosition`) _[no en openapi.yaml todavía]_
  - `POST /floor-plan/desks/{deskId}/request` (operationId `requestDeskFromFloorPlan`) _[no en openapi.yaml todavía]_
- Estado por puesto y fecha: `FREE`, `ASSIGNED`, `REQUESTED`, `MINE`, `RELEASED`, más distinción visual `EXECUTIVE`.
- Persistencia de `coord_x`/`coord_y` (% de la imagen, 0-100) vía editor de arrastre.

## Capabilities
- `floor-plan` (ADDED)

## Impact
- **Entidades**: `Desk` (`coord_x`, `coord_y`, `category`, `desk_number`, `active`); lectura de `FixedAssignment`, `Request`, `Release` vía `BookableResource`/`ResourceType=DESK`.
- **Seguridad**: ver plano = cualquier rol autenticado; editar posiciones = solo `ADMIN`; solicitar desde el plano = `EMPLOYEE` (ver `docs/security-design.md`).
- **UI**: vista de plano (imagen + marcadores) para empleado y editor de arrastre para admin. El parking no tiene plano.

## Out of scope
- Plano para plazas de parking (el parking se gestiona como lista numerada).
- Carga/gestión de la imagen de fondo de la planta (asset estático; fuera de este contrato).
- Reservas de visitante sobre puestos (no aplican a puestos).
- El formulario de solicitud unificada plaza/puesto (lo aporta `requests`/`desks`).
- Definición canónica de la entidad `Desk` y su CRUD (lo aporta `desks`).
