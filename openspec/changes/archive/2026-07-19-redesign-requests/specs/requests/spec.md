# requests

## ADDED Requirements

### Requirement: Presentación de solicitudes
La bandeja de solicitudes DEBE (MUST) seguir la identidad ALEATICA y permitir filtrar y resolver
peticiones sin cambiar las mutaciones existentes.

#### Scenario: Filtrar por estado
- **WHEN** el administrador selecciona una pestaña (Pendientes/Aprobadas/Rechazadas/Todas)
- **THEN** la tabla muestra las solicitudes de ese estado
- **AND** el contador de Pendientes refleja el número real

#### Scenario: Resolver una solicitud
- **WHEN** el administrador pulsa Aprobar o Rechazar en una fila
- **THEN** se ejecuta la mutación existente correspondiente
- **AND** la UI usa avatares de color, tipografía y tokens de marca
