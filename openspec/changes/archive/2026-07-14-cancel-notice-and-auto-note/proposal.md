## Why

Cuando un empleado cancela una solicitud ya **APROBADA**, el recurso reservado (plaza o puesto) se libera y vuelve a estar disponible, pero hoy ningún administrador se entera: la cancelación no publica ningún evento de notificación. Además, el correo de aprobación muestra "Nota del administrador: auto" en las auto-aprobaciones, filtrando una constante interna (`"auto"`) que resulta confusa para el empleado. Ambos son defectos de comunicación en la capability `notifications`.

## What Changes

- **Aviso al admin en cancelación de solicitud APROBADA**: cuando el empleado cancela una `Request` cuyo estado previo era `APPROVED` (se libera recurso), el sistema publica un nuevo `RequestCancelledEvent` que, tras `AFTER_COMMIT`, envía un correo (`request-cancelled.html`) a **todos** los administradores activos indicando quién canceló, la fecha de la solicitud y el recurso liberado con su número real (plaza nº X en planta Y / puesto nº Z).
- La cancelación de una `Request` en estado `PENDING` **NO** genera ningún aviso (no se libera recurso). Esto acota la exclusión previa "cancelación de la propia solicitud no genera email" al caso PENDING.
- **Ocultar la nota "auto"**: el correo de aprobación deja de mostrar la línea "Nota del administrador" cuando la nota es igual a la constante de auto-aprobación (`Request.AUTO_APPROVAL_NOTE = "auto"`). Las notas reales escritas por un admin siguen mostrándose.

## Capabilities

### New Capabilities

<!-- Ninguna. No se introducen capabilities nuevas. -->

### Modified Capabilities

- `notifications`: nuevo requisito de aviso al admin ante la cancelación de una solicitud APROBADA (recurso liberado); y modificación del correo de aprobación para ocultar la nota "auto" en auto-aprobaciones. La exclusión existente de "cancelación de la propia solicitud" se acota al caso PENDING.

## Impact

- **Backend** (`backend/src/main/java/com/aleatica/parking/`):
  - `request/application/RequestService.cancel` — publica `RequestCancelledEvent` solo cuando el estado previo era `APPROVED`.
  - `notification/event/RequestCancelledEvent` — nuevo record `RequestCancelledEvent(RequestResponse request)`.
  - `notification/application/EmailNotificationListener` — enruta el nuevo evento a `NotificationDispatcher.requestCancelled`.
  - `notification/application/NotificationDispatcher.requestCancelled` — emite una orden por cada admin activo (patrón `requestCreated`).
  - `notification/NotificationEventType` — nuevo valor `REQUEST_CANCELLED`.
  - `notification/application/NotificationRenderer` + `EmailContentRenderer.renderRequestCancelled` — resuelve nombre del solicitante (`request.employeeId()`) y número del recurso liberado.
  - `notification/application/EmailContentRenderer.renderRequestApproved` — trata la nota "auto" como ausente.
- **Plantillas** (`backend/src/main/resources/templates/email/`): nueva `request-cancelled.html`; ajuste en `request-approved.html` (condición de la nota).
- **Sin migración de BD.** Sin cambios de API pública. Fallo SMTP nunca revierte la cancelación (best-effort/outbox, `AFTER_COMMIT`).
