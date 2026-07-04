# Proposal: init-releases

## Why
Inicializar la capability **releases** en OpenSpec: documentar como contrato
verificable el comportamiento de liberación de recursos con asignación fija
para una fecha concreta. La liberación es la pieza que reconcilia las
asignaciones fijas indefinidas con la disponibilidad puntual: permite que un
recurso fijo quede libre un día concreto (porque el titular no acude o el admin
lo libera por inasistencia) y, por tanto, sea solicitable por otros.

## What Changes
- Se añade la capability `releases` con sus Requirements y escenarios BDD.
- Endpoints: `GET /releases/mine` (`listMyReleases`), `POST /releases` (`createRelease`),
  `DELETE /releases/{id}` (`cancelRelease`), `POST /releases/administrative`
  (`createAdministrativeRelease`) — ver `docs/openapi.yaml`.
- Tipos `VOLUNTARY` (solo dueño, `release_date >= hoy`) y `ADMINISTRATIVE`
  (solo `ADMIN`, `reason` obligatorio).
- Una `Release` hace el recurso disponible para `release_date` en el cálculo
  de disponibilidad.

## Capabilities
- `releases` (ADDED)

## Impact
- **Entidades**: `Release` (`employee_id`, `released_by_id`, `release_date`,
  `type`, `reason`); lectura de `FixedAssignment`, `ParkingSpace`, `Employee`
  (ver `docs/data-model.md §3.4`).
- **Seguridad**: BOLA en cancelación/listado propio (`release.employee_id ==
  session.employee_id`); liberación administrativa restringida a `ADMIN`
  (ver `docs/security-design.md`).
- **UI**: botón "Liberar" sobre el recurso fijo del empleado y listado de mis
  liberaciones; en el panel admin, liberación administrativa con motivo.
- **Sin email** (la capability `notifications` no participa).

## Out of scope
- Cálculo de disponibilidad (lo consume `availability-calendar`).
- Asignación fija propiamente dicha (la aporta `fixed-assignments`).
- Liberación de puestos `DESK` (llega con `generic-resource-refactor` + `desks`).
- Notificaciones por email (no aplican a liberaciones).
