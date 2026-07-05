# Tasks: align-app-shell-with-mockups

> Presentación del chrome global alineada a docs/mockups/styles.css + shell.js. Sin cambio de contrato/rutas/lógica. Verificación visual app-vs-mockup.

## 1. Header
- [ ] 1.1 Componente header con curva SVG decorativa (2 paths `#c0dd97`/`#ef9f27`), `logo-dot` (conic-gradient), marca "parking / ALEATICA", título de página.
- [ ] 1.2 Avatar con iniciales del usuario autenticado; controles idioma/tema/exportar/logout accesibles (visibles o en menú del avatar).
- [ ] 1.3 Estilos `.app-header/.curve/.logo-dot/.brand-*/.page-title/.avatar` acordes al mockup.

## 2. Sidebar
- [ ] 2.1 `nav-item` con icono Tabler + etiqueta; mapear cada ruta a su icono (`ti-calendar-event`, `ti-inbox`, `ti-users`, `ti-parking`, `ti-armchair`, `ti-map-2`, `ti-user-plus`, `ti-history`, `ti-settings`).
- [ ] 2.2 Estado activo `green-soft` + borde izquierdo verde 3px + icono verde (sin subrayado).
- [ ] 2.3 Badge rojo de conteo en Solicitudes (pendientes).
- [ ] 2.4 Agrupar secundarios (Disponibilidad, Liberaciones, Login-logs) bajo "Administración".
- [ ] 2.5 Aplicar a `AdminLayout` y `EmployeeLayout`.

## 3. Modales
- [ ] 3.1 `Modal` con prop `variant` (green por defecto / red destructivo): cabecera de color, título blanco.
- [ ] 3.2 Aplicar `red` a modales destructivos (rechazo, eliminación, liberación administrativa) y `green` al resto.

## 4. Login y responsive
- [ ] 4.1 `auth-head` con curva decorativa (mockup 12).
- [ ] 4.2 Verificar header/sidebar responsive (<768px) sin romper la vista.

## 5. Quality Gate + verificación visual
- [ ] 5.1 `npm run lint && npm test && npm run build` verdes, cobertura ≥80%; ajustar solo los tests estrictamente afectados por la reestructura (roles/labels accesibles preservados).
- [ ] 5.2 Verificación visual con capturas app-vs-mockup (Plazas, Empleados, Solicitudes, Login) en escritorio y móvil.
