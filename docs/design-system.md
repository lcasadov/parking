# Sistema de diseño — parking (ALEATICA)

> **Autoridad de diseño.** Lo consumen `frontend-bootstrap` y `frontend-engineer`
> para implementar componentes consistentes sin reinventar tokens en cada pantalla.
> **Fuente única de verdad de los tokens**: si en el código aparece un color hex
> distinto del declarado aquí, el código está mal. La implementación de referencia
> es [`docs/mockups/styles.css`](mockups/styles.css) (25 mockups en `docs/mockups/`).
>
> **Stack visual**: **CSS propio** (variables CSS) + **Tabler Icons** (webfont).
> **NO** se usa Tailwind, MUI ni shadcn/ui. Estado del frontend: React 18 + Vite 5.

---

# RESTYLE 2026 — Lenguaje visual PREMIUM (norte: Stripe / Apple)

> **Autoridad vigente del lenguaje visual.** Esta sección **prevalece** sobre la
> tipografía y la paleta descritas más abajo (§2–§3, redacción ALEATICA original).
> Objetivo: acabado "caro y profesional" — neutro cálido, tipografía muy cuidada,
> profundidad sutil en capas, acento restringido, micro-motion pulido. Fuente única
> de verdad de tokens: [`frontend/src/styles/tokens.css`](../frontend/src/styles/tokens.css).

## R.1 Tipografía — **Geist** (auto-alojada, sin CDN)

- **Familia única**: **Geist Variable** (Vercel) para cuerpo, UI **y** titulares —
  grotesca contemporánea con carácter técnico-premium. Sustituye a la serif editorial
  Cormorant Garamond (desentonaba con el norte) y a Mulish.
- **Numerales / KPI**: **Geist Mono Variable**, cifras **tabulares** (`tnum`, `zero`)
  — sabor técnico/financiero Stripe, columnas perfectamente alineadas.
- **Auto-alojamiento**: `@fontsource-variable/geist` + `@fontsource-variable/geist-mono`
  importados en `src/main.tsx` (npm, woff2 empaquetados por Vite; **jamás** `<link>` a
  Google Fonts). Tokens: `--font-sans`, `--font-display`, `--font-mono` (alias
  `--font-serif` → display, por retrocompatibilidad).
- **Tracking óptico** (clave del look): titulares con tracking **negativo**
  (`--tracking-display: -0.022em`), cuerpo casi neutro (`-0.006em`), labels uppercase
  con tracking amplio (`--tracking-label: 0.14em`). Leading ceñido en display.
- **Pesos**: cuerpo 400, énfasis 500, titulares 600. Botones **550–560** (se abandona
  el 700/800 anterior). `font-optical-sizing: auto`.
- **Escala**: `--fs-h1 40px`, `--fs-h2 27px`, `--fs-body 15px`, `--fs-num 32/40px`.

## R.2 Color — acento **restringido** + neutro cálido

**Acento (único acento fuerte de la app):** el verde ALEATICA se reinterpreta como
**verde pino profundo desaturado** — mismo hue de marca, sobriedad Stripe/Linear.

| Token | Light | Dark | Uso |
|---|---|---|---|
| `--brand-green` | `#1a7548` | `#34a86a` | Primario: botones, nav activo, foco |
| `--brand-green-hover` | `#155f3a` | `#3cb976` | Hover del primario |
| `--brand-green-active` | `#0f4730` | `#2c8f5a` | Pulsado del primario |
| `--brand-green-deep` | `#124e30` | `#a7e0bd` | Tinta de acento (eyebrow, numerales) |
| `--brand-green-soft` | `#e8efe9` | `rgba(119,184,41,.22)` | Fondo suave (nav activo / ocupado) |

**Neutros cálidos** (Stripe/Apple): `--bg #f4f3ef` · `--panel #ffffff` ·
`--panel-2 #faf9f5` · `--ink #1b1a17` · `--ink-soft #5f5c56` · `--ink-faint #918d84`.
Hairlines discretas: `--line rgba(27,26,23,.10)`. Dark: neutros cálidos oscuros
(no negro puro): `--bg #1c1c19` · `--panel #26261f` · `--ink #e9e7df`.

**Estado (desaturado, mismo hue):** ocupado=verde · liberado=`--brand-blue #1f88bd`
(cian calmado) · pendiente=`--brand-yellow #e8c34a` (oro suave, se abandona el lima
neón) · solicitud=`--brand-orange #e08a1e` (ámbar contenido). Destructivo:
`--red #c1392f` (contenido, se abandona `#e24b4a`).

## R.3 Superficies y profundidad

- **Sombras suaves EN CAPAS** (nunca planas ni duras): `--shadow-card` combina
  contacto fino + halo ambiental amplio; `--shadow-card-hover` levanta un punto.
  Cards, tablas, `auth-card` y KPI usan `--shadow-card`.
- **Radios**: controles `--radius-md 8px`, tarjetas/paneles `--radius-panel 14px`,
  pills `--radius-pill 20px`.

## R.4 Componentes núcleo (antes → después)

