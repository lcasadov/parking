## Context

El admin se apoya en tablas paginadas en servidor (Spring `Pageable` + `@PageableDefault`). Los endpoints de solicitudes usan métodos de repositorio con el orden fijado en el nombre (`findByStatusOrderByCreatedAtAsc/Desc`, `findAllByOrderByCreatedAtDesc`), por lo que el `Sort` del `Pageable` no tiene efecto real. La UI actual renderiza `<th>` estáticos sin capacidad de orden. Este change introduce una capacidad transversal de ordenación y la estrena en Solicitudes.

## Goals / Non-Goals

**Goals:**
- Cabecera ordenable reutilizable (`SortableTh` + `useTableSort`) usable por cualquier tabla, con indicador visual y `aria-sort`.
- Ordenación en SERVIDOR para Solicitudes por `requestedDate` y `createdAt`, correcta a través de toda la paginación (no solo la página visible).
- Preservar el orden por defecto actual cuando el cliente no pide orden (sin regresiones).
- Blindar el servidor con whitelist de campos ordenables.

**Non-Goals:**
- Extender la ordenación a las demás tablas (roadmap más abajo); se hará en changes posteriores.
- Ordenación multi-columna, persistencia del orden en la URL o en preferencias del usuario.
- Ordenación en cliente de datos paginados (se descarta por engañosa).

## Decisions

- **Servidor, no cliente, para tablas paginadas.** Ordenar solo la página cargada daría un resultado incorrecto respecto al total. Solicitudes pagina en servidor, así que se envía `?sort=campo,dir` y el backend ordena el conjunto completo. (Las tablas que cargan todo el conjunto —p. ej. Puestos— podrán ordenar en cliente en su propio change; el hook `useTableSort` sirve para ambos modos.)
- **Formato de orden = contrato Spring.** El estado `{ field, dir }` se serializa a `sort=field,dir`. `useTableSort` expone tanto el estado como la cadena `sort` lista para la query; cuando el estado es `null` no se añade el parámetro.
- **Ciclo de 3 estados por columna.** sin-orden → asc → desc → sin-orden. El tercer clic vuelve al orden por defecto del servidor (útil para "deshacer" el orden). Cambiar de columna reinicia en asc.
- **Backend: separar orden por defecto de orden del cliente.** Se sustituyen los métodos con `OrderBy` en el nombre por `findByStatus(status, pageable)` / `findAll(pageable)`. En `RequestService.listPending`/`listByStatus`, si el `Pageable` entrante **no trae sort**, se construye un `Pageable` con el orden por defecto (pending: `createdAt ASC` FIFO; by-status: `createdAt DESC`); si trae sort, se **sanea contra la whitelist** y se usa, cayendo al defecto si el campo no está permitido.
- **Whitelist por endpoint.** Conjunto explícito de propiedades ordenables (`requestedDate`, `createdAt`). Evita `PropertyReferenceException`/ordenación por campos sensibles o no indexados y mantiene el contrato estable aunque cambie la entidad.
- **Reset de página al reordenar.** Cambiar el criterio de orden vuelve a `page = 0` para no dejar al usuario en una página inexistente/incoherente.
- **A11y.** La cabecera es un `<button>` dentro del `<th>`, con `aria-sort` en el `<th>` y `aria-label` "Ordenar por {columna}"; navegable por teclado.

## Roadmap (fuera de este change)

Reutilizando `table-sorting`, en changes posteriores:
- **Empleados** (Nombre, Departamento, Rol, Categoría, Estado) — servidor.
- **Recursos·Plazas** (Plaza/nº, Planta, Estado) — servidor.
- **Recursos·Puestos** (Número, Categoría, Estado) — CLIENTE (carga todo el inventario).
- **Visitantes·Fichas** (Nombre, DNI) y **·Reservas** (Fecha) — servidor.
- **Registros·Auditoría** (Fecha/occurredAt, Acción) y **·Accesos** (Fecha, Resultado) — servidor.
- **Liberar·Por fecha** (Recurso, Empleado, Origen) — CLIENTE. Tablas **"Mis…"** del empleado (Fecha).

Cada tabla paginada exigirá revisar su endpoint por el mismo patrón (`OrderBy` fijo vs `Pageable` sort) y su whitelist.

## Risks / Trade-offs

- **Métodos de repositorio con `OrderBy` usados en otros sitios.** Antes de eliminarlos, verificar que no se usan fuera de `listPending`/`listByStatus`; si se usan, conservar o migrar sus llamadas al orden por defecto explícito.
- **Rendimiento del orden.** Ordenar por `requestedDate`/`createdAt` sin índice adecuado puede degradar en tablas grandes; ambos campos ya se filtran por rango en otras queries, riesgo bajo, pero conviene confirmar índices.
- **Coherencia de la whitelist front/back.** El front solo ofrece cabeceras para campos permitidos; el back valida igualmente para no confiar en el cliente. Duplicidad menor asumida a cambio de robustez.
- **Zona horaria en `createdAt`.** El orden usa el instante persistido; sin cambios respecto al comportamiento actual.
