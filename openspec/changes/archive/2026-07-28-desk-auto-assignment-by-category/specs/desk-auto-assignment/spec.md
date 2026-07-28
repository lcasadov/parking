## ADDED Requirements

### Requirement: Auto-asignación de puesto según el rango del empleado
El sistema DEBE (MUST) asignar automáticamente un puesto libre a un empleado que solicita un `DESK` sin elegir uno, usando la misma frontera de rango que la auto-asignación de plazas (`HIGH_CATEGORIES = {CEO, CONSEJO, DIRECTOR_N1, DIRECTOR_N2}`):
- Un empleado de categoría **alta** DEBE (MUST) recibir un puesto `EXECUTIVE` libre si lo hay; si no, DEBE (MUST) recibir un `STANDARD` libre.
- Un empleado que **no** es de categoría alta DEBE (MUST) recibir únicamente un puesto `STANDARD` libre, y NO DEBE (MUST NOT) recibir nunca un `EXECUTIVE`.
- Entre los puestos candidatos libres, la elección DEBE (MUST) ser determinista (orden estable por número de puesto).

#### Scenario: Un alto recibe un puesto EXECUTIVE cuando hay libre
- **GIVEN** un empleado de categoría alta y al menos un puesto `EXECUTIVE` libre para la fecha
- **WHEN** solicita un `DESK` sin elegir puesto
- **THEN** el sistema le asigna un puesto `EXECUTIVE` libre

#### Scenario: Un alto cae a STANDARD si no hay EXECUTIVE
- **GIVEN** un empleado de categoría alta, sin `EXECUTIVE` libre pero con `STANDARD` libre para la fecha
- **WHEN** solicita un `DESK` sin elegir puesto
- **THEN** el sistema le asigna un puesto `STANDARD` libre

#### Scenario: Un no-alto nunca recibe EXECUTIVE
- **GIVEN** un empleado que no es de categoría alta, sin `STANDARD` libre pero con `EXECUTIVE` libre para la fecha
- **WHEN** solicita un `DESK` sin elegir puesto
- **THEN** el sistema NO le asigna el `EXECUTIVE`
- **AND** la solicitud queda pendiente de aprobación

### Requirement: Sin puesto válido libre, la solicitud queda pendiente aunque el modo sea automático
Cuando no hay ningún puesto libre válido para la categoría del solicitante y el modo de aprobación es `AUTOMATIC`, el sistema DEBE (MUST) crear la solicitud en estado `PENDING` (a resolver por el ADMIN) en lugar de responder `409` o exigir la elección de un puesto. Esto aplica solo cuando el empleado NO ha elegido puesto (`resourceId` ausente).

#### Scenario: Automático sin puesto válido → pendiente
- **GIVEN** modo `AUTOMATIC` y ningún puesto válido libre para la categoría del empleado en la fecha
- **WHEN** el empleado solicita un `DESK` sin elegir puesto
- **THEN** el sistema crea la solicitud `PENDING`
- **AND** no responde `409` ni exige elegir puesto

### Requirement: La promoción de lista de espera de puestos respeta la categoría
Al promover una solicitud de puesto en espera (capability `request-waitlist`), el sistema DEBE (MUST) aplicar la misma regla de categoría: un empleado no-alto NO DEBE (MUST NOT) ser promovido a un puesto `EXECUTIVE`.

#### Scenario: No se promueve a un no-alto a un EXECUTIVE
- **GIVEN** un puesto `EXECUTIVE` recién liberado y, en la cola de espera, solo empleados no-altos
- **WHEN** el sistema atiende la promoción de la cola de puestos
- **THEN** no asigna el `EXECUTIVE` a ningún empleado no-alto
