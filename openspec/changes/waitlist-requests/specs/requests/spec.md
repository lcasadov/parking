## MODIFIED Requirements

### Requirement: Creación de solicitud con opt-in de lista de espera
El endpoint `POST /requests` DEBE (MUST) aceptar un campo opcional `waitlist` (booleano, por defecto `false`). Su presencia solo altera el comportamiento cuando **no hay disponibilidad** en modo AUTOMÁTICO: con `waitlist: true` la solicitud se crea `PENDING` (`waitlisted = true`) en lugar de responder `409 NO_AVAILABILITY`. Con hueco disponible, el campo se ignora (la solicitud se auto-asigna). El contrato previo se mantiene para todo lo demás (ventana de fecha, duplicados, selección de recurso).

#### Scenario: El 409 NO_AVAILABILITY deja de ser terminal con opt-in
- **GIVEN** modo AUTOMÁTICO sin hueco para (F, T)
- **WHEN** el cliente reenvía `POST /requests` con `waitlist: true` tras recibir un `409 NO_AVAILABILITY`
- **THEN** el sistema crea la solicitud en espera y responde con éxito

#### Scenario: Solicitud duplicada sigue rechazándose
- **GIVEN** el empleado ya tiene una solicitud `PENDING` (en espera o no) para (F, T)
- **WHEN** envía otra `POST /requests` para (F, T), con o sin `waitlist`
- **THEN** el sistema responde `409` de solicitud duplicada
- **AND** no crea una segunda solicitud

### Requirement: Representación de la solicitud incluye el estado de espera
`RequestResponse` DEBE (MUST) exponer el campo `waitlisted` (booleano) para que la UI distinga una solicitud pendiente normal de una en lista de espera.

#### Scenario: La solicitud en espera se expone como tal
- **GIVEN** una solicitud `PENDING` con `waitlisted = true`
- **WHEN** el empleado consulta sus solicitudes
- **THEN** la solicitud incluye `waitlisted: true` en la respuesta
