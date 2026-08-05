## 1. Frontend — capacidad reutilizable `table-sorting`

- [x] 1.1 Crear `src/hooks/useTableSort.ts`: estado `{ field, dir } | null`, `toggle(field)` con ciclo sin-orden → asc → desc → sin-orden (cambiar de columna reinicia en asc) y `sortParam` (cadena `campo,dir` o `undefined`).
- [x] 1.2 Crear `src/components/SortableTh.tsx`: `<th>` con `<button>` (etiqueta + icono ▲/▼/neutro), `aria-sort` según estado y `aria-label` "Ordenar por {columna}". Props: `field`, `label`, `sort`, `onToggle`, `className?`.
- [x] 1.3 Estilos de la cabecera ordenable (icono neutro/activo, foco visible) en el CSS de tablas.
- [x] 1.4 i18n: clave genérica de aria-label "Ordenar por {{column}}" (es/en).

## 2. Frontend — Solicitudes ordenable en servidor

- [x] 2.1 `src/types/request.ts`: añadir `sort?: string` a `RequestListParams`.
- [x] 2.2 `src/api/requestsApi.ts`: pasar `sort` como query param en `listPendingRequests` y `listRequests`/by-status (omitir si `undefined`).
- [x] 2.3 `src/hooks/useRequests.ts`: propagar `sort` (ya viaja en `params` → queryKey + queryFn, automático).
- [x] 2.4 `src/pages/PendingRequestsPage.tsx`: usar `useTableSort`, sustituir los `<th>` de "Fecha solicitada" (`requestedDate`) y "Creada" (`createdAt`) por `SortableTh`, pasar `sortParam` a la query y volver a `page = 0` al reordenar.

## 3. Backend — respetar el `Sort` del `Pageable` en solicitudes

- [x] 3.1 Repositorio de `requests` (puerto + adaptador JPA): añadidos métodos sin `OrderBy` (`findByStatus(status, pageable)`, `findAll(pageable)`); los `...OrderBy...` solo se usaban en `listPending`/`listByStatus` (main) + fakes de test (actualizados con stubs).
- [x] 3.2 Whitelist de campos ordenables (`requestedDate`, `createdAt`) y helper `withResolvedSort` que sanea el `Sort` entrante contra ella (campo no permitido → se descarta).
- [x] 3.3 `RequestService.listPending`: orden por defecto `createdAt ASC` (FIFO) sin sort permitido; usa el del cliente si lo trae. Ahora vía `findByStatus`.
- [x] 3.4 `RequestService.listByStatus`: orden por defecto `createdAt DESC` sin sort permitido; usa el del cliente si lo trae. Ahora vía `findByStatus`/`findAll`.
- [x] 3.5 Documentado el parámetro `sort` y los campos permitidos (`requestedDate`/`createdAt`) + orden por defecto en el `@Operation.description` de `GET /requests/pending` y `GET /requests` (springdoc auto-documenta el `sort` del `Pageable`).

## 4. Tests y Quality Gate

- [x] 4.1 Backend (`RequestServiceTest`, 5 tests nuevos): `listPending`/`listByStatus` aplican el orden por defecto (createdAt ASC/DESC) sin `sort`; respetan el sort permitido del cliente; un campo no permitido cae al orden por defecto. Corregido `withResolvedSort` para soportar `Pageable.unpaged()`.
- [x] 4.2 Frontend (`useTableSort.test.ts` 2, `SortableTh.test.tsx` 4): el hook cicla none→asc→desc→none y reinicia en asc al cambiar de columna, con `sortParam` correcto; `SortableTh` refleja `aria-sort` (none/ascending/descending) y llama a `onToggle` con el campo. `PendingRequestsPage.test.tsx` (16) sigue verde con las cabeceras ordenables.
- [x] 4.3 Verificación: front `tsc`+`eslint`+`build` verdes y tests nuevos (6) + `PendingRequestsPage` (16) verdes; back `test-compile` OK y suite completa **819 tests / 0 fallos del módulo de solicitudes** (`RequestServiceTest` 68/68 con los 5 nuevos). Nota: la suite completa reporta 2 fallos PRE-EXISTENTES ajenos a este change (restricción nº de puesto 1–65 retirada antes, y upsert de push), y la suite de front tiene tests desincronizados por los rediseños de UI previos de esta rama — pendientes de la pasada de consolidación.
