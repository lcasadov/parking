## Why

El portal del empleado tiene varias fricciones detectadas revisando la app: al
liberar un recurso fijo y volver a reservar no se recupera el propio; no hay forma
cómoda de soltar los recursos cuando te ausentas (vacaciones); "Mis solicitudes" y
"Mis plazas" son confusas; el plano no aporta como sección; el cerrar sesión abre un
menú redundante; y el nombre "parking" se queda corto (ya gestiona plazas y puestos).
En el admin, reasignar recursos entre empleados para una fecha es tedioso: hay que
reasignar uno a algo temporal y luego el otro, sin intercambio directo.

## What Changes

- **Auto-asignación**: al reservar, si el empleado tiene un recurso FIJO ese día de la
  semana y está libre, se le reasigna EL SUYO antes de la asignación por categoría
  (plaza y puesto). La UI lo avisa.
- **Ausencia/vacaciones (empleado)**: acción destacada en "Mis sitios fijos" para
  liberar plaza y/o puesto en días sueltos o un rango, en lote.
- **Mi Semana**: botón "Mapa" (plano con el asiento resaltado) en las tarjetas de
  puesto; botón "Ir al parking" (navegación externa a la dirección del parking) solo
  en Hoy y Mañana.
- **Mis solicitudes**: quitar exportar y "Nueva solicitud"; navegación por meses
  (filtra por fecha de recurso, ambos sentidos sin tope); mostrar fecha de recurso y
  fecha de solicitud; botón de acción según estado (PENDING → "Cancelar solicitud",
  APPROVED → "Liberar") con modal y toast coherentes.
- **Mis plazas → "Mis sitios fijos"**: página informativa (read-only), una fila por
  recurso con sus días; botón "Mapa" en filas de puesto.
- **Navegación empleado**: quitar la sección "Plano".
- **App shell**: renombrar la app a **"Reservas"**; cerrar sesión con diálogo de
  confirmación (sin popover redundante); quitar "Exportar mis datos".
- **Admin (Ocupación)**: reasignar el recurso de una asignación por fecha e
  **intercambiar (swap) recursos entre dos empleados** en una operación atómica, con
  **email a ambos** afectados.

## Capabilities

### New Capabilities
- `admin-resource-reassignment`: reasignación e intercambio (swap) de recursos entre
  empleados para una fecha concreta desde Ocupación, atómico y con notificación por email.
- `employee-absence-release`: liberación en lote de los recursos fijos del empleado para
  días sueltos o un rango (flujo de ausencia/vacaciones).

### Modified Capabilities
- `requests`: la auto-asignación prefiere el recurso fijo propio si está libre; `listMine`
  admite filtro por rango de fechas (mes) sobre `requestedDate`.
- `employee-portal`: rediseño de Mi Semana (botones mapa/parking), Mis solicitudes
  (meses, dos fechas, acción por estado, sin exportar/nueva) y Mis sitios fijos (read-only);
  se elimina la sección Plano del empleado.
- `app-shell`: renombrado a "Reservas", cierre de sesión con confirmación y sin popover
  duplicado, sin "Exportar mis datos".

## Impact

- Backend: `RequestService` (auto-asignación + `listMine` con rango de fechas),
  nuevo endpoint de reasignación/swap admin (`requests`/`occupancy`), notificaciones email.
- Frontend: `MyWeekPage`, `MyRequestsPage`, `MyResourcesPage` (→ Mis sitios fijos),
  `AppShell`/`SidebarUserCard`, rutas del empleado (quitar Plano), `OccupancyPage` +
  modal de asignación, reutilización de `FloorPlanFocusModal` y `useBatchRelease`, i18n es/en.
- Sin cambios de esquema de BD (reutiliza fixed-assignments, requests, releases).