- **Tablas**: cabecera negra (`background: --ink`) **→** cabecera Stripe **quieta**
  (fondo `--panel-2`, etiqueta versalita 11px `letter-spacing .07em`, tinta
  `--ink-faint`, hairline inferior). Filas con separador hairline + hover cálido sutil,
  padding `13px 18px`, card con `--shadow-card`.
- **Botones**: saturación bootstrap + peso 800 **→** primario verde pino con doble
  sombra en capas + filete interior superior claro ("atrapa la luz"), peso 560;
  secundario **ghost** (panel + hairline); destructivo rojo contenido. Se conserva el
  pulsado táctil (`whileTap` + `--press-scale`), afinado (hover oscurece el tono, sin
  `filter: brightness`; active con sombra interior).
- **Modales**: barra de cabecera de color saturado **→** cabecera **quieta** (título
  Geist sobre `--panel-2` + hairline) con **filete de acento superior** + icono teñido
  según semántica (`green`/`red`/`amber`); overlay con `backdrop-filter: blur(3px)` y
  scrim cálido; superficie con `--shadow-4` y radio `--radius-panel`.
- **Pills**: peso 700 **→** 600, tracking ceñido.

## R.5 Motion

Sin cambios de arquitectura: se reutilizan los tokens `--dur-*` / `--ease-*` y los
resortes de `theme/motion.ts` (Ola A). Regla vigente: animar solo `transform`/`opacity`,
UI < 300 ms, entradas `ease-out`, resortes sin rebote salvo momentum; todo respeta
`prefers-reduced-motion`.

---

## 1. Identidad de marca

- **Familia QRIA (ALEATICA)**: conjunto de aplicaciones corporativas. `parking` es una de ellas.
- **Logo**: círculo con gradiente cónico verde → teal → azul:
  `conic-gradient(from 200deg, #97c459 0deg 140deg, #1d9e75 140deg 260deg, #378add 260deg 360deg)`. Clases `.logo-dot` (34px), `.phone-logo` (24px).
- **Header** con curva decorativa SVG (verde + naranja) anclada a la derecha (`.app-header .curve`, `.auth-head .curve`, `.phone-header .curve`).
- **Tono**: institucional sobrio, no juguetón. Pesos tipográficos 400/500 (nunca 700).

---

## 2. Tokens de color

Declarados como variables CSS en `:root`. El modo oscuro las **reasigna** en `body.theme-dark` (ver §10).

### 2.1 Paleta base (light)
| Token | Hex | Uso |
|---|---|---|
| `--bg-page` | `#f5f4ef` | Fondo del área de contenido (`.main`) |
| `--bg-card` | `#ffffff` | Fondo de cards y tablas |
| `--bg-soft` | `#fafaf7` | Fondo de cabeceras/footers suaves |
| `--border` | `rgba(0,0,0,0.12)` | Bordes finos (0.5px) |
| `--border-strong` | `rgba(0,0,0,0.2)` | Bordes principales |
| `--text` | `#444441` | Texto primario |
| `--text-muted` | `#888780` | Texto secundario |
| `--text-soft` | `#5f5e5a` | Texto en cells |
| `--highlight` | `#fcf6e6` | Fila de tabla destacada |

> Fondo exterior del viewport (fuera de las cards): `#e8e6df` (auth/phones) — color de marco, no es token.

### 2.2 Paleta corporativa
| Token | Hex | Uso |
|---|---|---|
| `--green` | `#639922` | Color de marca principal (sidebar activo, day-chip on) |
| `--green-dark` | `#3b6d11` | Hover/active de enlaces |
| `--green-soft` | `#eaf3de` | Fondos suaves verdes |
| `--green-border` | `#c0dd97` | Bordes suaves verdes |
| `--green-darker` | `#173404` | Texto sobre `green-soft` |
| `--teal` | `#1d9e75` | Acción secundaria (exportar, success) |
| `--blue` | `#378add` | Acción informativa / filtros |
| `--blue-soft` | `#e6f1fb` | Fondo informativo |
| `--blue-border` | `#b5d4f4` | Borde informativo |
| `--blue-darker` | `#042c53` | Texto sobre `blue-soft` |
| `--red` | `#e24b4a` | Acción destructiva |
| `--red-text` | `#a32d2d` | Texto de error |
| `--red-soft` | `#fcebeb` | Fondo de error |
| `--red-border` | `#f09595` | Borde de error |

> Verde de **foco** de formularios: `#97c459` (verde claro de marca; ver §6.11 y §9).

### 2.3 Paleta de estado (ámbar/rosa)
| Token | Hex | Uso |
|---|---|---|
| `--amber-soft` | `#faeeda` | Fondo PENDING / pdte. asignar |
| `--amber-border` | `#fac775` | Borde ámbar |
| `--amber-text` | `#633806` | Texto ámbar |
| `--pink-soft` | `#fbeaf0` | Fondo liberada / RECHAZADA |
| `--pink-border` | `#f4c0d1` | Borde rosa |
| `--pink-text` | `#4b1528` | Texto rosa |

