## Context

La capability `notifications` está construida con arquitectura hexagonal: los casos de uso publican eventos de dominio (`request/.../event` — `RequestCreatedEvent`, `RequestApprovedEvent`, `RequestRejectedEvent`, `FixedAssignmentRevokedEvent`), un `EmailNotificationListener` (`@TransactionalEventListener(phase = AFTER_COMMIT)`) los convierte en llamadas al `NotificationDispatcher`, que produce una o varias `NotificationCommand(eventType, recipientEmployeeId, request)` y las entrega a `NotificationDeliveryService.dispatch`. Éste renderiza con `NotificationRenderer.render` (resuelve destinatario por `EmployeeRepository.findById` y el número del recurso vía `ParkingSpaceRepository`/`DeskRepository`), delega el HTML a `EmailContentRenderer` (plantillas Thymeleaf en `templates/email/`) y envía con `SmtpEmailSender`, con outbox de reintento resiliente.

Estado actual relevante:

- `RequestService.cancel(id, login)` permite al empleado cancelar su solicitud `PENDING` (cualquier fecha) o `APPROVED` futura. Al cancelar una `APPROVED` se libera el recurso (índice único filtrado). Hoy **no publica ningún evento** de notificación (decisión D4 del change `cancel-approved-request`).
- `NotificationDispatcher.requestCreated` ya resuelve destinatarios = todos los `Employee` con `role = ADMIN` y `active = true` (`findByRoleAndActiveTrue(Role.ADMIN)`); patrón reutilizable para el aviso a admins.
- `EmailContentRenderer.renderRequestApproved` fija `approvalNote = request.approvalNote()` en el contexto Thymeleaf; la plantilla `request-approved.html` muestra la línea con `th:if="${approvalNote != null and !approvalNote.isBlank()}"`. En auto-aprobación (`RequestService`, línea `request.approve(resourceId, null, Request.AUTO_APPROVAL_NOTE, now)`) la nota es la constante `Request.AUTO_APPROVAL_NOTE = "auto"`, que se muestra como "Nota del administrador: auto".
- El nombre del solicitante y el número del recurso ya se resuelven en `NotificationRenderer` (para el correo de aprobación): el solicitante por `request.employeeId()`, el recurso por `resolveResource(request)` → `ResolvedResource(type, number, floor)`.

## Goals / Non-Goals

**Goals:**

- Notificar a los administradores activos cuando un empleado cancela una solicitud **APROBADA**, con el nombre de quién canceló, la fecha de la solicitud y el recurso liberado por su **número real**.
- No notificar cuando la solicitud cancelada estaba en `PENDING` (no se libera recurso).
- Ocultar la línea de nota del administrador en el correo de aprobación cuando la nota es la de auto-aprobación (`"auto"`), conservando las notas reales.
- Reutilizar la infraestructura existente (evento → listener → dispatcher → renderer → sender) sin introducir patrones nuevos.

**Non-Goals:**

- No se cambia la lógica funcional de `cancel` (qué solicitudes pueden cancelarse ni cómo se libera el recurso).
- No se toca el correo al propio empleado que cancela (no se le envía nada).
- No hay migración de BD ni cambios de esquema de `email_outbox`.
- No se modifica el flujo de reintento SMTP.

## Decisions

### D1 — Nuevo evento `RequestCancelledEvent` en lugar de reutilizar uno existente

Se crea `notification/event/RequestCancelledEvent(RequestResponse request)`, coherente con el resto de eventos de la capability (cada uno un `record(RequestResponse request)`).

- **Por qué**: la semántica (recurso liberado, destinatarios = admins) no coincide con ningún evento actual; reutilizar `RequestCreatedEvent` o `RequestApprovedEvent` acoplaría plantillas y destinatarios distintos bajo un mismo tipo. Un evento propio mantiene el mapeo 1:1 evento→plantilla del outbox.
- **Alternativa descartada**: pasar un flag en un evento existente — rompe el enum `NotificationEventType` como clave de re-render del outbox.

### D2 — Publicar el evento SOLO cuando el estado previo era `APPROVED`

En `RequestService.cancel`, tras confirmar la transición a `CANCELLED`, se publica `RequestCancelledEvent` únicamente si el estado **anterior** a la cancelación era `APPROVED` (se liberó recurso). La cancelación de `PENDING` no publica nada.

- **Por qué**: solo la cancelación de una APROBADA libera un recurso que interesa a los admins; una PENDING no reservaba nada.
- **Implementación**: capturar el estado previo antes de mutar la entidad y condicionar la publicación. El evento se publica dentro de la transacción; el `@TransactionalEventListener(AFTER_COMMIT)` garantiza que solo se envía si el commit tiene éxito.

