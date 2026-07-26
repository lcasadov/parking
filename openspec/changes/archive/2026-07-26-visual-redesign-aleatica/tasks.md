# Tasks — visual-redesign-aleatica

Reconstrucción as-built: todas las tareas reflejan trabajo ya implementado y verificado (build verde, lint 0 en cada commit, según sus propios mensajes) en la rama `feat/fullstack/rediseno-completo`. Se marcan `[x]` porque describen el estado ya mergeado a la rama, no trabajo pendiente.

## 1. Ola A — Fundaciones (motion + primitivas headless + shell fijo)

- [x] 1.1 Añadir dependencias `@radix-ui/react-*` (dialog, alert-dialog, dropdown-menu, popover, select, tabs, tooltip, visually-hidden) y `framer-motion` (`b5c0567`)
- [x] 1.2 Tokens de motion/elevación/cristal/safe-areas en `tokens.css`; espejo JS en `theme/motion.ts` (`DUR`, `EASE`, `SPRING*`, `PRESS_SCALE`) (`b5c0567`)
- [x] 1.3 `Button` con pulsado táctil (`whileTap` + spring crítico) respetando `prefers-reduced-motion` (`b5c0567`)
- [x] 1.4 Primitivas Radix headless: `Dialog`, `ConfirmDialog`, `Menu`, `Tooltip`, `SelectField` (`b5c0567`)
- [x] 1.5 `AppShell` con chrome fijo: sidebar fijo desktop / header de cristal + drawer off-canvas móvil (Framer Motion, scroll-lock, cierre por Escape), re-vistiendo los 3 layouts sin tocar rutas/wiring (`b5c0567`)

## 2. Olas B1-B4 — Pantallas por área (mobile-first + migración a Radix)

- [x] 2.1 Ola B1 — Empleado: `MyWeekPage` con tarjetas de día y motion escalonado; `FloorPlanPage` con `PageHeader`; `MyRequestsPage`/`MyResourcesPage` tabla→tarjetas en móvil (solo CSS); modales del empleado a Radix `Dialog` conservando `role="dialog"` (`7ab15c2`)
- [x] 2.2 Ola B2 — Admin operativo: modales operativos a Radix `Dialog`; Ocupación con columna de recurso sticky en móvil; tablas admin→tarjetas (`.table-cards-mobile`); fix funcional `mergeFixedAssignmentDays` (empareja por `resourceId`+`resourceType`, tarea 7.1 de `restructure-admin-workflows`) (`3e8b6ed`)
- [x] 2.3 Ola B3 — Gestión: `EmployeeFormModal` con mapa día→recurso por tipo, prefill sin colapsar, guardado multi-PUT resiliente, pestaña Histórico (tarea 7.3 de `restructure-admin-workflows`); Empleados/Plazas/Puestos/Auditoría/Accesos→tarjetas en móvil; Settings pulido (`024e6d8`)
- [x] 2.4 Ola B4 — Login rediseñado (panel editorial + card, mobile-first, spinner de envío); `Toast` con tonos success/info; `TooltipProvider` montado; últimos modales legacy a Radix; fundido de pestañas generalizado (`ffd6005`)
- [x] 2.5 Marcar 7.1/7.3 en `restructure-admin-workflows/tasks.md`; diferir 7.2/7.4/7.5 (tests/docs pendientes en ese change) (`f9765e3`)

## 3. Sistema visual dark-premium Flexopus (autoridad de color vigente)

- [x] 3.1 Tipografía Geist (auto-alojada) + Geist Mono tabular; verde ALEATICA reinterpretado como verde pino desaturado (iteración `581be80`, superada en color por 3.2) (`581be80`)
- [x] 3.2 Adoptar paleta del mockup aprobado (dark `#0c1411`, acento esmeralda `#0f9e68`/`#33d68a`) en `tokens.css`, con nombres de token heredados conservados como alias; botones/pills/cards del mockup; sidebar premium con nav activo en pastilla esmeralda (`b4c54fc`)
- [x] 3.3 Dashboard de inicio nuevo (KPIs + sparklines + pendientes accionables + actividad reciente); índice del `ADMIN` pasa de Empleados a Dashboard (`b4c54fc`)
- [x] 3.4 Quitar la curva de marca del header de login (no encaja con el nuevo lenguaje visual) (`06990ca`)

## 4. Plano — nivel Flexopus sobre la imagen real

