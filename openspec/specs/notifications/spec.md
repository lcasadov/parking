# notifications Specification

## Purpose
TBD - created by archiving change init-notifications. Update Purpose after archive.
## Requirements
### Requirement: Envío de email tras evento confirmado (AFTER_COMMIT)
**El sistema DEBE (MUST) enviar el email correspondiente a cada evento de dominio una vez confirmada (`AFTER_COMMIT`) la transacción que lo origina, usando la plantilla Thymeleaf y los destinatarios definidos, y NO debe enviarlo si la transacción se revierte. El correo de aprobación al empleado DEBE usar un tono formal, identificar el recurso asignado por su NÚMERO real (no por referencia/id interno) y su planta cuando aplique, y adjuntar el plano de la planta. En las auto-aprobaciones, cuando la nota de aprobación es la constante interna de auto-aprobación (`Request.AUTO_APPROVAL_NOTE = "auto"`), el correo DEBE (MUST) tratarla como ausente y NO mostrar la línea "Nota del administrador"; las notas reales escritas por un admin se siguen mostrando.**

#### Scenario: Nueva solicitud notifica a todos los admins activos
- **GIVEN** un `Employee` que crea una `Request` y la transacción de creación se confirma
- **WHEN** se completa el commit del caso de uso de creación de solicitud
- **THEN** el sistema envía el email con la plantilla `request-created.html` a todos los `Employee` con `role = ADMIN` y `active = true`
- **AND** no incluye a los admins con `active = false`

#### Scenario: Aprobación de plaza indica número real, planta y adjunta el plano
- **GIVEN** una `Request` de tipo `PARKING` aprobada cuyo `resourceId` apunta a una `ParkingSpace` con `number = 3005`
- **WHEN** se confirma (`AFTER_COMMIT`) la transacción que pasa la `Request` a `APPROVED`
- **THEN** el sistema resuelve el número de la plaza (`ParkingSpace.number` = 3005) y su planta (`floor()` = `number / 1000` = 3) a partir de `resourceId` y `resourceType`
- **AND** envía el email `request-approved.html` al `Employee` solicitante con un saludo formal ("Estimado/a Sr./Sra. `<Nombre Apellidos>`,") que comunica que su solicitud ha sido APROBADA e indica "la plaza nº 3005 en la planta 3"
- **AND** el correo NO muestra el `requestId`/referencia interna como identificador del recurso
- **AND** el correo lleva adjunta la imagen del plano de la planta (`floor-plan.png`)

#### Scenario: Aprobación de puesto indica el número de puesto y adjunta el plano
- **GIVEN** una `Request` de tipo `DESK` aprobada cuyo `resourceId` apunta a un `Desk` con `number = 12`
- **WHEN** se confirma (`AFTER_COMMIT`) la transacción que pasa la `Request` a `APPROVED`
- **THEN** el sistema resuelve el número del puesto (`Desk.number` = 12) a partir de `resourceId` y `resourceType`
- **AND** envía el email `request-approved.html` al `Employee` solicitante con saludo formal indicando "el puesto nº 12", sin planta (los puestos no tienen planta derivada)
- **AND** el correo lleva adjunta la imagen del plano de la planta (`floor-plan.png`)

#### Scenario: Aprobación manual conserva la nota real del administrador
- **GIVEN** una `Request` en estado `PENDING` con `approval_note` escrita por un admin (distinta de `"auto"`) y un admin que la aprueba
- **WHEN** se confirma (`AFTER_COMMIT`) la transacción que pasa la `Request` a `APPROVED`
- **THEN** el sistema envía el email `request-approved.html` al `Employee` solicitante
- **AND** el cuerpo muestra la línea "Nota del administrador" con el `approval_note` introducido por el admin, además del número del recurso y el plano adjunto

#### Scenario: Auto-aprobación no muestra la nota interna "auto"
- **GIVEN** una `Request` que el sistema auto-aprueba automáticamente asignando un recurso disponible, con `approval_note` igual a `Request.AUTO_APPROVAL_NOTE` (`"auto"`)
- **WHEN** se confirma (`AFTER_COMMIT`) la transacción de auto-aprobación
- **THEN** el sistema envía el mismo email `request-approved.html` formal con el número real del recurso, la planta cuando aplique y el plano adjunto
- **AND** el correo NO muestra la línea "Nota del administrador" (la nota `"auto"` se trata como ausente)

#### Scenario: Transacción revertida no genera email
- **GIVEN** un caso de uso que dispara un evento de notificación
- **WHEN** la transacción del caso de uso falla y se hace rollback
- **THEN** el sistema NO envía ningún email
- **AND** no registra intento de envío para ese evento