### 2.4 Paleta semántica (estado de dominio → color)
| Estado de dominio | Background | Border | Text |
|---|---|---|---|
| `APPROVED` / asignada | `--green-soft` | `--green-border` | `--green-darker` |
| Release / liberada | `--pink-soft` | `--pink-border` | `--pink-text` |
| `PENDING` / pdte. asignar | `--amber-soft` | `--amber-border` | `--amber-text` |
| Request aprobada (esa fecha) | `--blue-soft` | `--blue-border` | `--blue-darker` |
| Libre (sin estado) | `#f1efe8` | `#d3d1c7` | `--text-soft` |
| `REJECTED` / error | `--red-soft` | `--red-border` | `--red-text` |
| `CANCELLED` / neutra | `#f1efe8` | `#d3d1c7` | `--text-soft` |

> Mapeo de **enums en inglés** (autoridad README) a color visible. Los textos de la UI vienen de i18n (§8), no del nombre del enum.

### 2.5 Paleta de avatares (decorativa, NO semántica)
| Clase | Background | Text |
|---|---|---|
| `.av-purple` | `#cecbf6` | `#26215c` |
| `.av-teal` | `#9fe1cb` | `#04342c` |
| `.av-pink` | `#f4c0d1` | `#4b1528` |
| `.av-amber` | `#fac775` | `#412402` |
| `.av-blue` | `#b5d4f4` | `#042c53` |
| `.av-coral` | `#f5c4b3` | `#712b13` |

Asignación estable por hash del identificador del empleado (no aleatoria por render).

---

## 3. Tipografía

- **Familia** (stack del sistema, sin Google Fonts en MVP):
  `-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen, Ubuntu, sans-serif`.
- **Escala** (extraída de los mockups):
  | Token | Tamaño | Uso |
  |---|---|---|
  | `text-xs` | 9–11px | Subtítulos de marca, badges, hints, `field-label` |
  | `text-sm` | 12–13px | Texto base de UI (filas, botones, inputs) |
  | `text-base` | 14px | Texto cuerpo |
  | `text-lg` | 15–16px | Encabezados de sección, título de modal |
  | `text-xl` | 18px | Título de página / brand-name |
  | `text-2xl` | 22px | Títulos H1 |
- **Pesos**: 400 (normal), 500 (énfasis). **Nunca 700**: el "bold" se sustituye por weight 500 + color contrastante.
- **Line height**: 1.4–1.6 según contexto.

---

## 4. Espaciado y radios

- **Escala de espaciado**: 4 · 8 · 12 · 16 · 20 · 24 · 32 px.
- **Border radius**:
  | Token | Valor | Uso |
  |---|---|---|
  | `radius-sm` | 4px | badges, pills internas, day-check |
  | `radius-md` | 6px | botones, inputs, tabs |
  | `radius-lg` | 8px | cards, tablas, modales |
  | `radius-xl` | 12px | auth-card, contenedores principales |
- **Sombras**: casi inexistentes. Patrón: borde fino `0.5px solid var(--border)`.
  - Modal: `box-shadow: 0 0 0 0.5px var(--border)`.
  - Popover: `box-shadow: 0 6px 20px rgba(0,0,0,0.14)`.
  - Phone frame: `box-shadow: 0 0 0 6px #2c2c2a`.

---

## 5. Iconografía

- **Librería**: **Tabler Icons** vía webfont `@tabler/icons-webfont`.
- **Clase**: `ti ti-<nombre>` (p. ej. `ti-edit`, `ti-check`, `ti-x`, `ti-calendar`, `ti-filter`, `ti-search`, `ti-chevron-down`).
- **Tamaños**: 14px (inline), 16px (botones), 18px (header).
- **Color**: hereda del contenedor; verde (`--green`) para el item activo del sidebar.
- Iconos **decorativos** → `aria-hidden="true"`; iconos **funcionales** (botón icon-only) → `aria-label` obligatorio (§9).

---

## 6. Componentes

Para cada uno: anatomía · estados · variantes · reglas de uso · ejemplo. Cada componente está respaldado por al menos un mockup de `docs/mockups/`.

### 6.1 Botones (`.btn`)
- **Anatomía**: altura 32px, `radius-md`, padding `0 14px`, `font-size 12px`, weight 500, icono opcional + texto (gap 6px).
- **Variantes**: `btn-green` (teal, acción positiva principal), `btn-blue` (informativa), `btn-red` (destructiva), `btn-white` (neutro/secundario, borde fino), `btn-success` (verde compacto in-row), `btn-danger-outline` (rojo perfilado in-row), `btn-icon-only` (32×32, solo icono), `btn-back` (transparente, volver).
- **Estados**: default · hover (oscurecer fondo o `green-soft` en neutros) · focused (outline §9) · disabled (opacidad 0.5, `cursor: not-allowed`).
- **Uso**: una sola acción **primaria** por vista. Destructivas siempre en rojo y con confirmación.
- **Ejemplo**: `<button class="btn btn-green"><i class="ti ti-check" aria-hidden="true"></i> Aprobar</button>`

### 6.2 Tablas (`.table`)
- Cabecera `.table-header` con fondo `#444441` y texto blanco; filas `.table-row` con borde inferior `0.5px`; fila destacada `.highlighted` (`--highlight`).
- Layout con **CSS grid** (los mockups no usan `<th>/<td>`), pero **el frontend final DEBE usar HTML semántico** (`<table>/<thead>/<tbody>` o roles ARIA de grid). Padding de celda `12px 14px`.
- **Uso**: listados con paginación servidor (TanStack Query). En móvil: scroll horizontal (`min-width: 560px`).

