## Context

`dbo.parking_spaces` (migración `V6`) es hoy una tabla mínima: `id BIGINT IDENTITY`, `label NVARCHAR(20)` único, `active BIT`, `created_at`. El `label` es texto libre (las 25 plazas dev/actuales usan `P-001…P-025`) y no contiene ninguna noción de planta. Tres tablas la referencian de forma **polimórfica por `id`** (`fixed_assignments`, `requests`, `releases` vía `resource_id` + `resource_type = 'PARKING'`, **sin FK** desde V13) y una de forma **directa por `id`** (`visitor_reservations.parking_space_id`, **con FK real**). Ninguna de esas referencias usa el `label`.

El propietario quiere organizar las plazas por **planta** y decide que la planta **no** sea un campo editable e independiente, sino una propiedad **derivada del número** de la plaza mediante un esquema *1000-based*: `floor = number / 1000` (división entera). Así, `1001…1999 → planta 1`, `2001…2999 → planta 2`, etc.

Restricción dura del propietario: **renumerar** las plazas existentes (no crear una tabla nueva ni migrar por `label`), **preservando todas las FKs y el histórico** asociado, lo que exige mantener el `id` estable.

## Goals / Non-Goals

**Goals:**
- Modelar la plaza con un **número entero ≥ 1000** del que se **deriva** la planta (`number / 1000`).
- Exponer `floor` en el DTO/UI y permitir **filtrar/agrupar** plazas por planta.
- **Renumerar** las 25 plazas existentes a números por planta **sin cambiar su `id`**, preservando asignaciones fijas, solicitudes, liberaciones y reservas de visitante históricas.
- Alta/edición por número con planta **derivada de solo lectura**.

**Non-Goals:**
- No se cambia el contrato de disponibilidad ni las FKs polimórficas (siguen operando por `id`).
- No se generaliza el concepto de planta a `desks` (los puestos tienen su propio `number` 1–65 y su plano; fuera de alcance).
- No se introduce una tabla `floors` ni gestión CRUD de plantas: la planta es un derivado, no una entidad.
- No se re-mapea `visitor_reservations` ni se toca su FK real.

## Decisions

### Decisión 1 — Planta derivada, no persistida
`floor` **no** se persiste como columna. Se calcula en el dominio/DTO como `number / 1000` (entero). 

- **Por qué:** evita duplicar un dato que siempre es función del número; elimina el riesgo de desincronización (número y planta divergentes) y de un `UPDATE` que deje planta incoherente. La derivación es O(1) y trivialmente testeable.
- **Alternativa considerada (columna `floor` persistida + trigger/`PERSISTED computed column`):** SQL Server soporta `floor AS (number / 1000) PERSISTED`. Se descarta como **columna base editable** (riesgo de incoherencia), pero una **columna computada `PERSISTED`** es una alternativa válida si se necesitara indexar/filtrar por planta a escala. Para el volumen actual (decenas de plazas) el filtro por rango de `number` (`number BETWEEN @f*1000 AND @f*1000+999`) es suficiente. → Ver *Open Questions*.

### Decisión 2 — `number` como columna entera dedicada (no parsear `label`)
Se añade `number INT NOT NULL` a `parking_spaces`, con índice único, y el `label` se deriva/renumera para reflejar el número (p. ej. `label = CAST(number AS NVARCHAR)` o formato `P-<number>`).

- **Por qué:** parsear la planta desde un `label` de texto libre es frágil. Una columna entera permite `CHECK (number >= 1000)`, orden natural y filtro por rango eficiente.
- **Alternativa (reutilizar `label` como numérico sin nueva columna):** menos migración pero pierde el tipo entero y las validaciones; se descarta.
- **Compatibilidad:** el `label` se mantiene (la capability `parking-spaces` y `visitor_reservations`/UI lo muestran) pero pasa a ser numérico/derivado del número. La unicidad se garantiza tanto en `number` como en `label`.

### Decisión 3 — Renumeración migratoria por `id`, sin tocar dependientes
La migración de datos actualiza **solo** `parking_spaces` (`SET number = …, label = …` por fila, ordenando por `id`), repartiendo las plazas en plantas `1..N`. Como las tablas dependientes referencian por `id` (polimórfico o FK real), **no requieren ningún cambio**.

