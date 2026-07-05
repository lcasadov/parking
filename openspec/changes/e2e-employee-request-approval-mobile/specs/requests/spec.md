# requests Specification (delta)

## ADDED Requirements
### Requirement: Cobertura E2E de los flujos de solicitud y resolución
**Los flujos de solicitud de recurso por el empleado y de resolución por el administrador DEBEN (MUST) estar cubiertos por tests end-to-end (Playwright) que ejerciten la aplicación real (SPA + backend), incluyendo la vista de plano en viewport móvil.**

#### Scenario: E2E — empleado solicita plaza y puesto
- **GIVEN** un `EMPLOYEE` autenticado en la aplicación
- **WHEN** solicita una plaza y un puesto para la misma fecha
- **THEN** el test verifica que ambas solicitudes quedan registradas como pendientes del empleado

#### Scenario: E2E — admin aprueba y rechaza
- **GIVEN** un `ADMIN` autenticado con solicitudes pendientes
- **WHEN** aprueba una solicitud (asignando recurso) y rechaza otra
- **THEN** el test verifica que la aprobada queda `APPROVED` con recurso y la rechazada `REJECTED`

#### Scenario: E2E — solicitud desde el plano en móvil
- **GIVEN** un `EMPLOYEE` en viewport móvil con puestos libres para la fecha
- **WHEN** pulsa "Solicitar" en la lista "Disponibles para solicitar" del plano
- **THEN** el test verifica el feedback de éxito y la creación de la solicitud del puesto