### 6.3 Pills / badges de estado (`.pill`)
| Clase | Estado de dominio |
|---|---|
| `pill-green` | `APPROVED` / asignada |
| `pill-amber` | `PENDING` |
| `pill-pink` | `REJECTED` / liberada |
| `pill-blue` | informativa |
| `pill-gray` | `CANCELLED` / neutra |
- `radius: 10px`, `font-size: 11px`, weight 500, borde `0.5px`.
- **Uso**: estado de una entidad en una fila. No usar como botón.

### 6.4 Cells de calendario (`.cell-*`)
`cell-assigned`, `cell-released`, `cell-pending`, `cell-request`, `cell-free` — fondo + texto según paleta semántica (§2.4). Para el calendario semanal del admin.

### 6.5 Day chips (`.day-chip`)
Botones 28×28 para días de la semana en la tabla de empleados. `on` (verde sólido, asignado) · `off` (gris perfilado, libre).

### 6.6 Day cards (`.day-card`)
Tarjetas de selección de día en el modal de empleado. Default: borde gris, fondo blanco. `selected`: borde verde 1.5px, fondo `--green-soft`, check verde.

### 6.7 Avatares (`.avatar`)
Tamaños: `avatar` (34px, header), `avatar-sm` (30px, tablas), `phone-avatar` (26px, móvil). Iniciales con paleta decorativa (§2.5).

### 6.8 Search box (`.search-box`)
Input con icono `ti-search` a la izquierda, ancho 240–280px, borde fino, `radius-md`. En móvil ancho completo.

### 6.9 Tabs (`.tab`)
Pill-style con borde fino. `active`: fondo blanco + texto oscuro. Inactivo: transparente + texto muted. Variante con contador `<span class="count">5</span>`. (Tabs internas de modal: `.modal-tab` con underline verde.)

### 6.10 Info banners (`.info-banner`)
Variantes `blue` (info neutra), `green` (confirmación), `red` (advertencia destructiva). **`amber` pendiente** (token existe, falta la regla CSS — ver §12).

### 6.11 Form fields
- `field-label` (verde, 11px); `field-label.red` (error).
- `field-value` (borde gris, padding 8×10, `radius-md`); modificadores: `readonly` (fondo `#f9f8f4`), `focused` (borde `#97c459`), `danger` (borde `--red-border`), `with-icon` (chevron a la derecha).
- `input.field-input` y `textarea` (mín. 64px, redimensionable vertical). Foco: borde `#97c459`.
- Layout: `field-row` (2 col), `field-row-1-4` (1.4fr/1fr); en móvil → 1 columna.
- `hint` (11px, muted) bajo el campo; checklist de política de contraseña `.policy` con estados `ok`/`bad` (§ auth).

### 6.12 Modales (`.modal`)
- Anchura 720px estándar, `narrow` 560px (confirmaciones).
- `modal-header` 48px, texto blanco, variante `green` (positivo) o `red` (destructivo). Tabs internas con underline verde. `modal-footer` con botones a la derecha, fondo `--bg-soft`. Overlay `rgba(0,0,0,0.45)`.
- A11y: `role="dialog"` + `aria-labelledby` apuntando al título; foco atrapado; cierre con `Esc`.

### 6.13 Sidebar (`.sidebar`)
200px, fondo blanco. Item activo: fondo `--green-soft`, borde izquierdo verde 3px, icono verde, weight 500. Badge rojo opcional `.badge-red` (contador). En móvil: barra horizontal con scroll (§7).

### 6.14 App header (`.app-header`)
64px, fondo blanco, curva SVG decorativa. Logo cónico + `brand-name` "parking" + `brand-sub` "ALEATICA". Título de página (oculto en móvil) y avatar a la derecha. Toggles de tema/idioma y menú de usuario (popover) en la cabecera.

### 6.15 Phone frame (`.phone`)
300px, `radius 24px`, marco oscuro (`box-shadow 0 0 0 6px #2c2c2a`). Header 54px con curva. `week-card` con borde izquierdo de color por estado (assigned/released/request/free). Representa la **vista de empleado en móvil**.

### 6.16 Controles de preferencias
- **Segmented** (`.segmented`): selector ES/EN o claro/oscuro; botón `active` con `green-soft`.
- **Toggle** (`.toggle`): switch booleano; `on` → track verde, knob a la derecha.
- **Popover** (`.popover`): menú de usuario/preferencias anclado bajo el header.

### 6.17 Pills de recurso y categoría

Componentes reales de `frontend/src/components/` que reutilizan la base `.pill` (§6.3) con semántica de dominio:

- **`ResourceTypePill`** (`ResourceTypePill.tsx`): distingue el tipo de recurso de una solicitud/asignación — **puesto** (`DESK` → `pill-blue`) vs **plaza** (`PARKING` → `pill-gray`). Default `PARKING` (retrocompatible). Texto vía i18n `requests.resourceType.*`.
- **`DeskCategoryBadge`** (`DeskCategoryBadge.tsx`): categoría del puesto — **Dirección** (`EXECUTIVE` → `pill-blue`) vs **Estándar** (`STANDARD` → `pill-gray`). Texto vía `desks.category.*`.

