## Context

El flujo de solicitudes es hexagonal. El dominio `Request` encapsula la máquina de estados; hoy sus transiciones son:

- `PENDING → APPROVED` (`approve`, ADMIN o auto-aprobación en modo `AUTOMATIC`).
- `PENDING → REJECTED` y `APPROVED → REJECTED` (`reject`, ADMIN; el segundo caso lo añadió el change `request-auto-assignment` para permitir el rechazo posterior que libera el recurso).
- `PENDING → CANCELLED` (`cancel`, empleado dueño). La invariante "solo desde `PENDING`" la comprueba el caso de uso `RequestService.cancel()` vía `request.isPending()` (`requirePending`).

`REJECTED` y `CANCELLED` son **terminales**. La disponibilidad de un recurso se recalcula sobre las filas `status = 'APPROVED'`: el índice único filtrado `UX_requests_space_date_approved` (SQL Server, `(parking_space_id/desk, requested_date) WHERE status='APPROVED'`) es la red dura de concurrencia. Cuando una fila deja de ser `APPROVED`, el recurso reaparece automáticamente en disponibilidad sin borrado físico.

**Problema**: `RequestService.cancel()` exige `PENDING`, así que una vez aprobada la solicitud el empleado no puede devolver el recurso. La regla de negocio es "siempre se puede cancelar o liberar en una fecha futura".

**Stakeholders**: empleado (cancela/libera su propia plaza/puesto aprobado de una fecha futura), ADMIN (conserva su rechazo posterior; no se ve afectado), disponibilidad (recupera el recurso liberado).

## Goals / Non-Goals

**Goals:**
- Permitir al empleado dueño cancelar su solicitud `APPROVED` cuando la `requested_date` es futura, liberando el recurso.
- Ampliar la máquina de estados del dominio con `APPROVED → CANCELLED`, preservando `PENDING → CANCELLED` y la terminalidad de `REJECTED`/`CANCELLED`.
- Registrar en auditoría la cancelación de una `APPROVED` como liberación por el empleado.
- Reflejar el nuevo caso en la UI ("Mis solicitudes") y en la doc OpenAPI, sin romper la cancelación en `PENDING`.

**Non-Goals:**
- No se toca la liberación de **asignaciones FIJAS** (capability `releases`): es un mecanismo distinto y ya existente.
- No se permite al empleado cancelar solicitudes de **otros** empleados (la verificación de pertenencia/BOLA se mantiene).
- No se permite cancelar una `APPROVED` de fecha **pasada** (no se libera un recurso de una fecha ya transcurrida).
- No se introducen columnas, tablas ni índices nuevos; no se cambia la ventana de creación (hoy..hoy+14) ni la unicidad `PENDING`.
- No se modifica el motor de disponibilidad (`AvailabilityService`): se reutiliza tal cual.

## Decisions

### D1 — Definición de "fecha futura": `requested_date >= hoy` (hoy inclusive)

Se adopta **`requested_date >= hoy` (hoy incluido)** como criterio de "futura" a efectos de cancelación, siendo `hoy = LocalDate.ofInstant(clock.now(), UTC)` (mismo reloj inyectable y misma zona que la ventana de creación).

- **Rationale**: la ventana de creación ya admite solicitudes para **hoy** (`hoy..hoy+14`); permitir cancelar/liberar una plaza de **hoy** es coherente (el día aún no ha terminado y el recurso puede reasignarse a otro empleado para hoy). Excluir hoy dejaría un día "muerto" en el que ni se puede pedir de nuevo ni liberar lo aprobado.
- **Consecuencia**: una `APPROVED` con `requested_date < hoy` (estrictamente pasada) **no** es cancelable → 409.
- **Alternativa descartada** — `requested_date > hoy` (estrictamente mayor que hoy): más restrictiva; impediría liberar una plaza de hoy que el empleado ya sabe que no usará, sin beneficio claro. Se descarta por coherencia con la ventana de creación.
- La comprobación de fecha **solo** aplica a la transición `APPROVED → CANCELLED`. La cancelación desde `PENDING` **no** exige fecha futura (una `PENDING` no ocupa recurso; su cancelación es siempre válida, comportamiento actual intacto).

### D2 — Ampliación de la máquina de estados: `APPROVED → CANCELLED`

El dominio `Request` expone la regla de qué solicitudes admiten cancelación por el empleado, análogamente a `canBeRejected()`:

```
canBeCancelledBy(today):
    PENDING                                   -> true   (cualquier fecha)
    APPROVED  && requested_date >= today      -> true   (futura, libera recurso)
    APPROVED  && requested_date <  today      -> false  (pasada)
    REJECTED | CANCELLED                      -> false  (terminal)

cancel():
    status = CANCELLED   // la guarda la impone el caso de uso vía canBeCancelledBy(...)
```

- El método `cancel()` del dominio realiza la transición; la **guarda** (estado + fecha) la evalúa el caso de uso antes de invocarlo, siguiendo el patrón ya usado con `requirePending`/`requireRejectable`. Se sustituye `requirePending` por `requireCancellable(request, today)` en `RequestService.cancel()`.
- Al cancelar una `APPROVED`, el `resourceId` se **conserva** como traza del recurso liberado (igual que en `reject`): la disponibilidad se recalcula solo sobre filas `APPROVED`, no hace falta limpiarlo.
- `REJECTED`/`CANCELLED` siguen sin admitir transición: intentar cancelar una ya cancelada o rechazada responde 409.

