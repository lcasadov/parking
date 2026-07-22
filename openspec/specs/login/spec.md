# login Specification

## Purpose
TBD - created by archiving change redesign-login. Update Purpose after archive.
## Requirements
### Requirement: Presentación del login
La pantalla de login DEBE (MUST) seguir la identidad ALEATICA definida en el design system,
sin alterar el flujo de autenticación existente.

#### Scenario: Acceso correcto
- **WHEN** el usuario introduce credenciales válidas y pulsa Entrar
- **THEN** se ejecuta el mismo flujo de autenticación que antes del rediseño
- **AND** la UI usa logo, tokens y tipografía de marca

#### Scenario: Error de credenciales
- **WHEN** las credenciales son inválidas
- **THEN** se muestra un banner de error con los intentos restantes
- **AND** tras 5 intentos fallidos se comunica el bloqueo temporal