### 6.18 Plano interactivo (familia FloorPlan*)

Componentes de la vista de plano (`FloorPlanPage`), única para admin y empleado (la diferencia es por **rol**, no por breakpoint; ver [`ux-flows.md` §3](ux-flows.md)):

- **`FloorPlanMarker`** (`.floor-marker`): botón accesible posicionado por `%` (`left`/`top`) sobre la imagen. Fuera de edición pinta el **color semántico de estado** (`markerStateClass`); en edición se pinta neutro y es **arrastrable** (Pointer Events, ratón + táctil). `EXECUTIVE` se distingue con **anillo ámbar + ◆** (`--exec-ring`). Un puesto `FREE` es solicitable; el resto va `disabled`.
- **`FloorPlanSurface`** (`.floor-plan-surface` / `.plano-world`): superficie con **leyenda** de estados + viewport con **zoom/pan** (transform CSS) que contiene la imagen de planta y un marcador por puesto colocado; lista aparte los puestos sin posición (`.floor-unplaced`).
- **`FloorPlanZoom`** (`.plano-zoom`): controles acercar / alejar / restablecer; indicador de porcentaje (`aria-live`). Acotado por `ZOOM_MIN`/`ZOOM_MAX`.
- **`FloorPlanFilters`** (`.chip-filters` / `.chip-filter`): chips de filtro por estado (Libre, Liberado hoy, Mi puesto, Solicitado, Ocupado) con **contadores** + chip de **Dirección**; actúan como toggle (`aria-pressed`).
- **`FloorPlanDatebar`** (`.plano-datebar`): navegación día anterior / "Hoy" / día siguiente, fecha larga localizada (`date-fns`) y recordatorio de la ventana de reserva; acotada a hoy…hoy+14.
- **`FloorPlanMobileList`** (`.mlist`): lista "Disponibles para solicitar" con un botón "Solicitar" por puesto libre. Se renderiza para **cualquier empleado** (`!canEdit`), no por breakpoint; el admin **no** la ve.
- **`FloorPlanSidePanel`** (`.plano-side`): panel lateral (escritorio) con lista de puestos **buscable** por número; en la variante admin (`showStatus`) cada fila muestra la **pill de estado** del día.

### 6.19 Exportación (CSV/XLSX y RGPD)

- **`ExportMenu`** (`.export-menu`): grupo de botones "CSV" / "XLSX" que dispara la descarga del fichero binario; deshabilita mientras descarga y admite `requiredRole` (defensa en profundidad; el backend sigue siendo la autoridad). Textos `exports.*`.
- **`ExportMyDataButton`**: acción RGPD "Exportar mis datos" (derecho de acceso) — cualquier usuario autenticado descarga sus datos personales en XLSX. Vive en la cabecera; solo visible con sesión activa.

### 6.20 Badge de solicitudes pendientes

- **`PendingRequestsBadge`** (`.badge-red`): contador rojo de solicitudes `PENDING` mostrado junto al item "Solicitudes" del sidebar de admin. Query ligera (`size 1`, solo `totalElements`); se **oculta** si no hay pendientes.

### 6.21 Marca y contenedores de autenticación

- **`BrandCurve`** / **`BrandLogo`** (`BrandCurve.tsx`): la **curva decorativa SVG** corporativa (verde + naranja) del header, con variantes `header` (viewBox 340×64) y `auth` (viewBox 380×96); trazos copiados de `docs/mockups/shell.js` y colores tokenizados (`--green-border`, `--curve-orange`). Decorativa → `aria-hidden`. `BrandLogo` renderiza el punto cónico + "parking" + "ALEATICA".
- **`AuthShell`** (`.auth-wrap` / `.auth-card`): contenedor de las pantallas de autenticación (login / cambio de contraseña) — `auth-card` con cabecera de marca + curva y toggles de tema/idioma en el cuerpo.

### 6.22 Modal de sesión expirada

- **`SessionExpiredModal`**: modal (variante `amber`, no roja) suscrito al evento del interceptor `401`. Muestra el aviso de sesión caducada + banner de fase; al cerrar limpia la sesión y navega a `/login`. Base: `Modal` (§6.12) + `InfoBanner` (§6.10).

---

## 7. Layout y responsive

- **Desktop**: `app-header` 64px + (`sidebar` 200px + `main` flex). Contenedor recomendado: `max-width` ~1300px centrado (ver §12).
- **Breakpoint activo**: `@media (max-width: 768px)` (implementado en `styles.css`):
  - `layout` pasa a columna; `sidebar` → barra horizontal con scroll; `page-title` oculto.
  - Tablas: scroll horizontal (`min-width: 560px`).
  - `field-row`/modales → 1 columna / ancho completo; `auth-card` ancho completo.
- **Empleado**: las vistas `.phone` ya representan el móvil.
- Breakpoints de referencia: `sm` 640 · `md` 768 · `lg` 1024 · `xl` 1280.

---

## 8. Internacionalización

