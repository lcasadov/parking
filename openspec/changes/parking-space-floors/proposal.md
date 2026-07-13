## Why

Las plazas de garaje se agrupan físicamente en **plantas**, pero el modelo actual solo guarda un `label` textual libre (`P-001…P-025`) sin ninguna noción de planta. El propietario necesita ver, filtrar y gestionar las plazas por planta. La convención acordada es derivar la planta del **número** de la plaza mediante un esquema *1000-based* (1000–1999 → planta 1, 2000–2999 → planta 2, …), de modo que la planta no sea un dato duplicado y editable, sino una propiedad calculada del número.

## What Changes

- **Plaza numerada (esquema 1000-based)**: cada plaza pasa a identificarse por un **número entero** (≥ 1000) del que se **deriva** su planta como `numero / 1000` (división entera). El `label` deja de ser texto libre y pasa a reflejar el número.
- **Planta expuesta**: se expone la `floor` (planta) en el DTO de plaza y en la UI; se permite **filtrar y mostrar** plazas por planta en gestión, plano y listados.
- **Alta/edición por número**: el alta y la edición de una plaza usan el **número**; la planta es **derivada y de solo lectura** (no editable directamente).
- **BREAKING — Renumeración de datos existentes**: las 25 plazas actuales (`P-001…P-025`, no numéricas) se **renumeran** a números por planta (p. ej. `1001,1002,…,2001,…`) repartidas en plantas `1..N`. Se **preservan las FKs por `id`**: `fixed_assignments`, `requests`, `releases` y `visitor_reservations` referencian `resource_id`/`parking_space_id` = **id** de la plaza, no su número/label, por lo que las asignaciones fijas, solicitudes, liberaciones y reservas de visitante históricas quedan intactas. Cambia el número/`label`; el `id` **no**.
- **Frontend**: la pantalla de gestión de plazas y el plano/listados muestran la planta; el formulario de alta/edición usa número con planta derivada de solo lectura.

## Capabilities

### New Capabilities
<!-- Ninguna capability nueva: el cambio evoluciona el recurso plaza existente. -->

### Modified Capabilities
- `parking-spaces`: se añade el concepto de **número** (esquema 1000-based) y **planta derivada** al modelo de plaza; se modifican los requisitos de alta y edición (validación del número/planta en lugar de `label` libre) y se añade el requisito de renumeración migratoria preservando FKs por `id`, más el filtrado/exposición por planta.

## Impact

- **Base de datos** (`backend/src/main/resources/db/migration/`): nueva migración de esquema y datos sobre `dbo.parking_spaces` (columna `number` y/o normalización de `label` a numérico, índice único, backfill/renumeración de las 25 filas existentes por `id`). No se tocan las tablas dependientes (`fixed_assignments`, `requests`, `releases`, `visitor_reservations`) porque referencian por `id`.
- **Backend**: entidad `ParkingSpace`, DTO de plaza (nuevo campo `floor` derivado), servicio y validación de alta/edición, endpoints `parking-spaces` (filtro por planta), `docs/openapi.yaml`, `docs/data-model.md`.
- **Frontend**: pantalla de gestión de plazas (columna/selector de planta), plano y listados (etiqueta de planta), formulario de alta/edición (input número + planta derivada readonly).
- **Fuera de sistemas dependientes**: la lógica de disponibilidad y las FKs polimórficas no cambian de contrato (siguen operando por `id`).
