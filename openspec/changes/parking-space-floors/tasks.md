## 1. Base de datos (migración esquema + datos)

- [ ] 1.1 Confirmar con el propietario el parámetro `spacesPerFloor` y el formato de `label` (número puro vs `P-<number>`) — resolver Open Questions 1 y 2 de `design.md` antes de fijar los valores de la migración.
- [ ] 1.2 Crear migración Flyway `Vnn__parking_space_number.sql`: añadir `number INT NULL`, `CONSTRAINT CK_parking_spaces_number CHECK (number >= 1000)`.
- [ ] 1.3 En la misma migración, renumerar las 25 filas existentes por `id` (algoritmo Decisión 3: `floor = 1 + (k / S)`, `number = floor*1000 + (k % S) + 1`) y actualizar `label` acorde, sin tocar `id`.
- [ ] 1.4 `ALTER COLUMN number INT NOT NULL` y crear `CREATE UNIQUE INDEX UX_parking_spaces_number ON dbo.parking_spaces(number)`.
- [ ] 1.5 Actualizar el/los seeds dev de plazas (si existen) para emitir plazas numeradas 1000-based en vez de `P-0xx`.
- [ ] 1.6 Actualizar `docs/data-model.md §3.2` (columna `number`, índice, nota de planta derivada) y el inventario de migraciones §7.

## 2. Tests backend (TDD — Red primero)

- [ ] 2.1 Test unitario de derivación de planta: `floor(number) == number / 1000` para 1007→1, 2001→2, 3025→3, 12010→12.
- [ ] 2.2 Test de migración/repositorio: tras renumerar, una plaza conserva su `id` y las FKs (`fixed_assignments`/`requests`/`releases`/`visitor_reservations`) siguen resolviendo por `id`.
- [ ] 2.3 Test de servicio de alta: `number` nuevo → 201 con `floor` derivado; `number` duplicado → 409; `number` ausente/no entero/<1000 → 400.
- [ ] 2.4 Test de servicio de edición: cambio de `number` recalcula `floor`/`label`; colisión de `number` → 409; plaza inexistente → 404; `active=false` excluye de disponibilidad.
- [ ] 2.5 Test de listado con filtro `floor`: devuelve solo plazas del rango `[floor*1000, floor*1000+999]`; el DTO incluye `number`, `label` y `floor`.

## 3. Implementación backend (Green)

- [ ] 3.1 Entidad `ParkingSpace`: añadir `number` (Integer), mapear columna; derivar `floor` en el dominio (`number / 1000`) sin persistirlo.
- [ ] 3.2 DTO de plaza: exponer `floor` de solo lectura junto a `number` y `label`; asegurar que `floor` no es campo de entrada en alta/edición.
- [ ] 3.3 Servicio de alta/edición: validar `number` (≥ 1000, entero), unicidad `number`/`label`, derivar `label` desde `number`, recalcular `floor` en cada cambio.
- [ ] 3.4 Repositorio/servicio de listado: soportar filtro por planta (rango de `number`) y ordenación por `number`.
- [ ] 3.5 Actualizar `docs/openapi.yaml`: esquema de plaza con `number` (entrada) y `floor` (readonly), y parámetro de query `floor` en el listado.

## 4. Frontend

- [ ] 4.1 Formulario de alta/edición de plaza: input `number` (entero ≥ 1000) + planta derivada mostrada de solo lectura (se recalcula al teclear).
- [ ] 4.2 Pantalla de gestión de plazas: columna/etiqueta de planta y selector de filtro por planta.
- [ ] 4.3 Plano y listados: mostrar la planta de cada plaza.
- [ ] 4.4 Cliente API: incluir el parámetro de filtro `floor` y mapear el campo `floor` derivado del DTO.
- [ ] 4.5 Tests de componente/UI: validación del `number`, planta derivada readonly, filtro por planta.

## 5. Verificación y Quality Gate

- [ ] 5.1 Backend: `mvn clean verify` verde, cobertura ≥80% líneas / ≥75% branches, 0 violations Sonar nuevas.
- [ ] 5.2 Frontend: `npm run lint && npm test && npm run build` sin errores, cobertura ≥80%.
- [ ] 5.3 Verificación de datos: migración aplicada en entorno `des` renumera las 25 plazas y las FKs históricas siguen intactas (asignaciones/solicitudes/liberaciones/reservas por `id`).
- [ ] 5.4 `openspec validate parking-space-floors` verde antes de crear la PR.
