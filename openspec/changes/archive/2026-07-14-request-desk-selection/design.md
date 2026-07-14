## Context

El flujo actual de solicitud de recurso tiene tres piezas relevantes:

- **`CreateRequestModal`** (frontend): modal unificado con checkboxes PLAZA/PUESTO. Envía un `POST /requests` por recurso seleccionado con `{ requestedDate, resourceType }`, **sin `resourceId`**. El tipo `RequestCreateRequest` del frontend (`types/request.ts`) no tiene `resourceId`, aunque el contrato OpenAPI ya lo define (`RequestCreateRequest.resourceId`, "puesto elegido para modo automático + DESK").
- **Plano** (`FloorPlanPage` + `FloorPlanSurface` + `FloorPlanMarker` + `FloorPlanMobileList`): renderiza la imagen real de la planta (`assets/floor-plan.png`) y un marcador por puesto en su `(coordX, coordY)`. Un marcador `FREE` es un botón que dispara `onRequest(desk)` → `POST /floor-plan/desks/{deskId}/request`. El color del marcador viene de `markerStateClass(state)` para estados `FREE|MINE|ASSIGNED|REQUESTED|RELEASED`; **no existe un estado visual de "seleccionado"**, de ahí que el usuario no perciba que ha elegido un puesto.
- **Backend**: `RequestService.create()` **ya** ramifica por `SystemSettingsService.approvalMode()` y, en `AUTOMATIC` + DESK con `resourceId`, auto-aprueba el puesto elegido (`createAutomatic` → `chosenDesk` → `autoApprove`). En cambio, `FloorPlanCommandService.requestDesk()` **siempre** persiste `PENDING` (`RequestEntity.createForResource(...)` + `RequestCreatedEvent`); no consulta el modo global. Este es el follow-up #98.

Los índices únicos filtrados de la BD (`UX_requests_desk_date_pending`, `UX_requests_..._approved`) y la ventana hoy..+14 se mantienen sin cambios: este change no toca esquema ni migraciones.

**Stakeholders**: empleado (elige el puesto y recibe confirmación inmediata), ADMIN (en modo automático deja de resolver puestos elegidos; en manual sigue resolviendo), propietario (coherencia entre las dos vías de solicitud de puesto).

## Goals / Non-Goals

**Goals:**
- Permitir al empleado **elegir un puesto concreto** en el modal de solicitud a través del plano, y enviar su `resourceId`.
- Dar **feedback visual** inequívoco de la selección en el plano (color distinto + mensaje de confirmación) y mostrar el **número** del puesto (no el id) en el modal.
- Que la vía del plano (`FloorPlanCommandService.requestDesk`) **respete el modo de aprobación global** y auto-apruebe en `AUTOMATIC`, alineándose con `RequestService.create()`.

**Non-Goals:**
- No se toca la auto-asignación de **plaza** por categoría/planta (sigue igual; la plaza no se elige en el plano).
- No se cambia la ventana de solicitud, la unicidad `PENDING`, ni los índices/migraciones.
- No se introduce selección múltiple de puestos: el empleado elige **un** puesto por solicitud de puesto.
- No se rediseña el editor de posiciones del plano (arrastre admin).

## Decisions

### D1 — Reutilizar el plano existente como selector modal (no crear un plano nuevo)

El plano de selección **reutiliza** `FloorPlanSurface`/`FloorPlanMarker` (misma imagen, mismas coordenadas, mismo `useFloorPlanQuery(date)`) dentro de un `Modal`, en lugar de duplicar un segundo plano. Se introduce un componente contenedor ligero (p. ej. `DeskPickerModal`) que:

- Reutiliza `FloorPlanSurface` en **modo selección** (sin edición/arrastre, sin lista móvil de solicitud directa): al pinchar un puesto `FREE` no dispara `POST` sino un callback `onPick(desk)`.
- Recibe la `date` ya elegida en `CreateRequestModal` (el selector no navega fecha: usa la fecha de la solicitud para colorear disponibilidad).
- Al elegir, propaga `{ deskId, deskNumber }` al modal y se cierra.

**Alternativa descartada** — un plano nuevo específico del selector: duplicaría el renderizado de marcadores/coordenadas y divergería de la vista principal. Reutilizar garantiza paridad visual y una sola fuente de verdad de posiciones.

**Ajuste en `FloorPlanSurface`/`FloorPlanMarker`**: se parametriza el comportamiento del click. Hoy `FloorPlanMarker` decide `canRequest = !editMode && state === 'FREE'`. Se añade un modo "selección" en el que el click sobre un `FREE` invoca `onRequest`/`onPick` pero, además, marca ese puesto como **seleccionado** (estado visual `SELECTED`). El marcador seleccionado se pasa por prop (`selectedDeskId`) para pintarlo distinto.

### D2 — Estado visual `SELECTED`: color + `aria` + mensaje

- **Color**: se añade una clase de marcador `floor-marker-selected` (token del design-system, p. ej. acento/azul de selección, sin hex suelto) que **prevalece** sobre el color semántico del estado mientras el puesto está elegido. `markerStateClass`/`utils/floorPlan` gana el mapeo del estado `SELECTED` (o el componente aplica la clase cuando `desk.deskId === selectedDeskId`).
- **Accesibilidad**: el marcador seleccionado expone `aria-pressed="true"` (o `aria-selected`) y su `aria-label` incluye el sufijo "seleccionado" (i18n), de modo que el estado es perceptible sin depender del color (WCAG 1.4.1).
- **Mensaje de confirmación**: el selector muestra un banner/`role="status"` con "Puesto {número} seleccionado" (i18n ES/EN) al elegir, cubriendo el bug de que "no se aprecia la selección".