- Todos los textos visibles provienen de claves **i18n** (react-i18next). **Nunca** texto hardcodeado.
- Namespaces por dominio: `common.*`, `auth.*`, `employees.*`, `requests.*`, `calendar.*`, `visitors.*`, `parkingSpaces.*`, `desks.*`.
- Idioma por defecto **español**, **inglés** obligatorio. Preferencia persistida en `localStorage`.
- Fechas con `date-fns` + locale dinámico (`es`/`en`).

> Las **claves** i18n se nombran en inglés (convención de código); los **valores** son ES/EN.

---

## 9. Accesibilidad

- Contraste mínimo **WCAG AA** en todo texto sobre fondo (verificar también en dark, §10).
- **Focus visible**: `outline: 2px solid var(--green); outline-offset: 2px` — **implementado** en `frontend/src/styles/base.css` sobre `a/button/input/[tabindex]:focus-visible`.
- `label` asociado a cada input vía `htmlFor`.
- Modales: `role="dialog"` + `aria-labelledby`; foco atrapado.
- Iconos decorativos `aria-hidden="true"`; icon-only con `aria-label`.
- Errores de formulario anunciados con `role="alert"`.

---

## 10. Modo oscuro

- **Toggle manual** (no detección automática del SO).
- Implementación real: clase **`body.theme-dark`** (no `.dark` en `<html>`), que **reasigna** las variables de color. Persistir en `localStorage` (clave sugerida `parking.theme`).
- Overrides definidos en `styles.css` → `body.theme-dark`:
  | Token | Dark |
  |---|---|
  | `--bg-page` | `#1c1c19` |
  | `--bg-card` | `#26261f` |
  | `--bg-soft` | `#222220` |
  | `--border` | `rgba(255,255,255,0.14)` |
  | `--border-strong` | `rgba(255,255,255,0.24)` |
  | `--text` | `#e9e7df` |
  | `--text-muted` | `#9c9a90` |
  | `--text-soft` | `#c7c5bb` |
  | `--green-soft` | `rgba(99,153,34,0.22)` |
  | `--green-darker` | `#cfe6a8` |
  | `--blue-soft` | `rgba(55,138,221,0.22)` |
  | `--blue-darker` | `#bcdcff` |
  | `--amber-soft` | `rgba(250,199,117,0.20)` |
  | `--amber-text` | `#f0cf94` |
  | `--pink-soft` | `rgba(212,83,126,0.24)` |
  | `--pink-text` | `#f3b9cd` |
  | `--red-soft` | `rgba(226,75,74,0.20)` |
  | `--red-text` | `#f3a3a3` |
  | `--highlight` | `rgba(252,246,230,0.06)` |
- Fondo del `body` en dark: `#141413`. Los tokens corporativos sólidos (`--green`, `--teal`, `--blue`, `--red`) se mantienen.
- Todo componente debe verificarse en **ambos** modos antes de darse por terminado.

---

## 11. Convenciones de CSS propio (sustituye a Tailwind)

> Este proyecto **no usa Tailwind/PostCSS/MUI/shadcn**. El sistema visual son
> clases CSS semánticas + variables, replicando `docs/mockups/styles.css`.

- **Tokens**: variables CSS en `:root` (light) y `body.theme-dark` (dark). Un único punto de definición; ningún hex suelto en componentes.
- **Organización sugerida** en `frontend/src/styles/`:
  - `tokens.css` — variables (`:root` + `body.theme-dark`).
  - `base.css` — reset, `body`, tipografía.
  - `components/*.css` — un fichero por componente (`button.css`, `table.css`, `modal.css`, …) **o** CSS Modules por componente (`Button.module.css`) importando los tokens globales.
- **Nomenclatura**: clases **orientadas a componente** como en los mockups (`.btn`, `.btn-green`, `.pill`, `.pill-amber`, `.field-value.danger`). Evitar utilidades atómicas tipo Tailwind.
- **Consumo en React**: `className="btn btn-green"`. Sin CSS-in-JS. Para scoping local, CSS Modules; los **tokens** permanecen globales.
- **Iconos**: `import '@tabler/icons-webfont/dist/tabler-icons.min.css'` una vez; usar `<i class="ti ti-*">`.
- **Fechas**: `date-fns` con locale dinámico.

Ejemplo mínimo de `tokens.css`:
```css
:root{
  --bg-page:#f5f4ef; --bg-card:#ffffff; --text:#444441;
  --green:#639922; --green-soft:#eaf3de; --green-darker:#173404;
  --blue:#378add; --red:#e24b4a; --amber-soft:#faeeda; /* …§2 */
  --radius-sm:4px; --radius-md:6px; --radius-lg:8px; --radius-xl:12px;
}
body.theme-dark{ --bg-page:#1c1c19; --bg-card:#26261f; --text:#e9e7df; /* …§10 */ }
```

---

## 12. Pendientes

1. **`info-banner.amber`**: el token ámbar existe pero falta la regla CSS de la variante (Prompt 6 §6.10 la pedía "por simetría").
2. **Container principal**: definir `max-width` (~1300px) + margin auto para pantallas anchas (no presente en los mockups).
3. **Cableado i18n de `date-fns`** (selección de locale `es`/`en` en runtime).
4. **Drawer móvil** del sidebar (alternativa a la barra horizontal) si el número de items crece.

