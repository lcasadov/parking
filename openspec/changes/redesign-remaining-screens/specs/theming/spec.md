# theming

## ADDED Requirements

### Requirement: Tema claro y oscuro
El sistema DEBE ofrecer una variante oscura de todos los tokens, conmutable manualmente
desde el menú de usuario, aplicable a todas las pantallas.

#### Scenario: Conmutar a oscuro
- **WHEN** el usuario activa el tema oscuro en Preferencias
- **THEN** todas las pantallas adoptan la paleta oscura vía la clase de tema
- **AND** tablas, pills, modales y plano mantienen contraste y legibilidad

#### Scenario: Persistencia
- **WHEN** el usuario vuelve a entrar
- **THEN** se recuerda su preferencia de tema
