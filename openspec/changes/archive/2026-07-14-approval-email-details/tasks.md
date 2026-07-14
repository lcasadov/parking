## 1. Asset del plano en el backend

- [x] 1.1 Copiar el asset del plano `frontend/src/assets/floor-plan.png` al classpath del backend en `backend/src/main/resources/templates/email/floor-plan.png`

## 2. Soporte de adjuntos en el contrato de email (TDD)

- [x] 2.1 (RED) Test de `SmtpEmailSender`: un `EmailMessage` con un `EmailAttachment` produce un `MimeMessage` multipart con el adjunto (nombre + content-type); un `EmailMessage` sin adjuntos se envía como antes (HTML sin partes adjuntas)
- [x] 2.2 Crear el record `EmailAttachment(String filename, String contentType, byte[] content)` en `notification/application`
- [x] 2.3 Extender `EmailMessage` a `(String to, String subject, String htmlBody, List<EmailAttachment> attachments)` con fábrica/constructor retrocompatible que deja `attachments = List.of()` por defecto (no romper los 4 render existentes)
- [x] 2.4 (GREEN) Adaptar `SmtpEmailSender.send`: `MimeMessageHelper` con `multipart` condicional, `addAttachment(filename, new ByteArrayResource(content), contentType)` por cada adjunto; mantener la traducción a `EmailDeliveryException`
- [x] 2.5 Verificar/ajustar `PendingEmailStore` y `NotificationDeliveryService` para transportar el nuevo campo `attachments` en el reintento sin regresión

## 3. Resolución del número real del recurso (TDD)

- [x] 3.1 (RED) Test de `NotificationDispatcher.requestApproved`: para `resourceType = PARKING` resuelve `ParkingSpace.number` y `floor()`; para `DESK` resuelve `Desk.number` (planta null); recurso no encontrado degrada sin lanzar excepción
- [x] 3.2 Inyectar `ParkingSpaceRepository` y `DeskRepository` en `NotificationDispatcher`
- [x] 3.3 Implementar la resolución (método privado, `S3776 < 15`) devolviendo un value object interno `ResolvedResource(type, number, floor)` a partir de `request.parkingSpaceId()` (resourceId) + `request.resourceType()`, con manejo seguro de `Optional` (S3655/S2259)
- [x] 3.4 Cargar el asset del plano desde el classpath (`ClassPathResource`, try-with-resources sobre el `InputStream`, S2095) a `byte[]`, con constantes de ruta y content-type `image/png` (S1192); degradar a correo sin adjunto si falla la carga

## 4. Renderizado formal del correo de aprobación (TDD)

- [x] 4.1 (RED) Test de `EmailContentRenderer.renderRequestApproved`: el `EmailMessage` resultante lleva saludo formal con nombre+apellidos, el número real del recurso (plaza+planta o puesto), la nota del admin si existe, y el adjunto del plano; NO muestra el `requestId` como referencia del recurso
- [x] 4.2 Ampliar `renderRequestApproved` para recibir el `ResolvedResource` (número/planta/tipo) y el adjunto del plano; fijar variables `resourceType`, `resourceNumber`, `floor` en el `Context`; usar `firstName` + `lastName` para el saludo; adjuntar el plano al `EmailMessage`
- [x] 4.3 Reescribir la plantilla `templates/email/request-approved.html`: saludo "Estimado/a Sr./Sra. `<Nombre Apellidos>`,", frase de APROBADA, bloque `th:if` para plaza ("la plaza nº X en la planta Y") y para puesto ("el puesto nº X"), nota del admin condicional, mención del plano adjunto
- [x] 4.4 Actualizar el asunto/constantes si procede (mantener S1192: constantes `static final`)

## 5. Verificación de ambas vías de aprobación (TDD)

- [x] 5.1 Test: la aprobación manual del admin produce el correo formal con número + planta + adjunto
- [x] 5.2 Test: la auto-aprobación automática produce el mismo correo formal con número + planta + adjunto (mismo camino `RequestApprovedEvent` → `requestApproved`)

## 6. (Opcional) Número de recurso en el correo de rechazo

- [ ] 6.1 (Opcional, no bloqueante) Evaluar reflejar el número del recurso en `request-rejected.html` cuando aplique; implementar solo si es trivial y sin ampliar el alcance

## 7. Quality Gate

- [x] 7.1 `mvn -f backend/pom.xml clean verify` verde: 0 failures, cobertura ≥80% líneas / ≥75% branches
- [x] 7.2 0 violations Sonar nuevas (S1192, S3776, S2095, S3655, S2259, S2699, S2925); sin `Thread.sleep` en tests
- [x] 7.3 `openspec validate "approval-email-details"` válido
