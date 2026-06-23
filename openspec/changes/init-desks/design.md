# Design: init-desks

## Context
Los puestos de oficina se incorporan como un segundo tipo de recurso reservable.
El núcleo de parking ya implementa asignación fija, solicitud, liberación y
disponibilidad sobre plazas. Tras `generic-resource-refactor`, esas tablas
referencian `resource_id` + `resource_type` (`PARKING`/`DESK`) en lugar de
`parking_space_id`. `desks` aporta la entidad `Desk` y el CRUD, y conecta los
puestos al ciclo existente sin duplicar lógica de reserva.

## Goals
- Modelar los 65 puestos numerados con categoría y coordenadas.
- Reutilizar íntegramente el ciclo de reserva vía `BookableResource` (`DESK`).
- Permitir plaza fija y puesto fijo simultáneos para un mismo empleado.
- Tratar `EXECUTIVE` como una distinción visual, no como una excepción de reglas.

## Decisions
- **Tabla `desks` hermana de `parking_spaces`**, no herencia de tabla única. *Por qué:* el `generic-resource-refactor` ya provee el discriminador `resource_type`; tablas separadas mantienen claras las columnas propias (`number`, `category`, `coord_x`, `coord_y`) sin nulos en `parking_spaces`.
- **`number` con CHECK 1-65 y unicidad**. *Por qué:* el conjunto reservable es fijo y numerado; la restricción a nivel BD garantiza que no se exceda ni se dupliquen puestos.
- **`coord_x`/`coord_y` como porcentaje (0-100)**. *Por qué:* independiza la posición de la resolución de la imagen del plano; su edición fina pertenece a `floor-plan`.
- **`DeskCategory` no altera reglas de reserva**. *Por qué:* `EXECUTIVE` se libera y solicita igual que `STANDARD`; solo cambia su representación visual, evitando ramas de negocio por categoría.
- **Sin `VisitorReservation` para `DESK`**. *Por qué:* los visitantes solo usan parking; el cálculo de disponibilidad de puesto omite ese término.
- **Solicitud unificada como conveniencia de UI**: una pantalla genera `Request` independientes (`PARKING` y/o `DESK`); no existe entidad agrupada. *Por qué:* aprobación/rechazo por separado y trazabilidad por recurso.

## Risks
- **Acoplamiento con `generic-resource-refactor`**: si el refactor no está completo, las claves de unicidad por `(employee, date, resource_type)` no funcionan → mitigación: `desks` se implementa estrictamente después del refactor y sus tests de unicidad lo verifican.
- **Confusión plaza/puesto en UI**: dos recursos del mismo empleado el mismo día → mitigación: solicitudes independientes y etiquetado claro por tipo.
- **Coordenadas inválidas o ausentes**: puestos sin posición → mitigación: validación 0-100 y valor por defecto centrado hasta posicionar en `floor-plan`.

## Migration Plan
- Flyway: crear tabla `desks` (`id`, `number` CHECK 1-65, `category`, `coord_x`, `coord_y`, `active`) con índice único en `number` (ver `docs/data-model.md` §"Future desks").
- Datos: seed opcional de los 65 puestos `STANDARD` con coordenadas neutras.
- Las tablas `fixed_assignments`, `requests`, `releases` ya admiten `resource_type = DESK` tras `generic-resource-refactor`; `desks` no las migra de nuevo.
