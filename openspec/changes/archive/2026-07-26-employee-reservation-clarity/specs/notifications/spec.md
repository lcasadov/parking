## ADDED Requirements

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
