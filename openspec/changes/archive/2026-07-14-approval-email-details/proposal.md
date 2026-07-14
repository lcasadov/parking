## Why

El correo de aprobación de solicitud (`request-approved.html`) es hoy poco útil y poco profesional: saluda de forma informal ("Hola <nombre>,"), identifica el recurso por su **referencia interna** (`requestId`) en lugar del **número real** de la plaza/puesto que el empleado ve en el sitio físico, y no aporta ninguna ayuda visual para localizarlo. El empleado recibe un identificador que no puede correlacionar con la señalización del garaje ni del plano de oficina. Esto genera confusión y consultas de soporte innecesarias.

## What Changes

- **Tono formal**: el cuerpo del correo de aprobación pasa a un saludo formal ("Estimado/a Sr./Sra. `<Nombre Apellidos>`,") y comunica explícitamente que la solicitud de plaza de garaje y/o puesto de trabajo ha sido **APROBADA**.
- **Datos del recurso por NÚMERO real** (no por id/referencia): el correo indica el **número de la plaza** asignada y su **planta** (`planta = número / 1000`) cuando el recurso es una plaza, y el **número del puesto** asignado cuando el recurso es un puesto. Hoy se muestra `requestId`; pasa a resolverse el número real vía `ParkingSpace.number`/`floor()` o `Desk.number` a partir de `resourceId` + `resourceType`.
- **Adjuntar el plano**: el correo de aprobación adjunta la imagen del plano de la planta (`floor-plan.png`) para que el empleado pueda ubicar la plaza/puesto sobre el plano.
- **Alcance del canal**: aplica al correo de **APROBACIÓN al empleado** en sus dos vías, aprobación manual del admin y auto-aprobación automática (ambas emiten el mismo `RequestApprovedEvent` y renderizan `request-approved.html`).
- **Correo de rechazo (opcional)**: se contempla reflejar también el número del recurso en el correo de rechazo cuando aplique, pero no es requisito bloqueante de este change.
- **GAP de infraestructura**: `EmailMessage` NO soporta adjuntos hoy; se extiende el contrato de mensaje y el adaptador SMTP (`SmtpEmailSender`) para enviar correos MIME multipart con adjunto binario, sin romper los correos sin adjunto existentes.

## Capabilities

### New Capabilities
<!-- Ninguna capability nueva; se modifica la existente. -->

### Modified Capabilities
- `notifications`: el requisito de "Aprobación de solicitud notifica al empleado" cambia su comportamiento observable (tono formal, número real de recurso + planta, plano adjunto), y se añade un requisito sobre soporte de adjuntos en el envío de email.

## Impact

- **Código backend afectado**:
  - `notification/application/EmailContentRenderer.java` — nuevo modelo de plantilla (saludo formal, número + planta del recurso, marca de adjunto) para `renderRequestApproved`.
  - `notification/application/EmailMessage.java` — extensión del record para transportar adjunto(s) opcionales.
  - `notification/application/NotificationDispatcher.java` (`requestApproved`) — resolución del número/planta desde `resourceId` + `resourceType` consultando `ParkingSpaceRepository`/`DeskRepository`, y carga del asset del plano.
  - `notification/infrastructure/SmtpEmailSender.java` — `MimeMessageHelper` multipart (`true`) y `addAttachment` cuando el mensaje trae adjunto.
  - `notification/application/EmailSenderPort.java` — contrato sin cambios de firma (sigue recibiendo `EmailMessage`, ahora con adjuntos).
  - Plantilla `templates/email/request-approved.html` — nuevo texto formal y variables `resourceNumber`, `floor`, `resourceType`.
  - Nuevo asset del plano accesible desde el classpath del backend (p. ej. `backend/src/main/resources/templates/email/floor-plan.png`), copiado del asset frontend `frontend/src/assets/floor-plan.png`.
- **Fuentes de datos consultadas**: `ParkingSpace` (`getNumber()`, `floor()`), `Desk` (`getNumber()`), `RequestResponse` (`parkingSpaceId` = `resourceId`, `resourceType`).
- **Sin cambios de API pública** (`docs/openapi.yaml`): el cambio es interno al canal de notificación.
- **Tests**: unitarios de `EmailContentRenderer` (número/planta según tipo), `NotificationDispatcher` (resolución de recurso + adjunto del plano), `SmtpEmailSender` (multipart con adjunto).
