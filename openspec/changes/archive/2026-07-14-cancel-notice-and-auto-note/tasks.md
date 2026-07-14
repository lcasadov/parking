## 1. Evento de dominio y publicación en la cancelación (Requisito A)

- [x] 1.1 Escribir test (RED) en `RequestServiceTest`: cancelar una `Request` cuyo estado previo es `APPROVED` publica exactamente un `RequestCancelledEvent` con el `RequestResponse` correcto; cancelar una `PENDING` no publica ningún evento
- [x] 1.2 Crear el record `notification/event/RequestCancelledEvent(RequestResponse request)` siguiendo el patrón de los demás eventos de la capability
- [x] 1.3 En `RequestService.cancel`, capturar el estado previo y publicar `RequestCancelledEvent` solo cuando ese estado era `APPROVED` (recurso liberado); no publicar en `PENDING` (GREEN)

## 2. Listener y dispatcher hacia los admins activos (Requisito A)

- [x] 2.1 Escribir test (RED) en `NotificationDispatcherTest`: `requestCancelled` emite una `NotificationCommand(REQUEST_CANCELLED, adminId, request)` por cada admin activo (`findByRoleAndActiveTrue(ADMIN)`) y ninguna si no hay admins activos
- [x] 2.2 Añadir `NotificationEventType.REQUEST_CANCELLED` al enum
- [x] 2.3 Implementar `NotificationDispatcher.requestCancelled(RequestResponse)` reutilizando el patrón de `requestCreated` (GREEN)
- [x] 2.4 Enrutar `RequestCancelledEvent` en `EmailNotificationListener` (`@TransactionalEventListener` AFTER_COMMIT) a `NotificationDispatcher.requestCancelled`

## 3. Render del correo de cancelación con nombre del solicitante y número (Requisito A)

- [x] 3.1 Escribir test (RED) para `EmailContentRenderer.renderRequestCancelled`: el cuerpo incluye nombre y apellidos del solicitante, la fecha de la solicitud y el número del recurso liberado (plaza nº X en planta Y / puesto nº Z); degrada sin número si el recurso no se localiza
- [x] 3.2 Crear la plantilla Thymeleaf `templates/email/request-cancelled.html` (tono formal, coherente con `request-approved.html`)
- [x] 3.3 Implementar `EmailContentRenderer.renderRequestCancelled(admin, request, resolved)` (GREEN)
- [x] 3.4 Añadir la rama `case REQUEST_CANCELLED` en `NotificationRenderer.renderFor`, resolviendo el solicitante por `employeeRepository.findById(request.employeeId())` y el recurso por `resolveResource(request)`; manejo seguro de `Optional` (S3655/S2259)

## 4. Ocultar la nota "auto" en el correo de aprobación (Requisito B)

- [x] 4.1 Escribir test (RED) para `renderRequestApproved`: con `approvalNote = "auto"` el contexto Thymeleaf recibe `approvalNote = null` (línea de nota oculta); con una nota real se conserva
- [x] 4.2 En `EmailContentRenderer.renderRequestApproved`, tratar `approvalNote` igual a `Request.AUTO_APPROVAL_NOTE` como ausente (fijar `null` en el contexto), reutilizando la constante existente sin literales mágicos (S1192) (GREEN)
- [x] 4.3 Verificar que `request-approved.html` mantiene su `th:if` de la nota sin cambios (o ajustar sin introducir el literal "auto" en la plantilla)

## 5. Tests de integración de ambas vías

- [x] 5.1 Test de integración: cancelar una solicitud APROBADA produce el correo a los admins activos con el número del recurso liberado (vía completa evento → listener → dispatcher → renderer)
- [x] 5.2 Test de integración: cancelar una solicitud PENDING no produce ningún correo
- [x] 5.3 Test de integración: auto-aprobación no muestra la línea "Nota del administrador"; aprobación manual con nota real sí la muestra
- [x] 5.4 Test: un fallo SMTP del aviso de cancelación no revierte la cancelación (la `Request` permanece `CANCELLED`)

## 6. Quality Gate

- [x] 6.1 `mvn clean verify` — 0 failures, cobertura ≥80% líneas / ≥75% branches
- [x] 6.2 `mvn sonar:sonar` — 0 violations nuevas (complejidad cognitiva <15, sin literales duplicados)
- [x] 6.3 Verificar sin migración de BD ni cambios de API pública; actualizar `openspec/changes/.../tasks.md` marcando lo completado