> **Ya resueltos** (antes en esta lista): el **focus ring de accesibilidad** está implementado en `frontend/src/styles/base.css` (§9); las **variables de espaciado y tipografía** (`--space-1…8`, `--text-xs…2xl`, `--radius-*`) están definidas en `frontend/src/styles/tokens.css` (§3, §4), así que los tamaños/paddings ya no viven inline.

---

# OLA A — Fundaciones + Shell (rediseño 2026)

> **Autoridad de esta sección.** Documenta las fundaciones del rediseño completo:
> tokens de motion/elevación, el **shell fijo** (header + menú), el **efecto de
> pulsado táctil** de los botones y el **catálogo de primitivas** (DS propio +
> **Radix UI** headless + **Framer Motion**). Los agentes de rollout de pantallas
> DEBEN construir sobre estas piezas y no reinventar overlays, menús ni animaciones.
>
> **Stack de la Ola A**: CSS propio (variables) + Tabler Icons + **Radix UI**
> (`@radix-ui/react-*`, primitivas headless accesibles) + **Framer Motion** (`framer-motion`).
> **Sigue prohibido** Material UI / Tailwind / shadcn. Se **preserva** el branding
> ALEATICA y el tema claro/oscuro existentes (esta ola los evoluciona, no los sustituye).

## A.1 Dependencias añadidas

| Paquete | Versión | Uso |
|---|---|---|
| `framer-motion` | `^11.18.2` | Pulsado táctil de botones, entrada de modales, drawer off-canvas |
| `@radix-ui/react-dialog` | `^1.1.21` | Primitiva `Dialog` |
| `@radix-ui/react-alert-dialog` | `^1.1.21` | Primitiva `ConfirmDialog` |
| `@radix-ui/react-dropdown-menu` | `^2.1.22` | Primitiva `Menu` |
| `@radix-ui/react-popover` | `^1.1.21` | (disponible para rollout) |
| `@radix-ui/react-tooltip` | `^1.2.14` | Primitiva `Tooltip` |
| `@radix-ui/react-select` | `^2.3.5` | Primitiva `SelectField` |
| `@radix-ui/react-tabs` | `^1.1.19` | (disponible para rollout; `Tabs` propio sigue vigente) |
| `@radix-ui/react-visually-hidden` | `^1.2.9` | Etiquetas accesibles ocultas |

## A.2 Tokens de MOTION (`styles/tokens.css` + `theme/motion.ts`)

Dos fuentes en sync: CSS (`--dur-*`, `--ease-*`, `--press-scale`, `--shadow-*`,
`--glass-*`, `--safe-*`) y JS (`theme/motion.ts`: `DUR`, `EASE`, `SPRING`,
`SPRING_PRESS`, `PRESS_SCALE`). **Regla de craft**: animar solo `transform`/`opacity`;
UI < 300 ms; entradas con `ease-out`; resortes sin rebote por defecto (rebote SOLO
cuando la interacción llevó momento). Todo respeta `prefers-reduced-motion`.

| Token CSS | Valor | JS equivalente | Uso |
|---|---|---|---|
| `--dur-instant` | `90ms` | `DUR.instant` | Feedback de pulsado / hover |
| `--dur-fast` | `140ms` | `DUR.fast` | Micro-transiciones de color/fondo, salidas |
| `--dur-base` | `200ms` | `DUR.base` | Entradas de popover/dropdown/overlay |
| `--dur-slow` | `280ms` | `DUR.slow` | Modal, drawer |
| `--ease-out` | `cubic-bezier(0.22,1,0.36,1)` | `EASE.out` | Entradas (respuesta inmediata) |
| `--ease-in` | `cubic-bezier(0.4,0,1,1)` | `EASE.in` | Salidas |
| `--ease-standard` | `cubic-bezier(0.4,0,0.2,1)` | `EASE.standard` | Transiciones neutras |
| `--press-scale` | `0.96` | `PRESS_SCALE` | Hundimiento del pulsado táctil |
| `SPRING_PRESS` | — | `{stiffness:620,damping:30,mass:0.7}` | Resorte del `whileTap` del botón |

### Elevación (materiales / profundidad)
`--shadow-1` (chips) · `--shadow-2` (cards flotantes, tooltip) · `--shadow-3`
(menús, selects, popovers) · `--shadow-4` (modales, drawer). Recalibradas en oscuro.

### Cristal y safe-areas iOS
`--glass-bg` + `--glass-blur` (`saturate(180%) blur(20px)`) para el chrome fijo
translúcido. `--safe-top/-bottom/-left/-right` = `env(safe-area-inset-*)`.
`--topbar-h` (56px) y `--sidebar-w` (252px) definen el chrome.

## A.3 Shell FIJO (`components/AppShell.tsx`)

El shell es único y responsive; re-viste los 3 layouts (`AdminLayout`,
`EmployeeLayout`, `AgencyLayout`) **sin cambiar sus destinos**. Cada layout pasa
sus `NavLink`s por la prop `nav`; `AppShell` los coloca en el sidebar (desktop) y
en el drawer (móvil) reutilizando el mismo `<Sidebar>` + `<SidebarUserCard>`.

