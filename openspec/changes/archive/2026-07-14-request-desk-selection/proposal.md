## Why

Hoy el empleado que quiere un **puesto** concreto no puede elegirlo desde el modal de solicitud (`CreateRequestModal`): el modal solo ofrece checkboxes plaza/puesto y envía la solicitud **sin recurso**, dejando la elección del puesto al ADMIN (modo manual) o sin puesto asignable en el flujo unificado. El backend ya soporta auto-aprobar un puesto elegido (`POST /requests` con `resourceId` en modo `AUTOMATIC`), pero el frontend nunca envía ese `resourceId`. Además, la vía alternativa de solicitud desde el **plano** (`POST /floor-plan/desks/{deskId}/request`) **siempre** crea la solicitud `PENDING`, ignorando el modo global de aprobación: en modo `AUTOMATIC` un puesto pinchado en el plano debería auto-aprobarse igual que uno elegido desde el modal. Por último, el plano no da **feedback visual** claro de que un puesto se ha seleccionado (bug de UX del marcador).

## What Changes

- **Elegir puesto concreto en la solicitud (frontend)**: en `CreateRequestModal`, cuando el recurso seleccionado incluye **PUESTO**, se añade un botón **"Seleccionar puesto"** que abre el **plano de la planta** (imagen real) como **selector modal**. El empleado pincha un puesto libre; al confirmar, el modal muestra el **número del puesto** (`Desk.number`), no su id/referencia.
- **Feedback visual de selección en el plano**: el marcador del puesto seleccionado se pinta en un **color distinto** (nuevo estado visual `SELECTED`) y el plano muestra un **mensaje de confirmación**. Corrige el bug actual de que la selección no se aprecia.
- **Cierre y número**: al seleccionar el puesto se **cierra la ventana del plano** y el modal de solicitud refleja el **número del puesto** elegido.
- **Envío con `resourceId` (frontend)**: la solicitud de puesto incluye el `resourceId` elegido en `POST /requests`. El tipo `RequestCreateRequest` del frontend gana el campo `resourceId` (el contrato OpenAPI ya lo define). En modo `AUTOMATIC` el puesto se auto-aprueba (ya soportado en backend); en `MANUAL` el `resourceId` se ignora (nace `PENDING`).
- **Coherencia del plano con el modo automático (backend)**: `FloorPlanCommandService.requestDesk` pasa a **consultar `SystemSettingsService.approvalMode()`** y, en modo `AUTOMATIC`, **auto-aprueba** el puesto pinchado (nace `APPROVED` con el puesto asignado) en lugar de crear siempre `PENDING`. En `MANUAL` el comportamiento actual queda intacto. Esto **SUPERSEDE** el follow-up issue #98.

## Capabilities

### New Capabilities
<!-- Ninguna: este change modifica capabilities existentes. -->

### Modified Capabilities
- `requests`: la solicitud unificada del empleado permite **elegir un puesto concreto** desde el plano y enviar su `resourceId`; el modal muestra el **número** del puesto elegido. (Modifica el flujo de UI de creación de solicitud; el contrato `POST /requests` ya admite `resourceId`.)
- `floor-plan`: (a) el plano puede actuar como **selector de puesto** devolviendo el puesto elegido al modal, con **feedback visual de selección** (estado `SELECTED` + mensaje de confirmación) y mostrando el **número** del puesto; (b) `POST /floor-plan/desks/{deskId}/request` **respeta el modo de aprobación global** y auto-aprueba en `AUTOMATIC`.

## Impact

- Frontend: `components/CreateRequestModal.tsx` (botón "Seleccionar puesto" + estado del puesto elegido + número), nuevo componente/uso del plano como selector (reutiliza `FloorPlanSurface`/`FloorPlanMarker`), `components/FloorPlanMarker.tsx` + `utils/floorPlan.ts` (estado visual `SELECTED`), `types/request.ts` (campo `resourceId`), `hooks/useRequests` (envío de `resourceId`), i18n ES/EN, estilos del marcador seleccionado.
- Backend: `floorplan/application/FloorPlanCommandService.java` (inyectar `SystemSettingsService`, ramificar `requestDesk` por `approvalMode`, auto-aprobar en `AUTOMATIC` reutilizando `RequestApprovedEvent`), sin cambios de esquema ni migraciones.
- Contrato: `docs/openapi.yaml` ya define `RequestCreateRequest.resourceId` y `POST /floor-plan/desks/{deskId}/request`; se documenta que este último puede devolver estado `APPROVED` en modo automático.
- Dependencia: reutiliza `system-settings` (`approvalMode`) y `request-auto-assignment` (auto-aprobación de puesto elegido) ya aplicados.
