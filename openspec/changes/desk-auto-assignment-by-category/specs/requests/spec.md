## MODIFIED Requirements

### Requirement: Auto-aprobación de solicitud de puesto en modo AUTOMATIC
En modo `AUTOMATIC`, una solicitud de `DESK` DEBE (MUST) resolverse sin intervención del ADMIN según haya o no elección explícita de puesto:
- **Con `resourceId`** (puesto elegido, p. ej. desde el asistente): el sistema valida la disponibilidad del puesto concreto y, si está libre, aprueba la solicitud con ese puesto; si no, responde `409`.
- **Sin `resourceId`** (reserva rápida del empleado): el sistema **auto-asigna** un puesto según el rango del empleado (capability `desk-auto-assignment`); si no hay puesto válido libre, crea la solicitud `PENDING`. NO DEBE (MUST NOT) exigir la elección de un puesto (`RESOURCE_SELECTION_REQUIRED`) en este caso.

#### Scenario: Puesto elegido y libre se aprueba al instante
- **GIVEN** modo `AUTOMATIC` y un `resourceId` de puesto libre para la fecha
- **WHEN** el empleado envía la solicitud de `DESK` con ese `resourceId`
- **THEN** la solicitud nace `APPROVED` con ese puesto

#### Scenario: Sin elegir puesto, se auto-asigna por categoría
- **GIVEN** modo `AUTOMATIC` y un puesto válido libre para la categoría del empleado
- **WHEN** el empleado envía la solicitud de `DESK` sin `resourceId`
- **THEN** la solicitud nace `APPROVED` con un puesto auto-asignado acorde a su categoría

#### Scenario: Sin elegir puesto y sin hueco válido, queda pendiente
- **GIVEN** modo `AUTOMATIC` y ningún puesto válido libre para la categoría del empleado
- **WHEN** el empleado envía la solicitud de `DESK` sin `resourceId`
- **THEN** la solicitud nace `PENDING` (no `409`, no `RESOURCE_SELECTION_REQUIRED`)
