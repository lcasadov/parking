# notifications Specification

## Purpose
TBD - created by archiving change init-notifications. Update Purpose after archive.
## Requirements
### Requirement: Envío de email tras evento confirmado (AFTER_COMMIT)
**El sistema DEBE (MUST) enviar el email correspondiente a cada evento de dominio una vez confirmada (`AFTER_COMMIT`) la transacción que lo origina, usando la plantilla Thymeleaf y los destinatarios definidos, y NO debe enviarlo si la transacción se revierte.**

#### Scenario: Nueva solicitud notifica a todos los admins activos
- **GIVEN** un `Employee` que crea una `Request` y la transacción de creación se confirma
- **WHEN** se completa el commit del caso de uso de creación de solicitud
- **THEN** el sistema envía el email con la plantilla `request-created.html` a todos los `Employee` con `role = ADMIN` y `active = true`
- **AND** no incluye a los admins con `active = false`

#### Scenario: Aprobación de solicitud notifica al empleado con la nota del admin
- **GIVEN** una `Request` en estado `PENDING` con `approval_note` y un admin que la aprueba
- **WHEN** se confirma (`AFTER_COMMIT`) la transacción que pasa la `Request` a `APPROVED`
- **THEN** el sistema envía el email `request-approved.html` al `Employee` solicitante
- **AND** el cuerpo incluye el `approval_note` introducido por el admin

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
**El sistema DEBE (MUST) NO enviar email en los eventos excluidos: liberación voluntaria de recurso, cancelación de la propia solicitud por el empleado, y reservas/eventos de visitante.**

#### Scenario: Liberación voluntaria no genera email
- **GIVEN** un `Employee` con asignación fija que libera voluntariamente su recurso para una fecha (`Release` de tipo `VOLUNTARY`)
- **WHEN** se confirma la transacción de creación del `Release`
- **THEN** el sistema NO envía ningún email

#### Scenario: Cancelación de la propia solicitud no genera email
- **GIVEN** un `Employee` con una `Request` en estado `PENDING`
- **WHEN** el empleado cancela su propia solicitud (`Request` pasa a `CANCELLED`)
- **THEN** el sistema NO envía ningún email

#### Scenario: Revocación de asignación fija sí notifica al empleado afectado
- **GIVEN** un admin que revoca una `FixedAssignment` (`active = false`, `revoked_*`)
- **WHEN** se confirma (`AFTER_COMMIT`) la transacción de revocación
- **THEN** el sistema envía el email `assignment-revoked.html` al `Employee` afectado