### D3 — Destinatarios = todos los admins activos (patrón `requestCreated`)

`NotificationDispatcher.requestCancelled(RequestResponse request)` itera `employeeRepository.findByRoleAndActiveTrue(Role.ADMIN)` y emite una `NotificationCommand(REQUEST_CANCELLED, admin.getId(), request)` por cada uno. Si no hay admins activos, no se emite ninguna orden (el flujo no falla), igual que `requestCreated`.

- **Por qué**: la liberación del recurso es información de gestión que compete a cualquier administrador; reutiliza el patrón ya validado y probado.

### D4 — Contenido del correo al admin y resolución de nombre/número

Nuevo `NotificationEventType.REQUEST_CANCELLED` + rama en `NotificationRenderer.renderFor` + `EmailContentRenderer.renderRequestCancelled(admin, request, resolved)` + plantilla `request-cancelled.html`. El correo informa:

- **Quién canceló**: nombre y apellidos del **solicitante**, resuelto por `request.employeeId()` (NO el admin destinatario). Como el destinatario de la orden es el admin, el renderer debe resolver el solicitante por separado vía `EmployeeRepository.findById(request.employeeId())` para obtener su nombre.
- **Fecha de la solicitud**: `request.requestedDate()`.
- **Recurso liberado con su número real**: se reutiliza `resolveResource(request)` → `ResolvedResource` (plaza nº X en planta Y / puesto nº Z), idéntico a la resolución del correo de aprobación. Degrada con elegancia si el recurso no se localiza (omite el número).

- **Por qué resolver el solicitante en el renderer y no en el dispatcher**: el patrón del proyecto es que el dispatcher solo lleva el id del destinatario en la orden y el nombre/email se resuelven al renderizar; el nombre del solicitante es contenido del cuerpo, así que se resuelve donde se resuelve el resto del contenido (renderer), manteniendo la orden mínima y el reintento re-renderizable.

### D5 — Ocultar la nota "auto" en el renderer (no en la plantilla)

`EmailContentRenderer.renderRequestApproved` deja de propagar la constante de auto-aprobación: si `request.approvalNote()` es igual a `Request.AUTO_APPROVAL_NOTE` (`"auto"`), fija `approvalNote = null` en el contexto (se trata como ausente). La plantilla `request-approved.html` conserva su `th:if="${approvalNote != null and !approvalNote.isBlank()}"` sin cambios.

- **Por qué en el renderer**: la plantilla no debe conocer la constante interna `AUTO_APPROVAL_NOTE`; centralizar la regla en Java evita literales mágicos en Thymeleaf (S1192) y hace la condición testeable unitariamente. La plantilla ya oculta la nota cuando es `null`, así que basta con nulificarla.
- **Alternativa descartada**: comparar contra el literal `"auto"` en la plantilla — duplica la constante y la hace frágil ante cambios.

## Risks / Trade-offs

- **Fallo SMTP al avisar al admin** → no revierte la cancelación: el aviso se publica en `AFTER_COMMIT` y viaja por el outbox best-effort; un fallo se registra y se reintenta, pero la `Request` permanece `CANCELLED` y el recurso liberado. Aceptado por diseño.
- **Solicitante no localizable al renderizar** (empleado borrado) → el correo degrada omitiendo/neutralizando el nombre en lugar de fallar, coherente con el manejo seguro de `Optional` (S3655/S2259) ya presente en el renderer.
- **Muchos admins activos** → una orden por admin: volumen acotado (los admins son pocos); mismo coste que `requestCreated`, ya en producción.
- **Confusión de estados** → publicar el evento en la cancelación de una PENDING generaría avisos espurios; mitigado condicionando estrictamente al estado previo `APPROVED` (D2), cubierto por test de ambas vías.

## Migration Plan

- Sin migración de datos ni de esquema. `REQUEST_CANCELLED` es un valor nuevo del enum `NotificationEventType`; el outbox lo persiste como cualquier otro tipo.
- Despliegue directo: al desplegar, las cancelaciones de APROBADAS empiezan a generar aviso; las entradas de outbox previas no se ven afectadas.
- Rollback: revertir el commit; no quedan datos incompatibles (ningún registro depende del nuevo tipo salvo entradas de outbox pendientes creadas tras el despliegue, que quedarían sin tipo conocido — riesgo bajo y transitorio).

## Open Questions

- Ninguna pendiente. El asunto/copys exactos de `request-cancelled.html` se fijan en implementación siguiendo el tono formal de `request-approved.html`.