Se mantiene la paleta pastel semántica existente para los demás estados; `SELECTED` es un realce temporal de UI (no un estado de dominio del puesto: el backend sigue devolviendo `FREE`).

### D3 — Cierre del selector y número del puesto en el modal

Al confirmar la selección (pinchar el puesto en modo selector), el `DeskPickerModal` invoca `onPick({ deskId, deskNumber })` y **se cierra** (`onClose`). `CreateRequestModal` guarda `selectedDesk = { id, number }` en estado y muestra el **número** (`desk.deskNumber`, mapeado de `Desk.number`) junto al checkbox de PUESTO, con opción de "cambiar"/"quitar". El `id` se conserva solo para el envío (`resourceId`), **nunca** se muestra al usuario.

### D4 — Envío del `resourceId` y ramificación por modo (sin cambio de contrato)

- `types/request.ts`: `RequestCreateRequest` gana `resourceId?: number` (alineado con OpenAPI).
- En `handleSubmit`, la solicitud de PUESTO incluye `resourceId: selectedDesk.id` cuando hay puesto elegido. La de PLAZA no lleva `resourceId` (la plaza la resuelve el backend). 
- El backend ya hace lo correcto: en `AUTOMATIC` + DESK con `resourceId` auto-aprueba; en `MANUAL` ignora `resourceId` y nace `PENDING`. **No** se exige elegir puesto en modo manual (retrocompatible): si el empleado no elige puesto, se envía sin `resourceId` como hoy.

### D5 — `FloorPlanCommandService.requestDesk` respeta el modo global (backend, supersede #98)

Se inyecta `SystemSettingsService` en `FloorPlanCommandService` y `requestDesk` ramifica al final, tras validar ventana + disponibilidad + no-duplicado (que se mantienen idénticas en ambos modos):

```
requestDesk(login, deskId, date):
    now = clock.now()
    requireWithinWindow(date, now)
    employeeId = resolve(login)
    desk = deskRepository.findById(deskId) or 404
    requireDeskRequestable(desk, date)        # FREE, activo, sin pending del recurso
    requireNoPendingDuplicate(employeeId, DESK, date)
    if systemSettings.approvalMode() == AUTOMATIC:
        entity = RequestEntity.createForResource(employeeId, DESK, desk.id, date, now)
        entity.approve(desk.id, null, "auto", now)     # nace APPROVED (actor SYSTEM)
        saved = requestRepository.saveAndFlush(entity)
        publish RequestApprovedEvent(...)
        return DeskRequestResponse(saved.id, desk.id, MINE)   # sigue MINE (es del empleado)
    else:  # MANUAL — comportamiento actual intacto
        saved = requestRepository.saveAndFlush(
                    RequestEntity.createForResource(employeeId, DESK, desk.id, date, now))
        publish RequestCreatedEvent(...)
        return DeskRequestResponse(saved.id, desk.id, MINE)
```

- **Coherencia** con `RequestService.autoApprove`: mismo actor (`resolvedById = null`), misma nota `"auto"`, mismo `RequestApprovedEvent`. Se extrae la constante `AUTO_APPROVAL_NOTE = "auto"` (evitar literal duplicado, S1192) o se reutiliza la existente si es accesible.
- La **red de concurrencia** no cambia: el índice único filtrado `APPROVED` protege dos auto-aprobaciones del mismo puesto/fecha → la segunda recibe 409. La comprobación previa (`requireDeskRequestable`) sigue siendo la primera capa.
- `DeskRequestResponse` puede ganar (opcional) el `status` resultante (`APPROVED`/`PENDING`) para que el frontend del plano ajuste el mensaje; si se añade, es aditivo y no rompe el contrato.
- **Complejidad cognitiva** < 15: la ramificación se extrae a métodos privados `createPendingDesk(...)` / `autoApproveDesk(...)` (S3776).

### D6 — Selector vs solicitud directa desde el plano (dos usos, un componente)

El plano tiene dos usos que conviven:
- **Solicitud directa** (`FloorPlanPage`): pinchar `FREE` crea la solicitud (`POST /floor-plan/...`). Sin cambios de UX salvo D5 (backend) y, opcionalmente, el mensaje de éxito según `status`.
- **Selector** (desde `CreateRequestModal`): pinchar `FREE` **no** crea la solicitud; solo devuelve el puesto elegido al modal (que hará el `POST /requests` al enviar). Esto evita crear dos solicitudes.

La distinción se hace por prop del contenedor (`mode: 'request' | 'select'`), no duplicando el plano.

## Risks / Trade-offs

- **[Doble vía de creación de puesto en `AUTOMATIC`]** El puesto puede solicitarse por `POST /requests` (con `resourceId`) o por `POST /floor-plan/.../request`. Ambas deben auto-aprobar de forma coherente; el riesgo es divergencia de comportamiento. Mitigación: mismo actor/nota/evento y tests que verifican paridad (`APPROVED` en ambos en modo automático).
- **[Estado `SELECTED` sólo de UI]** Un observador podría confundir "seleccionado" con un estado de dominio. Mitigación: es un realce local del selector, se limpia al cerrar; el backend nunca devuelve `SELECTED`. Documentado en D2.
- **[Accesibilidad del color de selección]** Depender solo del color excluiría a usuarios con baja visión. Mitigación: `aria-pressed`/`aria-selected` + sufijo textual + mensaje `role="status"` (D2).
- **[Retrocompatibilidad del modal]** Añadir el botón no debe romper el flujo manual sin puesto elegido. Mitigación: el `resourceId` es opcional; sin selección el envío es idéntico al actual.
- **[Concurrencia en auto-aprobación desde el plano]** Dos empleados auto-aprobando el mismo puesto/fecha. Mitigación: el índice único filtrado `APPROVED` devuelve 409 a la segunda; no se relaja ningún índice (D5).
