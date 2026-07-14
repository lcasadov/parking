## MODIFIED Requirements

### Requirement: Envío de email tras evento confirmado (AFTER_COMMIT)
**El sistema DEBE (MUST) enviar el email correspondiente a cada evento de dominio una vez confirmada (`AFTER_COMMIT`) la transacción que lo origina, usando la plantilla Thymeleaf y los destinatarios definidos, y NO debe enviarlo si la transacción se revierte. El correo de aprobación al empleado DEBE usar un tono formal, identificar el recurso asignado por su NÚMERO real (no por referencia/id interno) y su planta cuando aplique, y adjuntar el plano de la planta.**

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

#### Scenario: Aprobación conserva la nota del administrador
- **GIVEN** una `Request` en estado `PENDING` con `approval_note` y un admin que la aprueba
- **WHEN** se confirma (`AFTER_COMMIT`) la transacción que pasa la `Request` a `APPROVED`
- **THEN** el sistema envía el email `request-approved.html` al `Employee` solicitante
- **AND** el cuerpo incluye el `approval_note` introducido por el admin, además del número del recurso y el plano adjunto

#### Scenario: Auto-aprobación automática usa el mismo correo formal con número y plano
- **GIVEN** una `Request` que el sistema auto-aprueba automáticamente (sin intervención manual del admin) asignando un recurso disponible
- **WHEN** se confirma (`AFTER_COMMIT`) la transacción de auto-aprobación
- **THEN** el sistema envía el mismo email `request-approved.html` formal, con el número real del recurso, la planta cuando aplique y el plano adjunto, idéntico al de la aprobación manual

#### Scenario: Transacción revertida no genera email
- **GIVEN** un caso de uso que dispara un evento de notificación
- **WHEN** la transacción del caso de uso falla y se hace rollback
- **THEN** el sistema NO envía ningún email
- **AND** no registra intento de envío para ese evento

## ADDED Requirements

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
