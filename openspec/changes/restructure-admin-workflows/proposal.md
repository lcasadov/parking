## Why

La aplicación funciona a nivel transaccional, pero su capa de administración se construyó **pantalla por endpoint** en lugar de **por tarea del usuario**: el ADMIN tiene 13 entradas de menú (varias de solo lectura y varias duplicadas), y para la operación más común —"dar una plaza/puesto a un empleado para un día concreto"— **no existe ningún camino**. Cuatro análisis funcionales independientes (orquestador + 3 agentes) convergen en el mismo diagnóstico: *ver* y *actuar* están divorciados, y la generalización PARKING/DESK quedó incompleta en las vistas clave. Esta reestructuración corrige **arquitectura de información y funcionalidad** como paso previo y necesario al rediseño estético (fase posterior separada): no tiene sentido "ponerlo bonito" sobre una estructura equivocada.

## What Changes

- **Navegación reorganizada por tarea.** ADMIN de 13 → 9 entradas (Operativa: Solicitudes, Ocupación, Plano, Liberar, Visitantes · Gestión: Empleados, Recursos, Registros, Ajustes). EMPLOYEE de 5 → 4 con **"Mi Semana" como ruta índice** (hoy aterriza en un listado tabular). **BREAKING** para rutas/URLs internas.
- **Fusiones de secciones sin eliminar ninguna página:**
  - "Asignación semanal" + "Disponibilidad" → **"Ocupación"** (rejilla accionable, cruza plaza y puesto por fecha).
  - "Liberaciones" + "Liberar por fecha" → **"Liberar"** (un destino, pivote por-empleado / por-fecha).
  - "Plazas" + "Puestos" → **"Recursos"** (pestañas). "Auditoría" + "Accesos" → **"Registros"** (pestañas).
  - Empleado: "Mis asignaciones fijas" + "Mis liberaciones" → **"Mis plazas"** (pestañas).
- **Vistas accionables.** Asignar/liberar **inline** desde la rejilla de Ocupación (reutiliza `PUT /fixed-assignments/employee/{id}`, ya existente). Toggle **Plazas/Puestos** en la vista de disponibilidad (`GET /availability` ya acepta `resourceType`).
- **Asignación puntual del admin (capacidad backend NUEVA).** Endpoint que crea una solicitud/asignación naciendo **`APPROVED`** para `{employeeId, requestedDate, resourceType, resourceId?}`, con validación de disponibilidad (409), auto-asignación por categoría/planta si se omite el recurso, y auditoría con el admin como actor. Hoy `POST /requests` es EMPLOYEE-only.
- **Generalización PARKING/DESK completada en las vistas.** Vista semanal de **puestos** (hoy solo plazas) y **"Mi Semana" multi-recurso** (hoy solo muestra `parkingSpaceLabel`).
- **Rol AGENCIA ampliado y documentado.** Acceso al pivote por-fecha de "Liberar", vista de ocupación de solo lectura e historial de sus propias liberaciones. Se documenta en el README (hoy no figura).
- **Correcciones de comportamiento** (a verificar en implementación): liberación de puesto fijo que omite `resourceType` (defaultea a PARKING) y muestra id interno en vez de "Puesto N"; selector de puesto ignorado en silencio en modo MANUAL; solicitud desde el plano sin confirmación.

## Capabilities

### New Capabilities
- `admin-punctual-assignment`: el ADMIN (y, si negocio lo aprueba, AGENCIA) asigna un recurso reservable a un empleado para una **fecha concreta**, creando una asignación que nace `APPROVED`, con validación de disponibilidad, auto-asignación opcional y traza de auditoría.

### Modified Capabilities
- `app-shell`: navegación reagrupada por tarea (ADMIN 9 ítems, EMPLOYEE 4, AGENCIA ampliado); "Mi Semana" como índice del empleado; fusiones de secciones en pestañas (Recursos, Registros, Mis plazas).
- `availability-calendar`: la disponibilidad admite conmutador `resourceType` (plaza/puesto); la rejilla de ocupación pasa a ser **accionable** (asignar/liberar inline); se añade la vista semanal de puestos.
- `weekly-assignment`: se integra en "Ocupación"; las celdas dejan de ser solo lectura y ofrecen asignación/liberación en contexto.
- `employee-portal`: "Mi Semana" como índice y **multi-recurso** (plaza + puesto por día); fusión de "Mis asignaciones fijas" + "Mis liberaciones"; la liberación de recurso fijo envía `resourceType` y muestra la etiqueta real del recurso.
- `releases`: las dos pantallas de liberación se unifican con pivote por-empleado / por-fecha; AGENCIA gana el pivote por-fecha, una vista de ocupación de solo lectura y el historial de sus liberaciones.
- `requests`: la solicitud desde el plano exige confirmación; en modo MANUAL el selector de puesto se oculta o se marca como "preferencia" (hoy se ignora en silencio).

## Impact

- **Backend:** nuevo endpoint de asignación puntual (controlador de requests o uno dedicado) reutilizando el servicio de disponibilidad y auto-asignación; posible extensión de `AdminWeeklyCalendarResponse` y `MyWeekDay` a `resourceType`/multi-recurso; ampliación de autorización de AGENCIA en liberaciones/ocupación. Actualiza `docs/openapi.yaml`.
- **Frontend:** reescritura de la navegación (`AppRoutes.tsx`, `AdminLayout.tsx`, `EmployeeLayout.tsx`, `AgencyLayout.tsx`, `paths.ts`); Ocupación accionable (fusión de `AdminCalendarPage` + `AvailabilityPage`); "Liberar" unificado (`AdministrativeReleasesPage` + `ReleaseByDatePage`); pestañas Recursos/Registros/Mis plazas; "Mi Semana" multi-recurso; correcciones en `MyFixedAssignmentsPage`, `CreateRequestModal`, `FloorPlanPage`.
- **Datos:** sin cambios de esquema previstos (la asignación puntual reutiliza la entidad `Request`). Puestos-para-visitantes queda **fuera de alcance**.
- **Docs:** `README.md` (rol AGENCIA, matriz de permisos), `docs/openapi.yaml`, `docs/ux-flows.md`, `docs/ui-screens.md`.
- **Fuera de alcance (candidatos a changes futuros):** rediseño visual/estético, sistema de diseño y animaciones (fase UI posterior); puestos para visitantes; modo de aprobación por tipo de recurso; solicitudes por rango/recurrentes; lista de espera; dashboard de inicio.
