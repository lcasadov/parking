# auth-screens Specification

## Purpose
TBD - created by archiving change redesign-remaining-screens. Update Purpose after archive.
## Requirements
### Requirement: Pantallas de autenticación y preferencias con identidad ALEATICA
Las pantallas de cambio de contraseña, preferencias y sesión expirada DEBEN (MUST) seguir la
identidad ALEATICA con los patrones AuthCard/Field/Modal, sin cambiar la lógica de auth.

#### Scenario: Cambiar contraseña
- **WHEN** el usuario debe cambiar su contraseña
- **THEN** se presenta la tarjeta con el checklist de política usando el patrón Field
- **AND** la validación y el guardado usan la lógica existente

#### Scenario: Preferencias
- **WHEN** el usuario abre el menú del avatar
- **THEN** el popover muestra identidad, conmutador ES/EN, conmutador claro/oscuro y logout
  con los estilos del design system

#### Scenario: Sesión expirada
- **WHEN** ocurre un 401 tras estar autenticado
- **THEN** se muestra el modal de sesión expirada con el shell de diálogo común

