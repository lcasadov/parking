# Design: init-releases

## Context
La liberación opera sobre una asignación fija activa: marca un recurso ya
asignado como disponible para una fecha concreta. Dos orígenes con reglas
distintas — voluntaria (la ejecuta el titular para sí mismo) y administrativa
(la ejecuta un admin sobre el recurso de otro, por inasistencia). El modelo
distingue `employee_id` (dueño cuyo recurso se libera) de `released_by_id`
(quien ejecuta), lo que permite trazar ambos casos con la misma tabla.
Arquitectura hexagonal: las reglas de ventana temporal y obligatoriedad de
`reason` viven en el dominio (`ReleaseUseCase`), no en el controlador.

## Goals
- Permitir liberar un recurso fijo para una fecha presente/futura de forma trazable.
- Garantizar BOLA: un empleado solo ve y cancela liberaciones propias.
- Evitar liberaciones duplicadas del mismo recurso y fecha bajo concurrencia.
- Que la liberación se refleje de inmediato en la disponibilidad.

## Decisions
- **Dos endpoints de creación separados** (`POST /releases` voluntaria vs
  `POST /releases/administrative`): RBAC y validación divergen (la voluntaria
  fija dueño = ejecutor y prohíbe `reason`; la administrativa exige `reason` y
  `employeeId`). Separarlos evita ramas condicionales por rol en un único handler.
- **`reason` obligatorio solo en `ADMINISTRATIVE`**, validado en servicio (no en
  el esquema, que permite `NULL` para `VOLUNTARY`) — coherente con `data-model.md §3.4`.
- **Ventana temporal `release_date >= hoy`** evaluada con `ClockPort` inyectable
  para testear el límite "hoy" de forma determinista; el pasado → 400.
- **Resolución de plaza implícita**: si `createRelease` omite `parkingSpaceId`,
  el servicio resuelve la `FixedAssignment` activa del empleado para el día de la
  semana de `releaseDate`; ambigüedad o ausencia → 409.
- **Unicidad recurso+fecha**: índice/comprobación que impide dos liberaciones
  activas del mismo recurso para la misma `release_date`; el conflicto → 409.
  Bajo concurrencia, la unicidad a nivel de BD garantiza que solo una gane.
- **Cancelación = borrado de la fila futura** (no baja lógica): `releases` es
  histórico purgable; una liberación futura anulada no necesita conservarse.
  Liberaciones pasadas no se cancelan → 409.
- **Sin notificaciones**: por regla de negocio, liberar/cancelar no dispara email.

## Risks
- **BOLA** en `cancelRelease`/`listMyReleases`: mitigado verificando
  `release.employee_id == session.employee_id` antes de cualquier efecto; rol no basta.
- **Carrera al liberar** el mismo recurso/fecha desde dos sesiones: mitigada con
  unicidad en BD (uno crea 201, el otro 409).
- **Liberación huérfana** si la asignación fija se revoca después: la fila persiste
  como histórico pero no añade disponibilidad útil; aceptado como no-problema.
- **Confusión dueño/ejecutor** en administrativa: mitigada con campos separados y
  tests que verifican `released_by_id` = admin, `employee_id` = titular.

## Migration Plan
- Flyway: tabla `releases` con `FK_releases_parking_spaces`,
  `FK_releases_employee`, `FK_releases_released_by`, `CK_releases_type`
  (`VOLUNTARY`/`ADMINISTRATIVE`) e índice `IX_releases_parking_space_id_date`
  (ver `docs/data-model.md §3.4` y §Indexes).
- Sin migración de datos (capability nueva).
- En el alcance ampliado, `generic-resource-refactor` migrará
  `parking_space_id → resource_id`; fuera del scope de este change.