- **Por qué:** el `id IDENTITY` es inmutable y es la clave real de todas las relaciones; renumerar el atributo humano no rompe integridad referencial. Es la opción de menor riesgo y sin downtime de datos relacionales.
- **Algoritmo de reparto (parametrizable):** con `spacesPerFloor = S` (p. ej. 25 o configurable), la fila `k`-ésima (0-based, orden por `id`) recibe `floor = 1 + (k / S)` y `number = floor*1000 + (k % S) + 1`. Con `S = 25` y 25 plazas → todas en planta 1 (`1001…1025`). Con `S` menor se reparten en varias plantas. El valor exacto de `S`/nº de plantas es una **decisión operativa** → *Open Questions*.

### Decisión 4 — Validación de alta/edición por `number`
El alta y la edición aceptan `number` (entero ≥ 1000), rechazan colisiones de `number`/`label` con 409 y valores inválidos con 400. La planta viaja en la respuesta como campo **derivado de solo lectura**; un intento de fijar `floor` directamente se ignora o rechaza (no es un campo de entrada).

## Risks / Trade-offs

- **[Renumeración cambia el identificador humano visible]** → Las plazas que un usuario conocía como `P-007` pasan a `1007` (o `P-1007`). Mitigación: comunicar el cambio; el histórico por `id` queda intacto, así que ninguna asignación/solicitud se pierde; documentar el mapeo `label_viejo → number` en la migración.
- **[Elección de plazas-por-planta arbitraria]** → repartir mal deja plantas desequilibradas. Mitigación: parametrizar `spacesPerFloor` y dejar el valor por defecto como Open Question a confirmar con el propietario antes de aplicar.
- **[`number` no derivable de planta editable]** → si alguien esperaba editar la planta directamente, no podrá. Mitigación: es una decisión de diseño explícita (planta = función del número); la UI lo muestra readonly.
- **[Colisión de `label` durante la renumeración]** → si el nuevo `label` chocara con un `label` viejo aún no migrado. Mitigación: hacer la renumeración en una sola sentencia/tx sobre el conjunto, o usar un `label` temporal; los `P-0xx` viejos no colisionan con `1xxx` nuevos.

## Migration Plan

1. **Esquema:** nueva migración Flyway `Vnn__parking_space_number.sql` que añade `number INT NULL` (temporalmente nullable), `CHECK (number >= 1000)`, e índice único `UX_parking_spaces_number`.
2. **Backfill/renumeración (misma o siguiente migración):** `UPDATE` de las filas existentes por `id` según el algoritmo de la Decisión 3, fijando `number` y `label`. Luego `ALTER COLUMN number INT NOT NULL`.
3. **Backend:** entidad `ParkingSpace` con `number`; DTO con `floor` derivado; validación alta/edición; endpoint de listado con filtro por planta; actualizar `docs/openapi.yaml` y `docs/data-model.md §3.2`.
4. **Frontend:** input `number` + planta derivada readonly en alta/edición; columna/selector de planta en gestión, plano y listados.
5. **Rollback:** las migraciones Flyway son forward-only; el rollback lógico consistiría en una migración compensatoria que reponga los `label` `P-0xx` desde el mapeo documentado y elimine la columna `number`. Dado que el `id` no cambia, el rollback no afecta a datos dependientes.

## Open Questions

1. **¿Cuántas plantas / cuántas plazas por planta (`spacesPerFloor`)?** Con 25 plazas actuales: ¿todas en planta 1 (`1001…1025`) o repartidas en varias plantas? Debe confirmarlo el propietario; el algoritmo queda parametrizado por `spacesPerFloor`.
2. **Formato del `label`**: ¿`"1007"` (número puro) o `"P-1007"` (prefijo conservado)? Afecta a lo que ve el usuario y a `visitor_reservations`/exports.
3. **¿Columna `floor` `PERSISTED` computada** en vez de derivación pura en app? Solo necesario si se requiere indexar/filtrar por planta a gran escala; para el volumen actual la derivación en dominio basta.
4. **Límite superior de `number`/plantas**: ¿hay un máximo de plantas del edificio a validar (p. ej. `number < 10000`)?
