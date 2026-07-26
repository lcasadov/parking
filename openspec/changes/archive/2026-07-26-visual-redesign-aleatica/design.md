## Context

Reconstrucción **as-built**: no hubo `design.md` previo a la implementación — este documento deriva de `git log`/`git show` sobre la rama `feat/fullstack/rediseno-completo` (base: `develop@a8aa8dc`) y de la lectura del código resultante (`frontend/src/styles/tokens.css`, `components.css`, `components/AppShell.tsx`, `components/Sidebar.tsx`, `components/ThemeToggle.tsx`, `pages/DashboardPage.tsx`, `pages/FloorPlanPage.tsx` y componentes del plano).

**Commits de origen (orden cronológico):**

| Commit | Descripción |
|---|---|
| `12e7a84` | chore: skills de diseño de interfaz (Claude Code, sin código de producto) |
| `b5c0567` | Ola A — fundaciones: Radix + Framer Motion, tokens de motion/elevación/cristal, safe-areas, `AppShell` con chrome fijo |
| `7ab15c2` | Ola B1 — pantallas de empleado mobile-first (MyWeek, FloorPlan header, MyRequests/MyResources, modales a Radix) |
| `3e8b6ed` | Ola B2 — admin operativo mobile-first + fix funcional `mergeFixedAssignmentDays` (recurso por día, tarea 7.1 de `restructure-admin-workflows`) |
| `024e6d8` | Ola B3 — gestión admin mobile-first + `EmployeeFormModal` con mapa día→recurso (tarea 7.3) |
| `ffd6005` | Ola B4 — login rediseñado, `Toast`/`Tooltip` providers, barrido de consistencia (últimos modales a Radix) |
| `f9765e3` | docs: marca 7.1/7.3 completadas en `restructure-admin-workflows/tasks.md`; difiere 7.2/7.4/7.5 |
| `581be80` | Lenguaje visual premium Stripe/Apple: tipografía Geist + paleta "verde pino" (superada por `b4c54fc`) |
| `06990ca` | fix: quitar curva de marca del header de login |
| `b4c54fc` | **Sistema visual Flexopus dark-premium** (autoridad de color vigente) + shell + Dashboard nuevo |
| `2b50df5` | Plano nivel Flexopus sobre `floor-plan.png` (imagen real) |
| `c67d140` | Ocupación + Empleados nivel mockup, fix capitalización de fecha |
| `1dad759` | fix: doble header en desktop; topbar de cristal fino (`.shell-deskbar`, transitorio) |
| `7fd621c` | Topbar de escritorio con toggles + CTA "Nueva reserva" (transitorio, ver `668188a`) |
| `8111e13` | Toggle de tema sol/luna + sidebar limpio |
| `e9ebf2b` | Logo ALEATICA en el drawer + favicons reales |
| `587b138` | fix: plano — quitar leyenda flotante, tooltips sin recorte |
| `0ce1c49` | Plano — edición de posiciones no destructiva (Cancelar/Guardar) |
| `92332d4` | Ocupación "control-room" con modo explícito en el título |
| `0cf4a40` | Pantallas de Gestión al nivel premium (StatTile/mini-KPIs) |
| `a71a164` | Ocupación — quita pestaña Disponibilidad, filtros rápidos, "Ver en plano" |
| `668188a` | **Shell: vacía la barra superior** — CTA y controles (idioma/tema) migran del `.shell-deskbar` transitorio al sidebar (estado final) |
| `277ffce` | Cabeceras consistentes: `PageHeader`/`EmbeddablePageHeader` + `SectionSwitch` en secciones fusionadas |

`1dad759`→`7fd621c` introducen una topbar de escritorio (`.shell-deskbar`) como paso intermedio; `668188a` la elimina y traslada su contenido (CTA + toggles) al sidebar, que es el estado **final** reflejado en `AppShell.tsx`/`Sidebar.tsx` actuales. El diseño documentado aquí describe el estado final, no los pasos intermedios.

