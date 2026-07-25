## ADDED Requirements

### Requirement: Reenvío del aviso de una solicitud PENDING propia estancada
El sistema DEBE (MUST) permitir al `Employee` dueño de una `Request` en estado `PENDING` reenviar el aviso a los administradores mediante `POST /requests/{id}/resend`, re-notificando `AFTER_COMMIT` a todos los `Employee` con `role = ADMIN` y `active = true` (reutilizando el mismo evento y plantilla que la creación de la solicitud). La operación DEBE (MUST) exigir que hayan transcurrido al menos 24 horas desde la creación de la solicitud o, si ya se reenvió antes, desde el último reenvío (`last_reminded_at`). Cada reenvío exitoso DEBE (MUST) actualizar `last_reminded_at` al instante actual. La operación NO DEBE (MUST NOT) modificar el estado ni ningún otro dato de la solicitud, y NO DEBE (MUST NOT) admitirse sobre una solicitud ajena ni sobre una que ya no esté `PENDING`.

#### Scenario: El dueño reenvía el aviso de su solicitud PENDING tras el cooldown
- **GIVEN** un `Employee` dueño de una `Request` en estado `PENDING` cuya `created_at` (o `last_reminded_at`, si existe) tiene 24 horas o más de antigüedad
- **WHEN** envía `POST /requests/{id}/resend`
- **THEN** el sistema responde 200 con la solicitud (estado `PENDING` sin cambios) y `lastRemindedAt` actualizado al instante actual
- **AND** dispara la misma notificación que la creación de la solicitud (`REQUEST_CREATED`) a todos los `Employee` con `role = ADMIN` y `active = true`

#### Scenario: Reenvío antes de cumplirse el cooldown
- **GIVEN** un `Employee` dueño de una `Request` `PENDING` cuya `created_at` (o `last_reminded_at`) tiene menos de 24 horas
- **WHEN** envía `POST /requests/{id}/resend`
- **THEN** el sistema responde 409 con `error = RESEND_TOO_SOON`
- **AND** no modifica `last_reminded_at` ni dispara ninguna notificación

#### Scenario: Reenvío de una solicitud que ya no está PENDING
- **GIVEN** un `Employee` dueño de una `Request` en estado `APPROVED`, `REJECTED` o `CANCELLED`
- **WHEN** envía `POST /requests/{id}/resend`
- **THEN** el sistema responde 409 con `error = REQUEST_NOT_PENDING`
- **AND** no modifica la solicitud ni dispara ninguna notificación

#### Scenario: Reenvío de la solicitud de otro empleado
- **GIVEN** un `Employee` autenticado que NO es el dueño de la `Request`
- **WHEN** envía `POST /requests/{id}/resend`
- **THEN** el sistema responde 403 (verificación de pertenencia) y no modifica la solicitud

#### Scenario: Reenvíos sucesivos exigen el cooldown completo cada vez
- **GIVEN** un `Employee` que acaba de reenviar con éxito el aviso de su `Request` `PENDING` (`last_reminded_at` actualizado ahora)
- **WHEN** envía de nuevo `POST /requests/{id}/resend` antes de que pasen 24 horas desde ese reenvío
- **THEN** el sistema responde 409 con `error = RESEND_TOO_SOON` (la referencia es el último reenvío, no la creación original)
