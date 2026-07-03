# Proposal: init-parking-spaces

## Why
Inicializar la capability **parking-spaces** en OpenSpec: dejar documentado
el comportamiento del catálogo de plazas (alta, edición, estado activa/inactiva
y configuración masiva del total) como contrato verificable antes de
implementarlo. Las plazas son el recurso reservable base del que dependen
asignaciones fijas, liberaciones, solicitudes, reservas de visitante y la
disponibilidad.

## What Changes
- Se añade la capability `parking-spaces` con sus Requirements y escenarios BDD.
- Endpoints: `GET /parking-spaces` (listado paginado con filtro `active`),
  `POST /parking-spaces`, `PUT /parking-spaces/{id}`,
  `POST /parking-spaces/configure` (ver `docs/openapi.yaml`).
- Unicidad de `label` con conflicto 409; validación de campos con 400;
  autorización exclusiva `ADMIN` con 403.

## Capabilities
- `parking-spaces` (ADDED)

## Impact
- **Entidades**: `ParkingSpace` (`label`, `active`, `created_at`).
- **Seguridad**: gestión restringida a `ADMIN` (ver `docs/security-design.md`);
  el `EMPLOYEE` solo percibe plazas vía disponibilidad.
- **UI**: pantalla de administración de plazas (alta/edición, toggle activa,
  configuración del total). Solo visible para `ADMIN`.

## Out of scope
- Asignación de plazas a empleados (`fixed-assignments`).
- Liberaciones, solicitudes y reservas de visitante sobre plazas.
- Cálculo de disponibilidad (`availability-calendar`).
- Puestos de oficina y plano (`desks`, `floor-plan`) y la abstracción
  `BookableResource` (`generic-resource-refactor`).