### D3 — Liberación del recurso vía cambio de estado (sin borrado físico)

Cancelar una `APPROVED` **libera** el recurso por el mismo mecanismo que el rechazo posterior del ADMIN (`request-auto-assignment §D5`):

- Al pasar de `APPROVED` a `CANCELLED`, la fila deja de cumplir el filtro `WHERE status='APPROVED'` del índice único filtrado `UX_requests_space_date_approved`; la plaza/puesto vuelve a estar disponible para `requested_date` de forma inmediata y automática.
- No se requiere borrado físico ni tocar `AvailabilityService`: la disponibilidad ya se calcula sobre `status = 'APPROVED'`.
- **Relación con "liberar"**: para recursos obtenidos por **solicitud** (plaza/puesto puntual), "liberar" por parte del empleado **es** cancelar la solicitud aprobada — no hay una entidad de "release" para solicitudes puntuales. La entidad `Release` de la capability `releases` libera **asignaciones FIJAS** (recurrentes), un caso distinto que no se toca aquí.

### D4 — Auditoría de la cancelación de una `APPROVED`

La cancelación de una solicitud `APPROVED` **libera capacidad** y por tanto debe quedar trazada, igual que el rechazo posterior:

- Se registra en auditoría la acción "cancelación/liberación por el empleado" con la solicitud, el recurso liberado, el empleado actor y la fecha.
- La cancelación desde `PENDING` mantiene su tratamiento actual (no libera recurso; auditoría según lo ya existente para `cancel`).
- No se introduce un evento de notificación nuevo: la cancelación la origina el propio empleado (no necesita avisarse a sí mismo). Si en el futuro se desea notificar al ADMIN, quedaría como mejora fuera de alcance.

### D5 — Endpoint y contrato (sin cambios de forma)

Se reutiliza el endpoint existente `POST /requests/{id}/cancel` (`EMPLOYEE`, verificación de pertenencia en el servicio). No cambia la firma ni el cuerpo; cambia la **semántica de estados aceptados**:

- 200: cancelada una solicitud `PENDING` (cualquier fecha) **o** una `APPROVED` con `requested_date >= hoy`.
- 409: la solicitud es `APPROVED` con `requested_date` pasada, o está en un estado terminal (`REJECTED`/`CANCELLED`).
- 403: la solicitud no pertenece al empleado de la sesión (BOLA), sin cambios.
- 404: la solicitud no existe, sin cambios.
- Se actualiza la documentación OpenAPI (`docs/openapi.yaml`) del endpoint para describir el nuevo caso `APPROVED` futura.

## Risks / Trade-offs

- **[`APPROVED → CANCELLED` amplía la máquina de estados]** → riesgo de habilitar cancelaciones indebidas. Mitigación: la transición se restringe al **dueño**, solo desde `APPROVED` con fecha **futura**; `REJECTED`/`CANCELLED` siguen terminales; la verificación de pertenencia (BOLA) permanece.
- **[Frontera de fecha "hoy"]** → depende del reloj/zona. Mitigación: se usa el mismo `ClockPort` y `ZoneOffset.UTC` que la ventana de creación, de modo que "hoy" es consistente en todo el agregado; tests parametrizados en el borde (ayer/hoy/mañana).
- **[Carrera cancelar vs. reasignar]** → un empleado cancela y libera mientras otro solicita esa plaza: no hay conflicto porque la cancelación solo elimina una fila `APPROVED`; el índice único filtrado sigue protegiendo cualquier nueva `APPROVED` sobre el recurso liberado.
- **[Confusión empleado con "release" de asignación fija]** → mismo verbo ("liberar") para dos mecanismos. Mitigación: documentado en `§D3`; la UI usa "Cancelar" para solicitudes y "Liberar" para asignaciones fijas (pantallas distintas).

## Migration Plan

1. **Sin migración de BD**: no hay columnas, tablas ni índices nuevos. La transición `APPROVED → CANCELLED` reutiliza el estado `CANCELLED` ya existente y el índice filtrado `UX_requests_space_date_approved` ya vigente.
2. **Backend** (TDD): dominio `Request` (`canBeCancelledBy` + `cancel` desde `APPROVED`) → `RequestService.cancel()` (guarda por estado+fecha, auditoría de liberación) → doc OpenAPI del endpoint.
3. **Frontend**: condición de visibilidad del botón Cancelar en "Mis solicitudes" (`PENDING` o `APPROVED` con `requested_date >= hoy`).
4. **Rollback**: revertir el código restaura la guarda `requirePending`; al no haber migración de esquema, no hay estado de BD que deshacer. Las solicitudes ya canceladas quedan `CANCELLED` (terminal), consistentes en cualquier versión.

## Open Questions

- ¿La cancelación de una `APPROVED` por el empleado debe **notificar al ADMIN** (que la había aprobado) de que el recurso quedó libre? Propuesta: no en este change (fuera de alcance); registrar solo en auditoría.
- ¿"Hoy inclusive" (`>= hoy`) es la política definitiva del negocio, o se prefiere `> hoy`? Decisión adoptada: `>= hoy` (D1), revisable si el propietario indica lo contrario.
