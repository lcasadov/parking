## MODIFIED Requirements

### Requirement: Header con identidad visual del design-system
**El header global DEBE (MUST) reproducir la identidad de los mockups: curva decorativa verde/naranja, logo, marca "Nexo / ALEATICA" (producto sobre empresa), título de la página y avatar del usuario, conservando los controles de idioma, tema, exportación y cierre de sesión.**

#### Scenario: Header en una pantalla de administración
- **GIVEN** un usuario autenticado en cualquier pantalla con chrome
- **WHEN** se renderiza el header
- **THEN** muestra la curva decorativa, el logo, la marca, el título de la pantalla y el avatar con las iniciales del usuario
- **AND** los controles de idioma, tema, exportación y cierre de sesión siguen accesibles
