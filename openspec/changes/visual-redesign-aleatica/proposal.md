# Proposal — visual-redesign-aleatica

Rama: `feat/fullstack/rediseno-completo` · Reconstrucción as-built (documentación posterior a la implementación).

## Why

`restructure-admin-workflows` corrigió arquitectura de información y funcionalidad, y diseñó explícitamente el **rediseño visual/estético como fase UI posterior separada** (Non-Goal). `design-system-components` sentó la base de clases/componentes de paridad con los mockups estáticos (`docs/mockups/`). Ambos changes dejaban pendiente la capa que este change documenta: adoptar el **lenguaje visual dark-premium tipo Flexopus** (mockup aprobado `redesign-mockup.html`) como aspecto final de la aplicación — shell fijo con motion, tipografía Geist, paleta esmeralda, Dashboard de inicio y plano sobre la imagen real de la oficina — sin reabrir la arquitectura de información ni el catálogo base de componentes ya resueltos por esos dos changes.

Este documento reconstruye **as-built** (a partir de `git log`/`git show` y el código actual) el trabajo ya implementado en la rama, para que quede trazado en OpenSpec antes del merge a `develop`.

## What Changes

- **Fundaciones de motion y primitivas headless (Ola A).** Se adoptan **Radix UI** (`Dialog`, `AlertDialog`/`ConfirmDialog`, `DropdownMenu`/`Menu`, `Tooltip`, `Select`) y **Framer Motion** como base de interacción; tokens de motion (`--dur-*`, `--ease-*`, `--press-scale`) y su espejo en JS (`theme/motion.ts`); `Button` con pulsado táctil (`whileTap` + spring crítico, sin overshoot salvo momentum de drag); todo respeta `prefers-reduced-motion` vía `useReducedMotion`. Safe-areas iOS (`--safe-top/bottom/left/right`).
- **Shell fijo de la aplicación.** `AppShell` pasa a chrome fijo en las dos resoluciones: sidebar fijo en escritorio (position:fixed, 100vh) y, en móvil, header de cristal fijo + **drawer off-canvas** con resorte y scrim (Framer Motion `AnimatePresence`), bloqueo de scroll del `body` y cierre por Escape/click-fuera/navegación. Iteración posterior (`668188a`) **vacía la barra superior de escritorio**: el CTA "Nueva reserva" y los toggles de idioma/tema pasan al sidebar (bajo la marca y sobre la tarjeta de usuario, respectivamente), liberando la franja superior del contenido para el título de sección.
- **Sistema visual dark-premium "Flexopus" (autoridad de color vigente).** Reescritura de `tokens.css`: acento único **esmeralda** (`--accent #0f9e68` claro / `#33d68a` oscuro), tema oscuro **dark-premium** (`--bg #0c1411`) como paleta de referencia del mockup aprobado, con los nombres de token heredados (`--green`, `--bg-page`, `--state-*`…) conservados como **alias** hacia los valores nuevos para propagar el cambio sin reescribir cada componente. Radios/sombras/espaciado ajustados al mockup (`--radius-panel 20px`, `--radius-pill`, `--shadow-panel/-card` en capas).
- **Tipografía premium Geist.** Sustitución de Cormorant Garamond + Mulish por **Geist Variable** (cuerpo/UI/titulares, tracking óptico negativo en titulares) y **Geist Mono Variable** (numerales/KPI tabulares), auto-alojadas vía `@fontsource-variable/*` (sin CDN de Google Fonts).
- **Dashboard de inicio (capacidad nueva).** `DashboardPage` como índice del `ADMIN` (antes: Empleados): saludo + fecha, fila de KPIs (ocupación del día, pendientes, plazas/puestos libres) con sparklines decorativas, panel de solicitudes pendientes accionable (aprobar in situ) y panel de actividad reciente (auditoría), alimentados por endpoints ya existentes (`/occupancy`, `/availability`, `/requests`, `/employees`, `/audit`).
- **Plano sobre la imagen real de la oficina, nivel Flexopus.** Se mantiene `floor-plan.png` (CAD real, no se sustituye por el plano de círculos) con tratamiento visual por tema (blueprint invertido en oscuro, atenuado en claro); marcadores premium por estado con halo y resorte al hover; tooltip con clamp/flip que ya no se recorta ni escala con el zoom (`587b138`); leyenda flotante eliminada (los chips de filtro ya cumplen esa función); edición de posiciones **no destructiva**: entrar en modo edición cambia el CTA a Cancelar/Guardar, los arrastres se bufferizan en cliente y solo se persisten (`PUT` secuencial) al confirmar.
- **Sidebar limpio con logo/branding ALEATICA + favicons.** Lockup horizontal compacto (símbolo ALEATICA + "parking" + "ALEATICA") en cabecera del sidebar/drawer; favicons reales (`ico`/16/32/apple-touch) sustituyendo el placeholder de CRA.
- **Toggle de tema sol/luna.** `ThemeToggle` pasa de switch con texto a selector segmentado con iconos sol/luna (a juego con el selector de idioma), sin tocar la lógica de `ThemeProvider`.
- **Pantallas operativas y de gestión al nivel del mockup.** Empleado mobile-first (Ola B1: `MyWeekPage` con tarjetas de día y motion escalonado, `FloorPlanPage` con `PageHeader`, tablas→tarjetas en móvil vía CSS); admin operativo (Ola B2: modales migrados a Radix `Dialog` conservando `role="dialog"`, tablas admin→tarjetas en móvil); gestión (Ola B3: `EmployeeFormModal` con mapa día→recurso, Empleados/Plazas/Puestos/Auditoría/Accesos→tarjetas móviles); login + toasts (Ola B4: `LoginPage` con panel editorial + card, `Toast` con tonos success/info, `TooltipProvider`); Ocupación como "control-room" con modo explícito por título, KPIs del modo, filtros rápidos siempre visibles y "Ver en plano" (tooltip con mini-plano + modal de plano completo); Gestión con mini-KPIs (`StatTile`) en Recursos/Registros/Ajustes; cabeceras unificadas (`PageHeader`/`EmbeddablePageHeader` + `SectionSwitch` en secciones fusionadas, sin tabs pequeñas duplicando el título).

