# auth-screens Specification

## Purpose
TBD - created by archiving change redesign-remaining-screens. Update Purpose after archive.
## Requirements
### Requirement: Pantallas de autenticación y preferencias con identidad ALEATICA
Las pantallas de cambio de contraseña, preferencias y sesión expirada DEBEN (MUST) seguir la identidad ALEATICA, sin cambiar la lógica de auth. La pantalla de cambio de contraseña DEBE (MUST) reutilizar el mismo `AuthShell` que el login (composición centrada de marca: fondo con orbes, spotlight, logo flotante, titular y subtítulo de marca), presentando su encabezado ("Cambiar contraseña") como cabecera **dentro** de la card en lugar de en una cabecera de card separada.

#### Scenario: Cambiar contraseña
- **GIVEN** un usuario que debe cambiar su contraseña
- **WHEN** accede a la pantalla de cambio de contraseña
- **THEN** se presenta sobre la misma composición de marca (`AuthShell`) que el login, con "Cambiar contraseña" como encabezado dentro de la card y el checklist de política usando el patrón Field
- **AND** la validación y el guardado usan la lógica existente

#### Scenario: Preferencias
- **WHEN** el usuario abre el menú del avatar
- **THEN** el popover muestra identidad, conmutador ES/EN, conmutador claro/oscuro y logout
  con los estilos del design system

#### Scenario: Sesión expirada
- **WHEN** ocurre un 401 tras estar autenticado
- **THEN** se muestra el modal de sesión expirada con el shell de diálogo común