Este change es puramente de **presentación**: no hay endpoints nuevos, contratos de API modificados, ni cambios de esquema. Se apoya en la base de clases de `design-system-components` (WP0: `.toggle`, `.modal-tabs`, `.day-cards`, `.popover`, etc.) y no reabre la arquitectura de información de `restructure-admin-workflows` (los mismos 9 destinos ADMIN / 4 EMPLOYEE, las mismas fusiones en pestañas).

## Goals / Non-Goals

**Goals (documentar as-built):**
- Trazar en OpenSpec el sistema visual dark-premium (tokens, tipografía Geist, paleta esmeralda) como autoridad vigente sobre `docs/design-system.md` §R.1-R.5 (que documenta una iteración intermedia ya superada).
- Documentar el shell fijo (sidebar/drawer) y la migración de CTA/controles a él como estado final.
- Documentar el Dashboard como capacidad nueva.
- Documentar el plano sobre imagen real con edición no destructiva.
- Referenciar (no duplicar) `design-system-components` y `restructure-admin-workflows` para lo que ya cubren.

**Non-Goals:**
- Prescribir cambios de código futuros — este change es retrospectivo.
- Resolver la deuda de `docs/design-system.md` desactualizado.
- Documentar los commits funcionales posteriores en la misma rama (wizard de reserva, lista de espera, reservas de visitante genéricas) — pertenecen a otra capa de cambio (funcional, no estética) y a otro change.

## Decisions

**D1. La paleta dark-premium de `b4c54fc` prevalece como fuente de verdad, no la de `581be80`.**
`581be80` (mismo día, 08:58) introdujo tipografía Geist + una paleta "verde pino" institucional (`#1a7548` sobre neutros cálidos claros), documentada en `docs/design-system.md` §R.1-R.5. `b4c54fc` (08:33 después, mismo día) la sustituye por la paleta del mockup aprobado (`#0f9e68` esmeralda sobre `#0c1411` dark-premium), conservando la tipografía Geist de `581be80`. El código vigente (`tokens.css`) es inequívocamente la paleta esmeralda dark-premium; `docs/design-system.md` no se actualizó tras `b4c54fc` y describe la paleta ya superada. Se documenta la discrepancia en `proposal.md` (Impact) en vez de silenciarla.

**D2. Alias de nombres de token para propagar el cambio sin tocar cada componente.**
`tokens.css` conserva los nombres heredados (`--green`, `--bg-page`, `--state-occupied-bg`, etc.) como alias `var(--accent)`/`var(--bg)`/etc. hacia los valores nuevos. Alternativa descartada: renombrar tokens en cascada por todos los `components.css`/TSX — habría multiplicado el diff sin beneficio funcional, dado que ambos changes previos (`design-system-components`, `restructure-admin-workflows`) ya dependen de esos nombres.

**D3. El shell fijo separa chrome de contenido: sidebar/drawer nunca hacen scroll con la página.**
Desktop: `position: fixed` a 100vh; el `<main>` desplaza. Móvil: header de cristal fijo arriba + drawer off-canvas (Framer Motion `AnimatePresence`/`motion.div`) que bloquea el scroll del `body` mientras está abierto y se cierra por Escape, click en scrim o navegación (efecto sobre `location.pathname`). Motivación (según mensajes de commit): reproducir el patrón "app" del mockup Flexopus (nav siempre accesible) en vez del header de scroll tradicional de los mockups originales de `docs/mockups/`.

**D4. CTA y controles globales viven en el sidebar, no en una topbar de escritorio.**
Iteración: `7fd621c` puso el CTA "Nueva reserva" + toggles en una topbar de cristal (`.shell-deskbar`); `668188a` la eliminó para "recuperar la franja superior del contenido" (mensaje de commit), moviendo el CTA a un slot bajo la marca del sidebar y los toggles de idioma/tema al pie, sobre `SidebarUserCard`. El componente `Sidebar` expone los slots `cta`/`controls`/`footer` para esto (`Sidebar.tsx`). El título de la pantalla queda como elemento superior único del área de contenido.