## Capabilities

### New Capabilities
- `dashboard`: pantalla de inicio del `ADMIN` con KPIs de ocupación/pendientes/libres, panel de pendientes accionable y actividad reciente, como ruta índice del rol.

### Modified Capabilities
- `design-system`: tokens de color reemplazados por la paleta dark-premium esmeralda (con alias de compatibilidad), tipografía Geist/Geist Mono en sustitución de Cormorant Garamond/Mulish, radios/sombras/espaciado del mockup Flexopus, tokens de motion/elevación/cristal y safe-areas.
- `theming`: el tema oscuro pasa a ser la paleta dark-premium de referencia (antes variante oscura de la paleta institucional original); el control de tema es un selector segmentado sol/luna en el sidebar/drawer.
- `app-shell`: chrome fijo (sidebar fijo desktop / drawer off-canvas móvil con Framer Motion); CTA "Nueva reserva" y toggles de idioma/tema alojados en el sidebar (no en una topbar); logo/lockup ALEATICA en cabecera del sidebar; primitivas de diálogo/menú/tooltip migradas a Radix.
- `floor-plan`: presentación sobre la imagen real de la oficina con tratamiento por tema, marcadores/tooltip/panel lateral de nivel premium, edición de posiciones no destructiva (Cancelar/Guardar con buffer local), leyenda flotante eliminada.
- `admin-screens`: Ocupación como vista "control-room" (modo explícito, KPIs del modo, filtros siempre visibles, "Ver en plano"); Gestión (Recursos/Registros/Ajustes) con mini-KPIs y controles en tarjeta; cabeceras de secciones fusionadas unificadas a `PageHeader` + `SectionSwitch`.
- `employee-portal`: "Mi Semana" con tarjetas de día y motion escalonado; plano y listados en tarjetas en móvil; modales del empleado migrados a Radix `Dialog`.

## Impact

- **Frontend:** ver `design.md` (Context) para el listado completo de ficheros por ola. Nuevas dependencias: `@radix-ui/react-*` (dialog, alert-dialog, dropdown-menu, popover, select, tabs, tooltip, visually-hidden), `framer-motion`, `@fontsource-variable/geist`, `@fontsource-variable/geist-mono`.
- **Backend:** sin cambios (change puramente de presentación; ningún endpoint nuevo ni contrato modificado).
- **Datos:** sin cambios de esquema.
- **Docs:** `docs/design-system.md` documenta el "RESTYLE 2026" (Stripe/Apple, verde pino `#1a7548`) de `581be80`, anterior al pivote a la paleta dark-premium esmeralda de `b4c54fc`; **queda desactualizado respecto a `tokens.css`** (fuente de verdad vigente) y no se corrige en este change (fuera de alcance; ver Out of Scope).
- **No hay Issues de GitHub asociados** a los commits de origen (rama de trabajo directo, sin flujo de Issues por commit); este change documenta el trabajo ya mergeado a la rama para trazabilidad OpenSpec previa al PR a `develop`.

## Out of Scope

- Redefinir el catálogo base de componentes/clases del design-system (`design-system-components`, WP0-WP5) — este change consume y re-viste esa base, no la sustituye.
- Reabrir la arquitectura de información / navegación por tarea (`restructure-admin-workflows`) — los 9+4 destinos y las fusiones en pestañas se mantienen; este change solo cambia su piel visual.
- Sincronizar `docs/design-system.md` con la paleta dark-premium vigente en `tokens.css` (deuda documental preexistente, no introducida por este change).
- El asistente de reserva multipaso, lista de espera, reservas de visitante genéricas y demás funcionalidad de commits posteriores en la misma rama (`fec0a3f` en adelante) — quedan fuera del alcance estético que cubre este change; son capacidades funcionales nuevas que merecerán su propio change.