- [x] 4.1 Mantener `floor-plan.png` (CAD real) con tratamiento por tema (blueprint invertido en oscuro, atenuado en claro); marcadores premium por estado (chips opacos, halo, resorte al hover); tooltip con etiqueta/ocupante; pin de avatar del ocupante; panel lateral en tarjetas con pills + KPIs del día (`2b50df5`)
- [x] 4.2 Quitar leyenda flotante (los chips de filtro ya cumplen esa función); tooltips sacados de la capa clipada/transformada (clamp horizontal + flip vertical); quitar texto "PLANTA DE OFICINA" redundante (`587b138`)
- [x] 4.3 Edición de posiciones no destructiva: "Editar posiciones" → Cancelar/Guardar; arrastres bufferizados en cliente; Guardar persiste secuencialmente; Cancelar revierte (`0ce1c49`)
- [x] 4.4 "Ver en plano" desde filas de Ocupación: hover→tooltip con mini-plano; click→modal con plano completo y puesto resaltado con pulso (reutiliza `FloorPlanSurface` en modo solo-lectura); ya no navega a la ruta del plano (`a71a164`)

## 5. Shell — sidebar/drawer como chrome único (estado final)

- [x] 5.1 Fix doble header en desktop (regla CSS sin media query mostraba la barra móvil también en desktop); topbar de cristal fina transitoria (`.shell-deskbar`) con toggles idioma/tema (`1dad759`)
- [x] 5.2 Completar la topbar transitoria con CTA "Nueva reserva" (navega al Plano según rol; oculto para AGENCIA) (`7fd621c`)
- [x] 5.3 Toggle de tema sol/luna (selector segmentado, a juego con idioma); sidebar más compacto (logo 94px, sin divisor, nav en `--ink-2`) (`8111e13`)
- [x] 5.4 Logo simple ALEATICA en el drawer (símbolo + "parking" + "ALEATICA", lockup horizontal); favicons reales (ico/16/32/apple-touch); elimina el CDN de Google Fonts residual (`e9ebf2b`)
- [x] 5.5 **Vaciar la barra superior de escritorio**: eliminar `.shell-deskbar`; CTA "Nueva reserva" pasa a slot bajo la marca del sidebar; toggles idioma/tema al pie sobre la tarjeta de usuario; título de sección sube al top del contenido; en móvil, CTA y controles viven en el drawer (estado final de `AppShell`/`Sidebar`) (`668188a`)

## 6. Ocupación y Gestión al nivel del mockup

- [x] 6.1 Ocupación: columna "hoy" contrastada, celda libre con "+", columna de recurso y cabecera sticky en desktop; Empleados: chips día→recurso visibles; fix capitalización de fecha larga (`::first-letter`) (`c67d140`)
- [x] 6.2 Ocupación "control-room": hero con modo explícito ("Plazas de parking"/"Puestos de oficina", `aria-live`) conmutado por `ResourceModeSwitch`; fila de KPIs del modo con datos reales; rejilla recurso×día con columna sticky e iniciales del ocupante (`92332d4`)
- [x] 6.3 Gestión al nivel premium: mini-KPIs (`StatTile`) en Recursos (Plazas/Puestos) y Registros (Auditoría/Accesos); Configuración con teselas de modo Manual/Auto y badge de modo actual (`0cf4a40`)
- [x] 6.4 Ocupación: quitar pestaña Disponibilidad (absorbida por el filtro "Solo libres"); filtros rápidos siempre visibles con contadores reales (Todos/Solo libres/Ocupados/Liberados/Solicitudes); título al tamaño estándar (`fs-h2`) (`a71a164`)
- [x] 6.5 Cabeceras consistentes en toda la app: secciones fusionadas (Recursos, Liberar, Registros, Mis plazas) usan `PageHeader` (título arriba) + `SectionSwitch` grande (reutiliza `.occ-mode`) siempre debajo, sin tabs pequeñas duplicando el título; nuevo `EmbeddablePageHeader` para la sub-página embebida (solo pinta su acción de crear/exportar) (`277ffce`)

## 7. Cierre de la reconstrucción as-built

- [x] 7.1 Verificar que ningún commit del rango documentado introduce cambios de backend, contrato de API o esquema de datos (confirmado por `git show --stat` de los 22 commits de origen)
- [x] 7.2 Confirmar que las tareas 7.1/7.3 de `restructure-admin-workflows` quedan reflejadas como completadas por esta rama (`f9765e3`) y que 7.2/7.4/7.5 siguen pendientes en ese change (no se resuelven aquí)
- [x] 7.3 Registrar en `proposal.md` (Impact) la deriva de `docs/design-system.md` respecto a `tokens.css` como deuda documental preexistente, sin corregirla en este change
- [x] 7.4 `openspec validate visual-redesign-aleatica --strict` sin errores