**D5. El plano conserva la imagen real (`floor-plan.png`), no el plano de círculos.**
`2b50df5` decide explícitamente mantener el CAD real con tratamiento visual por tema (invertido/blueprint en oscuro, atenuado en claro) en vez de sustituirlo por una representación esquemática — la imagen real ya existía y los mockups aprobados data-driven no aportaban una alternativa vectorial equivalente.

**D6. Edición de posiciones del plano pasa a no-destructiva.**
`0ce1c49`: antes, cada arrastre persistía inmediatamente (`PUT` por marcador). Se cambia a buffer local (los arrastres solo actualizan estado en cliente) con dos acciones explícitas — "Cancelar" (descarta y revierte al original) y "Guardar cambios" (`mutateAsync` secuencial sobre los puestos modificados). Motivo (mensaje de commit): evitar persistencia accidental de un arrastre en curso o un gesto fallido.

**D7. Dashboard reutiliza endpoints existentes; no introduce agregaciones de backend.**
Los KPIs (ocupación, pendientes, libres) y la actividad reciente se calculan en el cliente combinando las respuestas de `/occupancy`, `/availability` (por `resourceType`), `/requests` (pendientes), `/employees` y `/audit` ya consumidos por otras pantallas. Alternativa descartada: endpoint agregado `/dashboard/summary` — se prefirió no ampliar el backend para una capa puramente de presentación (este change es front-only).

**D8. Migración incremental a primitivas Radix conservando contratos de test.**
Todos los modales legacy migran a Radix `Dialog`/`AlertDialog` a lo largo de las Olas B1-B4, pero conservan el atributo `role="dialog"` (y equivalentes ARIA) para no romper los selectores de los tests existentes. Motivo: permitir la migración incremental sin coordinar un barrido simultáneo de la suite de tests E2E/unitarios.

## Risks / Trade-offs

- **Deriva entre `docs/design-system.md` y `tokens.css`.** El documento describe una paleta ya superada (§R.1-R.5 de `581be80`). Riesgo: quien lea solo la documentación implementará con la paleta incorrecta. Mitigación propuesta (fuera de alcance de este change): un change de solo-docs que resincronice `docs/design-system.md` con `tokens.css`.
- **Iteraciones intermedias no limpiadas del historial.** `.shell-deskbar` existió entre `1dad759` y `668188a`; el código final ya no lo referencia, pero quien lea el historial de commits sin el contexto de `668188a` puede asumir que la topbar de escritorio sigue vigente. Mitigación: este `design.md` documenta explícitamente el estado final.
- **Alcance visual amplio en una sola rama junto a cambios funcionales posteriores.** Los commits `fec0a3f` en adelante (wizard de reserva, lista de espera, visitantes genéricos) comparten rama con este rediseño visual pero son funcionalidad nueva, no estética. Mitigación: este change delimita su alcance a los commits listados en el Context y remite el resto a un change funcional separado.
- **Sin Issues de GitHub previos.** Los commits de origen no referencian `#<n>` (no se siguió el flujo de Issue→branch→PR de `CLAUDE.md` para este tramo de trabajo). Mitigación: este change se registra directamente en OpenSpec como reconstrucción as-built, y su futuro PR puede abrir el Issue correspondiente antes de mergear.

## Migration Plan

1. Sin migración de datos ni de esquema (change de presentación).
2. Sin pasos de despliegue especiales: los cambios ya están integrados en la rama `feat/fullstack/rediseno-completo`; el "rollout" es el propio merge a `develop` vía PR.
3. Rollback: revertir el merge de la PR; no hay estado persistente que deshacer (tokens/CSS/componentes, sin datos).
4. Seguimiento recomendado (fuera de alcance de este change): change de solo-docs para resincronizar `docs/design-system.md` con la paleta dark-premium vigente.
