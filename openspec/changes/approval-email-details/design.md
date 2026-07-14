## Context

El canal de notificaciones (hexagonal) separa render (`EmailContentRenderer` + plantillas Thymeleaf), orquestación (`NotificationDispatcher`), entrega resiliente (`NotificationDeliveryService`) y adaptador SMTP (`SmtpEmailSender` implementando `EmailSenderPort`). El mensaje renderizado viaja como un record inmutable `EmailMessage(to, subject, htmlBody)`, autocontenido para permitir reintentos sin re-renderizar.

Estado actual del correo de aprobación:
- `renderRequestApproved` fija en el contexto `requestId`, `requestedDate` y `approvalNote`. La plantilla `request-approved.html` saluda "Hola <nombre>," y muestra el `requestId` como "referencia".
- `RequestResponse` expone `parkingSpaceId` (que es en realidad el genérico `resourceId`, ver `Request.getResourceId()`) y `resourceType` (`PARKING`/`DESK`). NO expone el número ni la planta del recurso.
- `ParkingSpace` tiene `getNumber()` y `floor()` (= `number/1000`). `Desk` tiene `getNumber()` (rango 1-65, sin planta).
- `EmailMessage` NO soporta adjuntos. `SmtpEmailSender` construye el `MimeMessageHelper` con `multipart = false` y solo llama `setText(html, true)`.
- El asset del plano vive en el frontend (`frontend/src/assets/floor-plan.png`); el backend no tiene copia en su classpath.

Restricciones: no romper los otros cuatro correos (created/rejected/revoked/password-reset), mantener la resiliencia (un fallo de correo nunca revierte la operación funcional), y respetar Sonar (S1192 constantes, S3776 complejidad < 15, S2095 recursos).

## Goals / Non-Goals

**Goals:**
- Correo de aprobación con tono formal y comunicación explícita de APROBADO.
- Mostrar el número real del recurso: plaza (`ParkingSpace.number` + planta `floor()`) o puesto (`Desk.number`), resuelto desde `resourceId` + `resourceType`.
- Adjuntar el plano de la planta al correo de aprobación.
- Habilitar adjuntos MIME en la infraestructura de email de forma genérica y retrocompatible.
- Cubrir con tests las dos vías de aprobación (manual admin y auto-aprobación), ambas plataformas de recurso, y el envío multipart.

**Non-Goals:**
- Rediseñar los otros correos (created/rejected/revoked/password-reset). El correo de rechazo con número es opcional y queda fuera del alcance bloqueante.
- Personalizar el saludo por género real del empleado: `Employee` no tiene campo de género/tratamiento; se usa la forma inclusiva "Estimado/a Sr./Sra.".
- Servir el plano desde una URL externa o CID inline en el HTML; se adjunta como fichero.
- Cambios en `docs/openapi.yaml` (el cambio es interno al canal de notificación).

## Decisions

### D1 — Resolver número/planta en el `NotificationDispatcher`, no en la web ni ampliando `RequestResponse`
`requestApproved(RequestResponse)` ya tiene el `resourceId` (`request.parkingSpaceId()`) y el `resourceType()`. Se inyectan `ParkingSpaceRepository` y `DeskRepository` en el dispatcher; según `resourceType`:
- `PARKING` → `parkingSpaceRepository.findById(resourceId)` → `number` + `floor()`.
- `DESK` → `deskRepository.findById(resourceId)` → `number` (planta = null).

Se construye un pequeño value object interno (p. ej. `ResolvedResource(ResourceType type, Integer number, Integer floor)`) que se pasa al renderer. Se extrae a un método privado para mantener `S3776 < 15`.

Alternativa descartada: añadir `resourceNumber`/`floor` a `RequestResponse` — contaminaría el DTO de la API pública con datos que solo el correo necesita, y obligaría a resolver el número en el caso de uso de aprobación. Mantener la resolución en el dispatcher acota el cambio al canal de notificación.

### D2 — Extender `EmailMessage` con adjuntos opcionales (record + componente `EmailAttachment`)
Nuevo record `EmailAttachment(String filename, String contentType, byte[] content)` y ampliación de `EmailMessage` a `EmailMessage(String to, String subject, String htmlBody, List<EmailAttachment> attachments)`. Para no tocar las cuatro llamadas de render existentes, se añade un constructor compacto/fábrica que deja `attachments` como lista vacía inmutable por defecto (`List.of()`), y una sobrecarga/`withAttachments(...)` para el correo de aprobación.

Alternativa descartada: un tipo de mensaje separado `EmailMessageWithAttachment` — duplicaría la lógica de entrega y reintento en `NotificationDeliveryService`/`PendingEmailStore`. Un único tipo con lista (posiblemente vacía) mantiene un solo camino de entrega.