- **Desktop (≥769px)**: `.shell-sidebar` = sidebar ALEATICA **fijo** (`position:fixed`,
  100dvh, `z-index:40`); `.main` reserva `margin-left:var(--sidebar-w)` y desplaza.
  El header/menú permanecen anclados (antes desplazaban).
- **Móvil (≤768px)**: el sidebar se oculta; aparece `.shell-topbar` = **header de
  cristal fijo** (`--glass-bg` + `backdrop-filter`, safe-area de iOS) con
  hamburguesa + wordmark ALEATICA + avatar. La navegación vive en un **drawer
  off-canvas** (`.shell-drawer`, `min(86vw,320px)`) que entra desde la izquierda
  con resorte (Framer Motion) + scrim (`.shell-scrim`). Cierre por: scrim, botón X,
  `Escape`, o navegar (efecto sobre `location.pathname`). Bloquea el scroll del
  body y hace focus al abrir. `.main` reserva `padding-top` = topbar + safe-top.
- Bajo `prefers-reduced-motion` el drawer aparece sin deslizamiento.

**Regla de rollout**: las páginas NO gestionan header ni navegación; solo renderizan
su contenido dentro del `<Outlet/>`. No usar `position:fixed`/`sticky` propios que
compitan con el chrome del shell.

## A.4 Botón con pulsado táctil (`components/Button.tsx`)

Misma API (`variant` green|blue|red|white, `icon`, `submit`) — **todo `<Button>`
existente adopta el efecto sin tocar el call-site**. Al presionar:
`whileTap → scale(0.96)` con `SPRING_PRESS` (Framer Motion, nace en pointer-down)
+ `box-shadow: inset …` y `brightness(0.92)` en `:active` (CSS). Hover gateado a
`@media (hover:hover) and (pointer:fine)`. Anulado bajo `prefers-reduced-motion`
y cuando `disabled`. **Framer Motion controla `transform`**: no añadir `transition:
transform` en CSS a `.btn`.

## A.5 Catálogo de primitivas

| Primitiva | Fichero | Base | Notas de uso |
|---|---|---|---|
| `Button` | `Button.tsx` | Framer Motion | Pulsado táctil. API sin cambios. |
| `Modal` | `Modal.tsx` | propio + Framer | **Legacy vigente**: overlay atenúa + panel materializa desde `scale(0.96)` (centrado). Todas las llamadas existentes lo heredan. |
| `Dialog` | `Dialog.tsx` | Radix Dialog | Alternativa de rollout al `Modal`: mismo look (cabecera con tono+icono, body, footer) con trap de foco/scroll/Escape de Radix. Migración: `open`/`onOpenChange` sustituye al montaje condicional; `onOpenChange(false)` = antiguo `onClose`. |
| `ConfirmDialog` | `ConfirmDialog.tsx` | Radix AlertDialog | Confirmaciones (tone `green`/`red`, `busy`, `icon`). `onConfirm` es el mismo callback de mutación de los modales de confirmación actuales. |
| `Menu` | `Menu.tsx` | Radix DropdownMenu | Menú contextual (`items` con `icon`/`danger`/`disabled`). Escala desde el disparador. Teclado completo. |
| `Tooltip` + `TooltipProvider` | `Tooltip.tsx` | Radix Tooltip | Solo contenido no esencial. Montar `TooltipProvider` una vez cerca de la raíz. |
| `SelectField` | `SelectField.tsx` | Radix Select | Reemplazo de `<select>` con look ALEATICA; `onValueChange` = value. |
| `Tabs` | `Tabs.tsx` | propio | Sin cambios (tablist accesible). Radix Tabs disponible si se necesita. |
| `Toast` | `Toast.tsx` | propio | Sin cambios (suscrito a errores 403/5xx). |
| `Popover` | `Popover.tsx` | propio | Sin cambios (usado por `SidebarUserCard`). |

### Estilos Radix (`styles/components.css`, namespace `.rx-*`)
`.rx-overlay`, `.rx-dialog(.rx-dialog-narrow)`, `.rx-dialog-header{.green|.red|.amber}`,
`.rx-menu`/`.rx-menu-item(.danger)`, `.rx-tooltip`, `.rx-select-*`. Animaciones de
entrada/salida vía `data-state` (Radix retiene el nodo hasta terminar la de cierre);
`transform-origin` anclado al disparador con `var(--radix-*-transform-origin)`.
Bloque final `@media (prefers-reduced-motion: reduce)` desactiva estas animaciones.

## A.6 Checklist de accesibilidad / móvil (aplicar en rollout)

- Touch targets ≥ 44px (topbar/drawer ya cumplen; botones de acción móvil ≥44px).
- Contraste AA: usar tokens de tinta `--*-deep`/`--ink` sobre soft/panel (ya validados
  en el contrato §1–2; los `deep` se aclaran en oscuro para conservar AA).
- `focus-visible` global en `base.css`; no eliminar outlines.
- `prefers-reduced-motion`: toda animación nueva debe degradar (opacidad/color sí,
  desplazamiento no) — patrón ya aplicado en Button/Modal/AppShell/`.rx-*`.
- Breakpoint del shell: **768px** (móvil) / **769px+** (desktop). Mantenerlo coherente.
