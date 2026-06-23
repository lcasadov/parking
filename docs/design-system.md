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
- **Focus visible**: `outline: 2px solid var(--green); outline-offset: 2px` (pendiente de añadir a `styles.css`, §12).
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
2. **Focus ring de accesibilidad**: añadir `outline: 2px solid var(--green); outline-offset: 2px` a elementos interactivos en `styles.css` (§9) — hoy no está.
3. **Container principal**: definir `max-width` (~1300px) + margin auto para pantallas anchas (no presente en los mockups).
4. **Tokens tipográficos y de espaciado como variables CSS**: hoy los tamaños/paddings están inline en `styles.css`; conviene extraerlos a variables (`--text-sm`, `--space-3`, …) para un único punto de cambio.
5. **Cableado i18n de `date-fns`** (selección de locale `es`/`en` en runtime).
6. **Drawer móvil** del sidebar (alternativa a la barra horizontal) si el número de items crece.
