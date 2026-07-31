## Why

Ninguna tabla del panel de administración permite ordenar por columna: para encontrar la solicitud más antigua/reciente, el empleado más nuevo o el último acceso, el ADMIN depende del orden fijo que impone el backend. El usuario pide, como mínimo, poder ordenar **Solicitudes por fecha** (Fecha solicitada y Creada) y sentar una base reutilizable para extender la ordenación al resto de tablas de forma consistente.

## What Changes

- **Nueva capacidad `table-sorting` (frontend):** un componente de cabecera ordenable `SortableTh` y un hook `useTableSort` que mantienen el estado de orden `{ field, dir } | null` y lo serializan al formato de Spring (`?sort=campo,dir`). Ciclo de clic: sin orden → ascendente → descendente → sin orden. Indicador visual (▲/▼/neutro) y `aria-sort` en la cabecera.
- **Solicitudes ordenable en SERVIDOR:** la bandeja de Pendientes (`GET /requests/pending`) y la lista por estado (`GET /requests`) admiten ordenación por `requestedDate` (Fecha solicitada) y `createdAt` (Creada). El cliente envía `sort`; al cambiar el orden se vuelve a la página 0.
- **Backend — respetar el `Sort` del `Pageable`:** hoy los métodos de repositorio usados fuerzan el orden en el nombre (`findByStatusOrderByCreatedAtAsc/Desc`, `findAllByOrderByCreatedAtDesc`), lo que impide ordenar por otra columna. Se cambia el servicio para usar métodos **sin `OrderBy`** aplicando un **orden por defecto** (createdAt) cuando el cliente no pide sort, y respetando el sort del cliente cuando sí. Se añade una **whitelist** de campos ordenables (`requestedDate`, `createdAt`); un sort no permitido cae al orden por defecto (no se expone ordenación por propiedades arbitrarias de la entidad).
- **Fuera de alcance (roadmap):** extender la ordenación al resto de tablas (Empleados, Recursos, Visitantes, Registros, Liberar, "Mis…") se hará en changes posteriores reutilizando `table-sorting`; cada tabla paginada requerirá revisar su endpoint (mismo patrón `OrderBy` fijo). Ver la sección Roadmap en `design.md`.

## Capabilities

### New Capabilities
- `table-sorting`: mecanismo transversal de ordenación de tablas del admin — cabecera ordenable (`SortableTh` + `useTableSort`), contrato de estado de orden y su traducción a `?sort=campo,dir`, y la política de whitelist de campos ordenables por endpoint.

### Modified Capabilities
- `requests`: las listas de solicitudes (bandeja de Pendientes y lista por estado) pasan a admitir ordenación por `requestedDate` y `createdAt` vía el `Sort` del `Pageable`, con orden por defecto cuando no se indica y whitelist de campos permitidos.

## Impact

- **Frontend nuevo:** `src/components/SortableTh.tsx`, `src/hooks/useTableSort.ts` (capacidad reutilizable).
- **Frontend modificado:** `src/pages/PendingRequestsPage.tsx` (cabeceras ordenables en Fecha solicitada / Creada), `src/types/request.ts` (`RequestListParams` gana `sort?`), `src/api/requestsApi.ts` (pasar `sort` a `GET /requests/pending` y `GET /requests`), `src/hooks/useRequests.ts` (propagar `sort`), i18n (aria-labels de "ordenar por…").
- **Backend modificado:** `RequestService.listPending` / `listByStatus` (aplicar sort por defecto + whitelist), puerto/adaptador de repositorio de `requests` (métodos sin `OrderBy`: `findByStatus(status, pageable)`, `findAll(pageable)`). Sin cambios de esquema ni migraciones.
- **Contrato API:** `GET /requests/pending` y `GET /requests` documentan el parámetro `sort` (Spring Pageable) y los campos permitidos; comportamiento por defecto sin cambios cuando no se envía `sort`.
- **Sin breaking changes:** el orden por defecto se preserva cuando el cliente no envía `sort`.
