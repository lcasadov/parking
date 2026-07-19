# design-system

## ADDED Requirements

### Requirement: Tokens de marca
El sistema DEBE exponer los colores de marca ALEATICA y los neutros como variables CSS
globales, disponibles para todas las pantallas.

#### Scenario: Variables disponibles
- **WHEN** cualquier componente del frontend se renderiza
- **THEN** las variables `--brand-green`, `--brand-blue`, `--brand-yellow`,
  `--brand-orange`, `--taupe` y sus variantes soft/deep están definidas
- **AND** los neutros `--bg`, `--panel`, `--ink`, `--line` están definidos

### Requirement: Tipografía de marca
El sistema DEBE usar Cormorant Garamond para títulos y numerales y Mulish para UI/cuerpo.

#### Scenario: Fuentes cargadas
- **WHEN** la aplicación arranca
- **THEN** ambas familias están cargadas y aplicadas según la escala tipográfica del contract

### Requirement: Estado de plaza a color
El sistema DEBE definir un mapa único estado→color reutilizable por todas las vistas.

#### Scenario: Mapa consistente
- **WHEN** una vista representa el estado de una plaza o puesto
- **THEN** usa los tokens ocupado(verde)/liberado(azul)/pendiente(amarillo)/solicitud(naranja)/libre(dashed)

### Requirement: Layout de shell
El sistema DEBE proveer un shell con sidebar de navegación (252px), header de página
y tarjeta de usuario, reutilizable por todas las rutas autenticadas.

#### Scenario: Navegación activa
- **WHEN** el usuario está en una ruta
- **THEN** el item correspondiente del sidebar se marca como activo (border-left 3px + fondo accent-soft)
