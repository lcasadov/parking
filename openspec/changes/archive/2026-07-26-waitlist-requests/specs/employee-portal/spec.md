## ADDED Requirements

### Requirement: Comunicación honesta de la falta de hueco y de la lista de espera
El portal del empleado DEBE (MUST) comunicar con claridad que, aunque un día aparezca sin recursos libres, puede solicitarlo igualmente porque los recursos se liberan con frecuencia (ausencias, vacaciones, cancelaciones). Cuando la disponibilidad del día/tipo es 0, el modal de reserva DEBE (MUST) explicar la situación y ofrecer **apuntarse a la lista de espera** en lugar de bloquear la acción; tras un `409 NO_AVAILABILITY` en modo AUTOMÁTICO, DEBE (MUST) permitir reintentar con `waitlist: true`.

#### Scenario: Sin hueco, el modal ofrece apuntarse en vez de bloquear
- **GIVEN** un empleado en el modal de reserva y un día/tipo con 0 disponibilidad
- **WHEN** ve la disponibilidad del día
- **THEN** el modal muestra un aviso honesto (los recursos se liberan a menudo) y ofrece "apuntarme a la lista de espera"
- **AND** no impide enviar la solicitud

#### Scenario: Reintento como lista de espera tras 409
- **GIVEN** modo AUTOMÁTICO y un envío que devolvió `409 NO_AVAILABILITY`
- **WHEN** el empleado confirma apuntarse a la lista de espera
- **THEN** la UI reenvía la solicitud con `waitlist: true` y muestra el resultado en espera

### Requirement: Estado "en lista de espera" visible en Mi Semana y Mis solicitudes
El portal DEBE (MUST) mostrar un distintivo "En lista de espera" para las solicitudes `PENDING` con `waitlisted = true`, tanto en "Mi Semana" (héroe HOY/MAÑANA y tira semanal) como en "Mis solicitudes". NO DEBE (MUST NOT) mostrar una posición numérica en la cola. `MyWeekDay` DEBE (MUST) exponer el estado de espera del recurso para poder pintarlo.

#### Scenario: Chip de espera en Mis solicitudes
- **GIVEN** una solicitud `PENDING` con `waitlisted = true`
- **WHEN** el empleado abre "Mis solicitudes"
- **THEN** esa fila muestra el distintivo "En lista de espera"
- **AND** no muestra ninguna posición numérica

#### Scenario: Chip de espera en Mi Semana
- **GIVEN** un día del héroe HOY/MAÑANA cuyo recurso está en lista de espera
- **WHEN** el empleado ve la tarjeta de ese día
- **THEN** el recurso muestra el estado "En lista de espera"
