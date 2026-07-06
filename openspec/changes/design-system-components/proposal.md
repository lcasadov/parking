# Proposal — design-system-components (C0)

Issue: #81 · Rama: `feat/frontend/81-design-system-base`

## Why

Auditoría de paridad visual (5 grupos de pantallas) revela una **causa raíz única**: ~14 clases del design-system que usan los mockups (`docs/mockups/styles.css`) **no existen o están incompletas** en `frontend/src/styles/components.css`. Por eso casi todas las pantallas divergen de los mockups. Sin portar esa base, cualquier arreglo por pantalla es un parche.

Regla transversal del proyecto: **donde un mockup solo dibuja "plaza", la implementación debe cubrir también "puesto"** (resource_type PARKING|DESK).

## What Changes

Rediseño de paridad total con los mockups (modales + apariencia), organizado en paquetes:

- **WP0 — Fundación**: portar/completar clases (`.toggle .track/.knob/.on`, `.modal-tabs`, `.field-row/.field-value`, `.day-cards/.day-card`, `.day-chip`, `.avatar-sm/.av-*`, `.popover/.pop-row`, `.week-card`, `.legend`, `.divider`, `.muted`, `.btn-back/.btn-success/.btn-danger-outline`, `.info-banner.red`, `.tab` caja + `.tab .count`, fix `.cell-free`). Componentes reutilizables: `Toggle`, `DayCards`, `Avatar`(sm/av-*), `Popover`, `InfoBanner`, `Legend`, `Tabs`, `FieldRow/FieldValue`; extender `Modal` (pestañas, icono+×, cabecera amber, footer bar). Reescribir `DayBadges`→`.day-chip`.
- **WP-BACK — Asignación fija robusta**: DELETE por `resourceType`, GET consumible para prefill plaza+puesto, atomicidad de doble PUT, unicidad. Tests + openapi.
- **WP1 — Empleados**: modal de empleado con pestañas DETALLES / PLAZA FIJA / PUESTO FIJO (recursos opcionales independientes). **Eliminar `FixedAssignmentsPage`**. Tabla con avatar de color + columnas plaza/días (day-chip) + legend.
- **WP2 — Auth/preferencias**: popover de usuario, tema como switch toggle, sesión expirada ámbar, info-banners de login/password.
- **WP3 — Solicitudes/calendario**: tabs caja + count rojo, info-banners en aprobar/rechazar, barra footer unificada, legend de calendario.
- **WP4 — Plazas/puestos/visitantes/auditoría**: toggle real en modales de plaza y puesto, legend, columna titular, pills de auditoría por color de acción (auditoría se queda en 2 rutas separadas).
- **WP5 — Móvil/plano**: week-cards con acciones, release con resumen/banners, editor de plano (marcadores neutros + guardar + banner ayuda), panel lateral con titular, solicitud unificada con banner rojo.

## Capabilities

- `app-shell` / design-system (componentes y estilos base compartidos).

## Impact

- Frontend: `styles/components.css`, ~8 componentes nuevos, `Modal`, `DayBadges`, y todas las páginas/modales de las 5 áreas. Se elimina `FixedAssignmentsPage`.
- Backend: paquete `fixedassignment` (DELETE/GET), `docs/openapi.yaml`.
- Sin cambios de datos (salvo lo que WP-BACK requiera; se espera ninguno).

## Out of scope

- Plano CAD real (bloqueado por asset; se mantiene el de círculos).
- Tab HISTÓRICO del modal de empleado (opcional).
- Unificar auditoría en una pantalla (decisión: se dejan 2 rutas).
- SSO (Fase 2).
