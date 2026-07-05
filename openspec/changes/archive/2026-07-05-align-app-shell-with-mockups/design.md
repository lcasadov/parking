# Design: align-app-shell-with-mockups

## Context
Los mockups definen el chrome corporativo (QRIA) en `docs/mockups/styles.css` +
`shell.js`: header blanco de 64px con curva decorativa a la derecha, logo conic,
marca, título y avatar; sidebar blanco de 200px con `nav-item` (icono Tabler +
etiqueta, activo verde-soft + borde izquierdo); modales con cabecera de color. La
app hoy tiene los tokens (de `frontend-bootstrap`) pero el header es plano, el
sidebar parece enlaces y los modales no tienen cabecera de color. `@tabler/icons-webfont`
ya está instalado e importado.

## Goals
- Reproducir el header (curva + logo + marca + título + avatar) conservando los controles funcionales (exportar, idioma, tema, logout).
- Reproducir el sidebar `nav-item` con iconos y estado activo del mockup (fin del aspecto "enlace subrayado").
- Modales con cabecera de color (verde por defecto, roja para acciones destructivas).
- Cero cambios de contrato/rutas/lógica; solo presentación.

## Decisions
- **Curva como SVG inline** (2 paths del `shell.js`), no imagen. *Por qué:* escala sin assets y usa los colores del token; idéntico al mockup.
- **Avatar con iniciales del usuario autenticado** (de `first_name`/`last_name`). *Por qué:* el mockup muestra iniciales ("RB"); se toma del usuario real. Al pulsar, menú con los controles (idioma/tema/exportar/logout) si se quiere compactar, o se dejan visibles.
- **`nav-item` reutilizable** con `NavLink` de react-router y clase `active`. *Por qué:* mantiene el enrutado; solo cambia el estilo (icono + estados). Se mapea cada ruta a su icono Tabler (`ti-calendar-event`, `ti-inbox`, `ti-users`, `ti-parking`, `ti-armchair`, `ti-map-2`, `ti-user-plus`, `ti-history`, `ti-settings`…).
- **Agrupar ítems secundarios bajo "Administración"** (Disponibilidad, Liberaciones, Login-logs). *Por qué:* el mockup tiene un sidebar corto con "Administración" como submenú; reduce ruido.
- **Badge de Solicitudes**: conteo de pendientes (query existente) en `badge-red`. *Por qué:* el mockup lo muestra; es un dato ya disponible.
- **Modal**: añadir prop `variant` (green/red) a la cabecera. *Por qué:* aprobar = verde, rechazar/eliminar = rojo, como los mockups 5/6.

## Risks
- **Romper tests existentes** de layout/modales por cambios de estructura. *Mitigación:* mantener roles/labels accesibles (headings, aria-labels), ajustar solo los tests estrictamente afectados; el enrutado y los textos i18n no cambian.
- **Regresión responsive**: el header/sidebar en móvil. *Mitigación:* conservar/मejorar los breakpoints; verificación visual en escritorio y móvil.
- **Iconografía**: mapear cada ruta a un icono Tabler correcto. *Mitigación:* usar el mapa del `shell.js` como referencia.

## Migration Plan
- Sin migración de datos ni esquema; sin cambios de contrato.
- Solo ficheros de frontend (`layouts/`, componente header, `Modal`, `styles/*.css`).
- Verificación visual con capturas app-vs-mockup (escritorio + móvil).
