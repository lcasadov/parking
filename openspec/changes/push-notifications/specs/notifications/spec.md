## ADDED Requirements

### Requirement: Entrega multi-canal gobernada por canal habilitado

El sistema SHALL entregar cada notificación de dominio por los canales **habilitados globalmente** (email y/o push), de forma independiente por canal. Cada canal SHALL comprobar su propio flag antes de enviar; deshabilitar un canal NO SHALL afectar al otro. El email SHALL permanecer como canal y fallback.

#### Scenario: Ambos canales habilitados

- **GIVEN** `emailNotificationsEnabled = true` y `pushNotificationsEnabled = true`
- **WHEN** se dispara una notificación dirigida a un usuario con suscripción push
- **THEN** el usuario recibe email y push

#### Scenario: Solo push habilitado

- **GIVEN** `emailNotificationsEnabled = false` y `pushNotificationsEnabled = true`
- **WHEN** se dispara una notificación
- **THEN** el sistema envía solo push y no envía email

#### Scenario: Ambos canales deshabilitados

- **GIVEN** `emailNotificationsEnabled = false` y `pushNotificationsEnabled = false`
- **WHEN** se dispara una notificación
- **THEN** el sistema no envía ninguna notificación (silencio asumido por el ADMIN)

### Requirement: Aviso al empleado en cambios de estado de su reserva

El sistema SHALL notificar al empleado destino (por los canales habilitados) cuando su solicitud es aprobada/confirmada, rechazada, cambiada por el admin (reasignación o intercambio), o cancelada por un **admin** siendo aprobada. El empleado que cancela su propia solicitud NO SHALL recibir un aviso de esa cancelación (la inició él). La disponibilidad de un hueco de lista de espera NO se avisa al empleado (se resuelve por el admin — ver capability `request-waitlist`).

#### Scenario: Recurso confirmado

- **WHEN** una solicitud del empleado pasa a aprobada (por el admin, auto-asignación o asignación puntual admin)
- **THEN** el empleado recibe un aviso de confirmación con el recurso y la fecha por los canales habilitados

#### Scenario: Recurso cambiado por el admin

- **WHEN** el admin reasigna o intercambia el recurso del empleado para una fecha
- **THEN** el empleado recibe un aviso indicando el nuevo recurso y la fecha

#### Scenario: Reserva rechazada

- **WHEN** el admin rechaza una solicitud pendiente del empleado
- **THEN** el empleado recibe el aviso de rechazo por los canales habilitados

#### Scenario: El admin cancela una reserva aprobada del empleado

- **GIVEN** un empleado con una reserva aprobada
- **WHEN** un admin la cancela
- **THEN** el empleado afectado recibe un aviso de que su reserva ha sido cancelada, por los canales habilitados

#### Scenario: El empleado cancela su propia solicitud

- **WHEN** el propio empleado cancela su solicitud
- **THEN** el empleado NO recibe aviso de esa cancelación; se avisa a los administradores de que el recurso ha quedado libre

### Requirement: Aviso al admin de solicitudes pendientes en modo manual

El sistema SHALL notificar a **todos los administradores activos** cuando se crea una solicitud con `approvalMode = MANUAL` (queda pendiente de aprobación). En `approvalMode = AUTOMATIC` el sistema NO SHALL avisar al admin (la solicitud nace aprobada). El empleado creador NO SHALL recibir un aviso adicional por este evento más allá del feedback existente.

#### Scenario: Nueva solicitud en modo manual

- **GIVEN** `approvalMode = MANUAL`
- **WHEN** un empleado crea una solicitud
- **THEN** todos los administradores activos reciben un aviso de "solicitud pendiente de aprobación" por los canales habilitados

#### Scenario: Nueva solicitud en modo automático

- **GIVEN** `approvalMode = AUTOMATIC`
- **WHEN** un empleado crea una solicitud (nace aprobada)
- **THEN** no se avisa a ningún administrador; el empleado recibe su confirmación
