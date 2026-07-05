# requests Specification (delta)

## ADDED Requirements
### Requirement: Resolución de solicitudes según el tipo de recurso en la UI admin
**La bandeja de solicitudes pendientes DEBE (MUST) mostrar el tipo de recurso (plaza o puesto) de cada solicitud, y el modal de aprobación DEBE (MUST) adaptarse a ese tipo, ofreciendo la lista de recursos disponibles del tipo correcto para la fecha solicitada.**

#### Scenario: Aprobar una solicitud de puesto
- **GIVEN** un `ADMIN` con una solicitud pendiente de tipo `DESK`
- **WHEN** abre el modal de aprobación
- **THEN** el modal indica que es una solicitud de puesto y ofrece los **puestos disponibles** para la fecha (no plazas)
- **AND** al confirmar, asigna el puesto elegido y la solicitud queda `APPROVED`

#### Scenario: Aprobar una solicitud de plaza
- **GIVEN** un `ADMIN` con una solicitud pendiente de tipo `PARKING`
- **WHEN** abre el modal de aprobación
- **THEN** el modal ofrece las **plazas disponibles** para la fecha
- **AND** al confirmar, asigna la plaza y la solicitud queda `APPROVED`

#### Scenario: El tipo es visible en los listados
- **GIVEN** solicitudes de plaza y de puesto
- **WHEN** el admin ve la bandeja de pendientes (o el empleado ve "Mis solicitudes")
- **THEN** cada fila muestra si la solicitud es de plaza o de puesto