### Requirement: Resiliencia ante fallo SMTP con reintento programado
**El sistema DEBE (MUST), cuando el envío SMTP falla, registrar el fallo en log y encolar el email para reintento mediante un job programado, sin revertir nunca la operación funcional ya confirmada.**

#### Scenario: Fallo SMTP no revierte la operación funcional
- **GIVEN** una `Request` ya pasada a `APPROVED` con commit confirmado
- **WHEN** el servidor SMTP rechaza o no responde al enviar `request-approved.html`
- **THEN** el sistema registra el fallo en log y deja el email pendiente de reintento
- **AND** la `Request` permanece `APPROVED` (la operación funcional NO se revierte)

#### Scenario: Job programado reintenta los emails fallidos
- **GIVEN** uno o más emails marcados como fallidos pendientes de reintento
- **WHEN** se ejecuta el job programado de reintento de notificaciones
- **THEN** el sistema reintenta el envío SMTP de cada email pendiente
- **AND** marca como enviado el que tiene éxito y conserva pendiente el que vuelve a fallar

### Requirement: Exclusiones de notificación
**El sistema DEBE (MUST) NO enviar email en los eventos excluidos: liberación voluntaria de recurso, cancelación por el empleado de una solicitud aún en estado `PENDING`, y reservas/eventos de visitante. La cancelación por el empleado de una solicitud ya `APPROVED` NO está excluida: se rige por el requisito de aviso al administrador (recurso liberado).**

#### Scenario: Liberación voluntaria no genera email
- **GIVEN** un `Employee` con asignación fija que libera voluntariamente su recurso para una fecha (`Release` de tipo `VOLUNTARY`)
- **WHEN** se confirma la transacción de creación del `Release`
- **THEN** el sistema NO envía ningún email

#### Scenario: Cancelación de una solicitud PENDING no genera email
- **GIVEN** un `Employee` con una `Request` en estado `PENDING`
- **WHEN** el empleado cancela su propia solicitud (`Request` pasa a `CANCELLED`)
- **THEN** el sistema NO envía ningún email

#### Scenario: Revocación de asignación fija sí notifica al empleado afectado
- **GIVEN** un admin que revoca una `FixedAssignment` (`active = false`, `revoked_*`)
- **WHEN** se confirma (`AFTER_COMMIT`) la transacción de revocación
- **THEN** el sistema envía el email `assignment-revoked.html` al `Employee` afectado

### Requirement: Soporte de adjuntos binarios en el envío de email
**El sistema DEBE (MUST) permitir que un `EmailMessage` transporte cero o más adjuntos binarios (nombre de fichero, tipo MIME y contenido) y el adaptador SMTP DEBE enviarlos como un mensaje MIME multipart, sin alterar el envío de los correos que no llevan adjunto.**

#### Scenario: Correo con adjunto se envía como MIME multipart
- **GIVEN** un `EmailMessage` renderizado con al menos un adjunto (p. ej. `floor-plan.png`, `image/png`)
- **WHEN** el adaptador SMTP procesa el mensaje
- **THEN** construye un `MimeMessage` multipart y añade cada adjunto con su nombre y tipo MIME
- **AND** el destinatario recibe el correo con el/los adjunto(s)

#### Scenario: Correo sin adjunto sigue enviándose igual que antes
- **GIVEN** un `EmailMessage` sin adjuntos (p. ej. `request-created.html`, `request-rejected.html`)
- **WHEN** el adaptador SMTP procesa el mensaje
- **THEN** envía el correo HTML sin ninguna parte adjunta, con el mismo comportamiento previo a este cambio
- **AND** no se produce ningún fallo por la ausencia de adjuntos

#### Scenario: Fallo al cargar el adjunto no revierte la operación funcional
- **GIVEN** una `Request` ya pasada a `APPROVED` con commit confirmado cuyo correo debe adjuntar el plano
- **WHEN** el asset del plano no puede cargarse o el envío del adjunto falla
- **THEN** el sistema registra el fallo en log y encola el email para reintento (o envía sin adjunto según la política de resiliencia), sin revertir nunca la aprobación
- **AND** la `Request` permanece `APPROVED`

### Requirement: Aviso al administrador cuando el empleado cancela una solicitud APROBADA

El sistema DEBE (MUST), cuando un empleado cancela una `Request` cuyo estado previo era `APPROVED` (con lo que el recurso reservado se libera), enviar tras `AFTER_COMMIT` el email `request-cancelled.html` a todos los `Employee` con `role = ADMIN` y `active = true`, informando de quién canceló (nombre y apellidos del empleado solicitante, resuelto por `request.employeeId()`), la fecha de la solicitud y el recurso liberado identificado por su NÚMERO real (plaza nº X en planta Y / puesto nº Z), resuelto igual que en el correo de aprobación. La cancelación de una `Request` en estado `PENDING` NO DEBE (MUST NOT) generar ningún email. Un fallo de envío NO DEBE (MUST NOT) revertir la cancelación ya confirmada.

