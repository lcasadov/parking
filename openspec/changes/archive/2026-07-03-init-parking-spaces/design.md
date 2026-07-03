# Design: init-parking-spaces

## Context
`ParkingSpace` es el recurso reservable del núcleo de parking: una tabla viva
(`parking_spaces`) con `label` único e indicador `active`. Todas las FKs
reservables (`fixed_assignments`, `releases`, `requests`, `visitor_reservations`)
apuntan a esta tabla. Arquitectura hexagonal: el caso de uso de gestión de
plazas no depende de Spring; el adaptador JPA implementa el puerto de
repositorio.

## Goals
- CRUD de plazas seguro y trazable, restringido a `ADMIN`.
- Garantizar la unicidad de `label` a nivel de dominio y de base de datos.
- Configuración masiva del total sin pérdida de histórico de plazas existentes.
- Excluir de forma fiable las plazas inactivas del cálculo de disponibilidad.

## Decisions
- **Unicidad de `label`**: índice único `UX_parking_spaces_label` (no filtrado,
  `label NOT NULL`); la colisión se traduce a 409 en la capa de aplicación,
  además de validar en el dominio para dar un mensaje claro.
- **Baja lógica, no física**: el estado se gobierna con `active` (BIT, default 1).
  Desactivar nunca borra la fila ni su histórico; las consultas de disponibilidad
  filtran por `active = true`.
- **Configuración masiva (`configure`)**: el cuerpo es `{ total }` (entero ≥ 0,
  ver `ParkingSpaceConfigureRequest`); ajusta el número de plazas del parque y
  devuelve el listado resultante. Reducir por debajo del existente desactiva
  plazas sobrantes preservando su histórico _[verificar con docs/data-model.md:
  el modelo solo describe "alta/ajuste del total"]_.
- **Filtro de listado**: `listParkingSpaces` acepta `active` opcional y pagina
  (`page`/`size`); sin filtro devuelve activas e inactivas.
- **Forma de error uniforme**: `{ error, message, fields, timestamp }`
  (autoridad `docs/openapi.yaml`).

## Risks
- Carrera en altas concurrentes con el mismo `label`: mitigada por el índice
  único de base de datos (segunda inserción → 409), no solo por comprobación
  previa en memoria.
- `configure` con `total` reducido podría dejar plazas con asignaciones futuras
  inactivas: la coherencia con `fixed-assignments`/`requests` se valida en esas
  capabilities; aquí solo se cambia el estado.

## Migration Plan
- Flyway: tabla `parking_spaces` e índice `UX_parking_spaces_label`
  (ver `docs/data-model.md` §3.2). Sin migración de datos (capability nueva);
  el seed de plazas iniciales, si lo hay, se entrega como migración aparte.
