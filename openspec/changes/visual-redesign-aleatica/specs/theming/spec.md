## MODIFIED Requirements

### Requirement: Tema claro y oscuro
El sistema DEBE (MUST) ofrecer una variante oscura **dark-premium** (fondo `#0c1411`, acento esmeralda `#33d68a`) de todos los tokens del mockup aprobado, conmutable manualmente mediante un **selector segmentado sol/luna** ubicado en el sidebar (escritorio) o el drawer (móvil), aplicable a todas las pantallas.

#### Scenario: Conmutar a oscuro con el selector sol/luna
- **GIVEN** el usuario ve el selector segmentado de tema (iconos sol/luna) en el sidebar o el drawer
- **WHEN** pulsa el icono de luna
- **THEN** todas las pantallas adoptan la paleta dark-premium vía `body.theme-dark`
- **AND** el botón de luna queda marcado como activo (`aria-pressed="true"`) y el de sol como inactivo
- **AND** tablas, pills, modales y plano mantienen contraste y legibilidad (colores dark recalculados, no una simple inversión)

#### Scenario: Persistencia
- **WHEN** el usuario vuelve a entrar
- **THEN** se recuerda su preferencia de tema (persistida por `ThemeProvider`, sin cambios de lógica respecto a la iteración anterior)
