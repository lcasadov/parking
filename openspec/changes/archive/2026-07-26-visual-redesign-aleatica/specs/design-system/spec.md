## MODIFIED Requirements

### Requirement: Tokens de marca
El sistema DEBE (MUST) exponer los colores de la paleta **dark-premium esmeralda** del mockup aprobado como variables CSS globales, disponibles para todas las pantallas. Los nombres de token heredados de la paleta institucional original (`--brand-green`, `--bg-page`, `--state-occupied-bg`, etc.) DEBEN (MUST) conservarse como **alias** hacia los valores nuevos, de modo que ningún componente existente necesite reescribirse para adoptar la paleta.

#### Scenario: Variables de acento y neutros disponibles
- **WHEN** cualquier componente del frontend se renderiza
- **THEN** `--accent` (`#0f9e68` claro / `#33d68a` oscuro) y los semánticos `--ok`/`--busy`/`--rel`/`--pend`/`--info` están definidos
- **AND** los neutros `--bg`, `--panel`, `--panel-2`, `--ink`, `--ink-soft`, `--ink-faint`, `--line` reflejan la paleta dark-premium (`--bg: #0c1411` en `body.theme-dark`)
- **AND** los alias heredados (`--brand-green`, `--bg-page`, `--text`, `--green`, `--state-occupied-bg`…) resuelven a los mismos valores que sus tokens nuevos equivalentes

#### Scenario: Radios, sombras y espaciado del mockup
- **WHEN** se renderiza una tarjeta, un botón o un pill
- **THEN** usa `--radius-panel` (20px), `--radius-md` (11px, controles) o `--radius-pill` (pills) según corresponda
- **AND** las tarjetas usan sombra en capas (`--shadow-card`/`--shadow-panel`), nunca una sombra plana única

### Requirement: Tipografía de marca
El sistema DEBE (MUST) usar **Geist Variable** (auto-alojada vía `@fontsource-variable/geist`) para cuerpo, UI y titulares, y **Geist Mono Variable** (`@fontsource-variable/geist-mono`) para numerales/KPI tabulares. Ninguna fuente DEBE (MUST NOT) cargarse desde un CDN externo (p. ej. Google Fonts).

#### Scenario: Fuentes cargadas sin CDN
- **WHEN** la aplicación arranca
- **THEN** Geist Variable y Geist Mono Variable están empaquetadas por Vite (import en `main.tsx`) y aplicadas vía `--font-sans`/`--font-display`/`--font-mono`
- **AND** `index.html` no referencia ninguna hoja de estilos de Google Fonts

#### Scenario: Numerales tabulares en KPIs
- **WHEN** se muestra un valor numérico de KPI (p. ej. en el Dashboard)
- **THEN** usa `--font-mono` con variantes tabulares, de modo que las cifras se alinean en columna

## ADDED Requirements

### Requirement: Tokens de motion, elevación y cristal
El sistema DEBE (MUST) exponer tokens de duración/curva de easing (`--dur-instant/fast/base/slow`, `--ease-out/in/standard/spring`), un factor de pulsado táctil (`--press-scale`), elevación en capas (`--shadow-1..4`) y material translúcido (`--glass-bg`, `--glass-blur`) para el chrome fijo (header móvil, drawer). Cualquier animación DEBE (MUST) limitarse a `transform`/`opacity` y respetar `prefers-reduced-motion`.

#### Scenario: Animación respeta reduced-motion
- **GIVEN** un usuario con `prefers-reduced-motion: reduce` activado en el sistema
- **WHEN** interactúa con un componente animado (drawer, botón, tooltip)
- **THEN** la transición se omite o se reduce a duración cero, sin desplazamientos ni rebotes

#### Scenario: Safe-areas de iOS respetadas
- **WHEN** la aplicación se abre en un dispositivo iOS con notch/home-indicator
- **THEN** el chrome fijo (header/drawer) usa `--safe-top/bottom/left/right` (`env(safe-area-inset-*)`) para no invadir esas zonas
