# Proposal: align-app-shell-with-mockups

## Why
El "chrome" global de la app (header y sidebar) y los modales no coinciden con la
identidad visual de los mockups (docs/mockups): falta la **curva decorativa
verde/naranja** del header y el avatar de usuario, el **sidebar** se ve como enlaces
subrayados en vez de `nav-items` con icono + estado activo (verde-soft + borde
izquierdo), faltan los **iconos Tabler** y el badge de conteo de Solicitudes, y los
**modales** no llevan la cabecera de color (verde/roja). Como es el chrome común,
afecta a **todas** las pantallas: alinearlo acerca visualmente toda la app de una vez.

## What Changes
- **Header** (`app-header`): curva SVG decorativa (2 paths, `#c0dd97` + `#ef9f27`), `logo-dot` (conic-gradient), marca "parking / ALEATICA", título de página, y **avatar** con iniciales del usuario; se conservan los controles funcionales (exportar, ES/EN, tema, cerrar sesión) integrados a la derecha.
- **Sidebar**: `nav-item` con **icono Tabler** + etiqueta, color muted, **estado activo** `green-soft` + borde izquierdo verde de 3px (sin aspecto de enlace/subrayado); badge rojo de conteo en Solicitudes; agrupación de ítems secundarios (Disponibilidad, Liberaciones, Login-logs) bajo "Administración".
- **Modales**: cabecera de color (`modal-header.green` / `.red`) con título en blanco.
- **Login** (`auth-head`): curva decorativa como en el mockup 12.
- Consolidar los tokens/estilos usados con `docs/mockups/styles.css` (fuente de verdad del design-system).

## Capabilities
- `app-shell` (ADDED — requisitos de presentación del chrome global: header, sidebar, modales, alineados al design-system)

## Impact
- **Frontend**: `AdminLayout`, `EmployeeLayout`, el componente de header, el `Modal`, `styles/*.css`. Presentacional; sin cambio de rutas, contrato API ni lógica.
- **Diseño**: coherencia con `docs/design-system.md` y `docs/mockups/styles.css`.

## Out of scope
- El contenido específico de cada pantalla (columnas de tablas, formularios) — se abordan por pantalla si hace falta.
- La paridad del plano de puestos (imagen real + coordenadas) → change aparte.
- El soporte de puestos en flujos de admin (aprobación/asignación fija) → change aparte.
