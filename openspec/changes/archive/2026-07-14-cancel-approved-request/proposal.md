## Why

Hoy, una vez que una solicitud de plaza o puesto pasa a `APPROVED`, el empleado **no puede cancelarla**: `RequestService.cancel()` exige que la solicitud esté en `PENDING` y responde 409 en cualquier otro estado. Esto choca con la regla de negocio "siempre se puede cancelar o liberar en una fecha futura": si un empleado deja de necesitar la plaza/puesto que ya le fue aprobado para una fecha aún no llegada, no tiene forma de devolver el recurso y este permanece ocupado (contando como `APPROVED` en disponibilidad) hasta que expira o interviene un ADMIN con un rechazo. Se desperdicia capacidad que otros empleados podrían solicitar.

## What Changes

- El empleado PUEDE cancelar su **propia solicitud `APPROVED`** siempre que la `requested_date` sea **futura** (hoy o posterior; ver `design.md §D1`). La cancelación transiciona la solicitud a `CANCELLED` y **libera el recurso** (plaza/puesto), que reaparece en disponibilidad para esa fecha.
- La máquina de estados del dominio `Request` gana la transición **`APPROVED → CANCELLED`** (cancelación por el empleado sobre una aprobada futura). Se conserva `PENDING → CANCELLED` (sin restricción de fecha). Los estados `REJECTED`/`CANCELLED` siguen siendo **terminales**.
- Cancelar una solicitud `APPROVED` con `requested_date` **pasada** NO se permite: responde 409 (no se puede liberar un recurso de una fecha ya transcurrida).
- La cancelación de una solicitud `APPROVED` se **registra en auditoría** como liberación del recurso por el empleado, análogamente al rechazo posterior del ADMIN.
- Frontend "Mis solicitudes": el botón **Cancelar** deja de mostrarse solo en `PENDING`; también aparece en solicitudes `APPROVED` cuya `requested_date` es futura. Sigue oculto para solicitudes `APPROVED` pasadas, `REJECTED` y `CANCELLED`.
- Delimitación con "liberar": para recursos obtenidos vía **solicitud** (no asignación fija) el mecanismo de liberación por el empleado ES cancelar la solicitud aprobada; la liberación de **asignaciones FIJAS** ya existe aparte (capability `releases`) y queda fuera de alcance.

## Capabilities

### New Capabilities
<!-- Ninguna. -->

### Modified Capabilities
- `requests`: la cancelación por el dueño deja de restringirse a `PENDING`; se admite también sobre una solicitud `APPROVED` con `requested_date` futura, lo que libera el recurso y lo devuelve a disponibilidad. La máquina de estados incorpora `APPROVED → CANCELLED`.

## Impact

- **Backend** (`backend/src/main/java/com/aleatica/parking/request/**`):
  - `domain/Request.java`: método `cancel()` / máquina de estados → añadir la transición `APPROVED → CANCELLED` y la guarda de fecha futura (o exponer un predicado `canBeCancelledBy(date, today)`).
  - `application/RequestService.java`: `cancel(id, login)` deja de exigir `PENDING`; acepta `PENDING` (cualquier fecha) o `APPROVED` con `requested_date` futura; registra auditoría de liberación en el caso `APPROVED`.
  - `RequestController.java`: la documentación OpenAPI del endpoint `POST /requests/{id}/cancel` refleja el nuevo caso (200 al cancelar una `APPROVED` futura; 409 si es `APPROVED` pasada o estado terminal).
- **Disponibilidad**: al pasar de `APPROVED` a `CANCELLED`, la fila deja de cumplir el filtro `WHERE status='APPROVED'` del índice único filtrado (`UX_requests_space_date_approved`), por lo que la plaza/puesto reaparece automáticamente en disponibilidad sin borrado físico (mismo mecanismo que el rechazo posterior del change `request-auto-assignment`).
- **Frontend** (`frontend/src/pages/MyRequestsPage.tsx`): condición de visibilidad del botón Cancelar (`PENDING` o `APPROVED` con `requested_date >= hoy`).
- **API spec** (`docs/openapi.yaml`): descripción y respuestas del endpoint de cancelación.
- **Sin cambios** en: esquema de BD / migraciones (no hay columnas nuevas ni índices nuevos), capability `releases` (asignaciones fijas), ventana de creación (hoy..hoy+14) ni unicidad `PENDING`.