#### Scenario: Cancelación de solicitud APROBADA notifica a los admins activos con el número del recurso
- **GIVEN** un `Employee` con una `Request` en estado `APPROVED` cuyo recurso es una `ParkingSpace` con `number = 3005`
- **WHEN** el empleado cancela la solicitud (pasa a `CANCELLED`, se libera el recurso) y la transacción se confirma (`AFTER_COMMIT`)
- **THEN** el sistema envía el email `request-cancelled.html` a todos los `Employee` con `role = ADMIN` y `active = true`
- **AND** el cuerpo indica el nombre y apellidos del empleado solicitante (resuelto por `request.employeeId()`), la fecha de la solicitud y "la plaza nº 3005 en la planta 3"
- **AND** no incluye a los admins con `active = false`

#### Scenario: Cancelación de solicitud PENDING no genera aviso
- **GIVEN** un `Employee` con una `Request` en estado `PENDING`
- **WHEN** el empleado cancela la solicitud (pasa a `CANCELLED`, no se libera ningún recurso)
- **THEN** el sistema NO envía ningún email a ningún administrador

#### Scenario: Fallo SMTP del aviso no revierte la cancelación
- **GIVEN** una `Request` previamente `APPROVED` que el empleado cancela y cuyo commit se confirma
- **WHEN** el servidor SMTP rechaza o no responde al enviar `request-cancelled.html`
- **THEN** el sistema registra el fallo y encola el email para reintento
- **AND** la `Request` permanece `CANCELLED` y el recurso liberado (la operación funcional NO se revierte)

### Requirement: Re-aviso a los admins ante el reenvío de una solicitud pendiente
El sistema DEBE (MUST), cuando un empleado reenvía el aviso de su `Request` `PENDING` (`POST /requests/{id}/resend`), enviar tras `AFTER_COMMIT` el mismo email de creación de solicitud (`REQUEST_CREATED`, plantilla `request-created.html`) a todos los `Employee` con `role = ADMIN` y `active = true`, sin usar una plantilla distinta. Un fallo de envío NO DEBE (MUST NOT) revertir el reenvío ya confirmado (mismo tratamiento de resiliencia SMTP que el resto de eventos de notificación).

#### Scenario: El reenvío notifica a los admins activos con la misma plantilla que la creación
- **GIVEN** una `Request` en estado `PENDING` cuyo dueño reenvía el aviso con éxito
- **WHEN** se confirma (`AFTER_COMMIT`) la transacción del reenvío
- **THEN** el sistema envía el email `request-created.html` a todos los `Employee` con `role = ADMIN` y `active = true`
- **AND** no incluye a los admins con `active = false`

#### Scenario: Fallo SMTP del re-aviso no revierte el reenvío
- **GIVEN** un reenvío de aviso cuya transacción se confirma (`last_reminded_at` actualizado)
- **WHEN** el servidor SMTP rechaza o no responde al enviar `request-created.html`
- **THEN** el sistema registra el fallo en log y encola el email para reintento
- **AND** `last_reminded_at` permanece actualizado (la operación funcional NO se revierte)

### Requirement: Notificación de promoción de lista de espera al empleado (modo AUTOMÁTICO)
Cuando el sistema promociona una solicitud en espera a `APPROVED` al liberarse un recurso (modo AUTOMÁTICO), DEBE (MUST) notificar al empleado solicitante que se le ha asignado el recurso, indicando el recurso y la fecha. La notificación se emite `AFTER_COMMIT` y se encola en el `email_outbox` (reintento), reutilizando el mecanismo existente.

#### Scenario: El empleado promovido recibe el aviso de asignación
- **GIVEN** una solicitud en espera de un empleado para (F, T)
- **WHEN** el sistema la promociona a `APPROVED` al liberarse un recurso
- **THEN** se encola una notificación al empleado con el recurso asignado y la fecha F

### Requirement: Aviso a administradores de liberación con cola (modo MANUAL)
Cuando se libera un recurso para una fecha en modo MANUAL y existen solicitudes en espera para (F, T), el sistema DEBE (MUST) notificar a los administradores activos que hay un recurso disponible para F con N solicitudes en espera, para que lo asignen. La notificación se emite `AFTER_COMMIT` vía `email_outbox`.

#### Scenario: Aviso al admin cuando se libera con cola
- **GIVEN** modo MANUAL y N (>0) solicitudes en espera para (F, T)
- **WHEN** se libera un recurso de tipo T para F
- **THEN** se encola una notificación a los administradores activos indicando F, el tipo T y N en espera