Nota de resiliencia: el `byte[]` del plano viaja dentro del `EmailMessage`, que se conserva para reintento. El asset es pequeño; es aceptable. Si `PendingEmailStore` serializa el mensaje, debe soportar el nuevo campo (revisar en implementación).

### D3 — `SmtpEmailSender` multipart condicional
`MimeMessageHelper` se crea con `multipart = !attachments.isEmpty()` (o siempre `true`, que es inocuo). Para cada `EmailAttachment` se llama `helper.addAttachment(filename, new ByteArrayResource(content), contentType)`. El `try/catch` actual sobre `MessagingException | MailException` sigue traduciendo a `EmailDeliveryException` para la resiliencia. Sin adjuntos, el comportamiento es idéntico al actual (retrocompatible, cubierto por escenario de spec).

### D4 — Ubicación del asset del plano en el backend
El backend no puede leer `frontend/src/assets/`. Se copia el plano a un recurso del classpath del backend, p. ej. `backend/src/main/resources/templates/email/floor-plan.png` (o `.../static/`), y se carga con `new ClassPathResource(...)` (try-with-resources sobre el `InputStream`, S2095) hacia un `byte[]`. Constante con la ruta del recurso y el content-type (`image/png`) para S1192.

Alternativa descartada: referenciar el fichero del frontend por ruta relativa — frágil (depende del layout del repo y del empaquetado del jar). El asset debe estar dentro del artefacto desplegable del backend.

### D5 — Redacción del cuerpo
La plantilla `request-approved.html` pasa a:
- Saludo: `Estimado/a Sr./Sra. <Nombre Apellidos>,` (usa `firstName` + `lastName`).
- Frase de aprobación: "Le comunicamos que su solicitud de plaza de garaje y/o puesto de trabajo ha sido APROBADA."
- Detalle según tipo: para plaza "…se le ha asignado la plaza nº 3005 en la planta 3."; para puesto "…se le ha asignado el puesto nº 12." (bloques `th:if` por `resourceType`).
- Nota del admin condicional (se conserva).
- Mención de que el plano se adjunta para localizar el recurso.

### D6 — Auto-aprobación reutiliza el mismo camino
Tanto la aprobación manual como la auto-aprobación automática emiten `RequestApprovedEvent` → `NotificationDispatcher.requestApproved(...)` → `renderRequestApproved`. No hay bifurcación: la mejora aplica a ambas por construcción. Los tests deben verificar explícitamente que la auto-aprobación produce el correo formal con número y adjunto.

## Risks / Trade-offs

- **[Recurso asignado no encontrado al resolver el número]** (`findById` vacío por borrado/carrera) → Mitigación: si no se resuelve el recurso, degradar con elegancia (log de advertencia y correo sin el número/plano, o texto genérico) sin lanzar excepción que rompa la notificación; nunca revertir la aprobación (`Optional.orElse...`, S3655/S2259).
- **[Peso del `byte[]` del plano en cada `EmailMessage` de aprobación, y en la cola de reintento]** → Mitigación: el asset es pequeño (plano PNG); aceptable. Revisar que `PendingEmailStore` soporte el nuevo campo sin inflar en exceso.
- **[Fallo al cargar el asset del plano]** → Mitigación: capturar y degradar a correo sin adjunto (escenario de spec), registrando el fallo; no bloquear el envío del texto de aprobación.
- **[Regresión en los otros 4 correos por el cambio de `EmailMessage`]** → Mitigación: constructor/fábrica retrocompatible con `attachments = List.of()`; test de "correo sin adjunto se envía igual que antes".
- **[Género/tratamiento del saludo]** → Trade-off: sin campo de género, se usa "Estimado/a Sr./Sra." inclusivo; aceptado por el usuario en el requisito.

## Migration Plan

1. Copiar el asset `floor-plan.png` al classpath del backend.
2. Extender `EmailMessage` + nuevo `EmailAttachment` (retrocompatible).
3. Adaptar `SmtpEmailSender` a multipart condicional.
4. Inyectar repos y resolver número/planta en `NotificationDispatcher.requestApproved`; cargar el plano.
5. Actualizar `renderRequestApproved` + plantilla `request-approved.html`.
6. Tests (TDD: escribir primero los tests de los escenarios de la spec).

Rollback: cambios acotados al módulo `notification` + un asset; revertir el commit restaura el comportamiento previo sin migración de datos.

## Open Questions

- ¿Debe el `PendingEmailStore` persistir el adjunto (para reintento fiel) o recargarlo en el reintento? Preferencia: incluirlo en el mensaje encolado por simplicidad; confirmar tamaño/serialización en implementación.
- ¿Reflejar el número también en el correo de rechazo? Opcional; se deja fuera del alcance bloqueante y se decide en implementación si es trivial.
