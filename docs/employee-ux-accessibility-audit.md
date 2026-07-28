# Auditoría de accesibilidad y usabilidad móvil — Portal de empleado

> WCAG 2.1 AA (+ notas 2.2 / HIG móvil). Revisión estática de código + CSS y
> cálculo de contrastes sobre tokens. Recomendada verificación posterior con
> lector de pantalla real. **[APLICADO]** = corregido en la sesión nocturna.

## Resumen
- 🔴 Bloqueante: 0
- 🟠 Importante: 6 (G1, G2, C1, W1, T1 aplicados; navegación por flechas del calendario pendiente)
- 🟢 Mejora: 9 (pendientes, siguiente sprint)

Fortalezas confirmadas: foco visible global (`:focus-visible`), diálogos con trampa de foco y bloqueo de scroll (Radix), tablas semánticas que se repintan como tarjetas en móvil, iconos decorativos `aria-hidden`, `DayBadges` con `sr-only`, banners/toasts con `role`, `minmax(0,1fr)` anti-overflow, `noopener,noreferrer` en enlaces externos.

## Global / design system
- **🟠 G1 [APLICADO]** — `--ink-faint`/`--ink-3`/`--text-muted` no llegaban a 4.5:1 (≈2.98:1 claro). Subidos a `#5f6f67` (claro) / `#8fa39a` (oscuro).
- **🟠 G2 [APLICADO]** — áreas táctiles <24px: `.absence-chip-x` (22→28px), `.toast-dismiss` (padding 2→6px).
- **🟢 G3** — los diálogos no cierran con Escape (decisión de producto global). Permitir Escape en diálogos cortos (logout/liberar/ausencia), mantener el bloqueo solo en el asistente fullScreen.
- **🟢 G4** — anillo de foco verde sobre superficies verdes puede ser débil; considerar doble capa (`box-shadow` + `outline`).

## ReservationCalendar / MultiSelectCalendar
- **🟠 C1 [APLICADO]** — cada día solo anunciaba el número. Añadido `aria-label` con fecha completa localizada + `aria-current="date"` en hoy; la fecha elegida es `role=status`.
- **🟢 C2** — sin patrón `role=grid` ni navegación por flechas/Home/End (APG Date Picker). Implementar roving tabindex.
- **🟢 C3** — días de otro mes son clicables (atenuados); considerar deshabilitarlos o navegar de mes.
- **🟢 C4** — alto de celda 38px; asegurar ≥40px y gap suficiente en móvil.

## CreateRequestModal / ResourceChoiceCard
- **🟢 M1** — `scrollIntoView({behavior:'smooth'})` ignora `prefers-reduced-motion`.
- **🟢 M2** — varios `role=status` simultáneos (conflicto/fijo/modo/waitlist) pueden solaparse; agrupar o dejar solo el más relevante.
- **✔ M3** — `aria-pressed` en las tarjetas y estado por texto+icono (no solo color): correcto. Vigilar contraste de `tone-none` (`--orange-deep`, ≈4.9:1).

## MyWeekPage
- **🟠 W1 [APLICADO]** — el estado del recurso se truncaba con ellipsis+nowrap (se perdía info al ampliar/idiomas largos). Ahora envuelve; `.week-resource-state` sube a `--text-sm` y quita opacidad.
- **🟢 W2** — varios botones "Reservar" idénticos para el lector; añadir `aria-label` contextual (recurso + día).
- **🟢 W3** — naranja para "libre" puede leerse como advertencia; el texto acompaña (ok 1.4.1), revisar coherencia con leyenda.
- **✔ W4** — aviso "pendiente" con `role=status`: correcto.

## MyRequestsPage
- **✔ R1** — tabla semántica + responsive: sólida.
- **🟢 R2** — `nav` de paginación reutiliza el título de página como nombre; usar una clave propia.
- **🟢 R3** — verificar contraste de los `status-badge`.

## MyFixedAssignmentsPage / AbsenceReleaseModal
- **🟠 A1 [APLICADO]** — botón de quitar chip <24px (ver G2).
- **✔ A2** — inputs/checkboxes bien etiquetados; chips con `role=list`. (El modal pasó a calendario multi-select; mantener labels.)
- **🟢 A3** — resultado parcial del lote: si algunos días fallan, informar "N liberados, M no disponibles".
- **✔ A4** — cabecera de acciones con `sr-only-head`.

## Toast
- **🟠 T1 [APLICADO]** — la live region no era persistente (el primer toast podía no anunciarse). Ahora el contenedor se monta siempre con `aria-live=polite`; los errores mantienen `role=alert`.

## SidebarUserCard / DeskMapButton / ParkingDirectionsButton
- **✔ S1** — patrón correcto; matiz: el `aria-label="Cerrar sesión"` sustituye nombre/rol para el lector.
- **✔ D1** — botones con texto e iconos decorativos; `noopener,noreferrer`. Sugerencia: avisar de que abre ventana externa (3.2.5).

## Verificación recomendada
- Lector de pantalla real (VoiceOver iOS/Safari; NVDA/Chrome) en: crear reserva desde calendario, liberar desde Mi Semana, modal de ausencia con varios chips.
- Zoom 200%/400% en Mi Semana y modal de reserva.
- axe-core/Lighthouse como red de seguridad (no detectan C1/C2/W1/T1).
