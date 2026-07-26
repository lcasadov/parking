## ADDED Requirements

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
