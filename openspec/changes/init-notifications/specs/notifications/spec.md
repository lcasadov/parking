# Capability: notifications

## Resumen
Notificaciones por email transversales del sistema, disparadas por eventos de
dominio (no por endpoints propios). Los correos se envían `AFTER_COMMIT` de la
transacción que origina el evento, usando plantillas Thymeleaf. Si el envío SMTP
falla, se registra en log y se reintenta mediante un job programado, sin revertir
nunca la operación funcional ya confirmada.

## Fase
🟢🔵 Fase 1 y Fase 2 (el evento de reset de contraseña por email solo aplica en 🔵 Fase 2).

## Reglas de negocio implicadas
(README §"Notificaciones por email"; NO hay códigos RN-xx)
- Envío `AFTER_COMMIT` de la transacción que dispara el evento: si la transacción falla, no se manda email.
- Si el envío SMTP falla, se registra en log y se reintenta mediante un job programado; **nunca** revierte la operación funcional.
- Eventos y destinatarios: nueva solicitud → todos los admins activos; solicitud aprobada/rechazada → empleado solicitante; asignación fija revocada → empleado afectado; 🔵 reset de contraseña → empleado afectado.
- El `approval_note` del admin viaja en el email de aprobación; el `rejection_reason` viaja en el email de rechazo.
- No se envía email al liberar un recurso voluntariamente ni al cancelar la propia solicitud.
- Plantillas Thymeleaf (`request-created.html`, `request-approved.html`, `request-rejected.html`, `assignment-revoked.html`, `password-reset.html`).
- Los visitantes no tienen cuenta y no reciben emails (la reserva de visitante no dispara notificación).

## Entidades implicadas
- Employee (destinatario: `email`, `active`, `role`)
- Request (eventos de creación/aprobación/rechazo; `approval_note`, `rejection_reason_code`, `rejection_reason`)
- FixedAssignment (evento de revocación)
- (Plantillas Thymeleaf — sin entidad propia; la cola de reintento se modela como almacén de emails fallidos, ver `docs/data-model.md`) _[verificar con docs/data-model.md: no existe tabla explícita de cola de email/email_log; se asume almacén de reintento]_

## Endpoints
- (Ninguno — capability transversal sin API propia; reacciona a eventos de dominio de `requests`, `fixed-assignments` y `employees`.)

## Permisos
| Rol | Permisos |
|---|---|
| ADMIN | Recibe el email de "nueva solicitud" (si `active = true`). No dispone de endpoints de gestión de notificaciones. |
| EMPLOYEE | Recibe los emails dirigidos a él (aprobación, rechazo, asignación revocada, 🔵 reset de contraseña). |

## ADDED Requirements
### Requirement: Envío de email tras evento confirmado (AFTER_COMMIT)
**El sistema DEBE enviar el email correspondiente a cada evento de dominio una vez confirmada (`AFTER_COMMIT`) la transacción que lo origina, usando la plantilla Thymeleaf y los destinatarios definidos, y NO debe enviarlo si la transacción se revierte.**

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
**El sistema DEBE, cuando el envío SMTP falla, registrar el fallo en log y encolar el email para reintento mediante un job programado, sin revertir nunca la operación funcional ya confirmada.**

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
**El sistema DEBE NO enviar email en los eventos excluidos: liberación voluntaria de recurso, cancelación de la propia solicitud por el empleado, y reservas/eventos de visitante.**

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

## Casos límite (edge cases)
- Empleado destinatario sin `email` válido: se registra el fallo en log y no se intenta reintento indefinido (se descarta tras agotar la política de reintentos). _[verificar con docs/data-model.md: política de máximo de reintentos no definida]_
- Sin admins activos al crear una solicitud: no hay destinatarios; el sistema no falla y no encola email.
- 🔵 El evento de reset de contraseña por email solo se dispara en Fase 2; en Fase 1 la contraseña temporal se muestra al admin y no se envía email.
- El email es un efecto lateral: su fallo nunca propaga error HTTP al cliente que originó el evento (la respuesta del endpoint origen ya se devolvió tras el commit).
- Idempotencia del reintento: un email ya enviado no se reenvía aunque el job se ejecute de nuevo.

## Dependencias con otras capabilities
- Depende de `requests` (eventos de creación, aprobación y rechazo de solicitud).
- Depende de `fixed-assignments` (evento de revocación de asignación fija).
- Depende de `employees` (🔵 evento de reset de contraseña; y para resolver los admins activos destinatarios).
- Comparte con `releases` la regla de exclusión (la liberación voluntaria NO notifica).
- El efecto observable para el usuario son los toasts de las pantallas que disparan los eventos (sin UI propia de esta capability).
