# Tasks — design-system-components (C0)

Leyenda: [ ] pendiente · [~] en curso · [x] hecho

## WP0 — Fundación design-system (frontend) — PRERREQUISITO
- [~] Portar clases faltantes a `components.css` (toggle track/knob, modal-tabs, field-row/value, day-cards, day-chip, avatar-sm/av-*, popover, week-card, legend, divider, muted, btn-back/success/danger-outline, info-banner.red, tab caja + count, fix cell-free)
- [~] Componentes: Toggle, DayCards, InfoBanner, Popover, Legend, Avatar(sm/av-*), Tabs, FieldRow/FieldValue
- [~] Extender Modal (pestañas, icono+×, cabecera amber, footer bar)
- [~] Reescribir DayBadges → day-chip
- [~] Quality gate: lint + test + build verdes

## WP-BACK — Asignación fija robusta (backend)
- [~] DELETE por resourceType (revocar solo plaza o solo puesto)
- [~] GET consumible para prefill plaza+puesto
- [~] Atomicidad doble PUT / unicidad
- [~] Tests unit + IT order-independent
- [~] Actualizar openapi.yaml
- [~] Quality gate: mvn verify verde, cobertura ≥80/75

## WP1 — Empleados + asignación fija (← WP0, WP-BACK)
- [ ] EmployeeFormModal con pestañas DETALLES / PLAZA FIJA / PUESTO FIJO (opcionales, independientes, day-cards, toggles)
- [ ] Mover alta/edición de asignación fija al modal; **eliminar FixedAssignmentsPage** (+ ruta + tests)
- [ ] EmployeesPage: avatar color + columnas plaza/días (day-chip) + legend + search-box
- [ ] RevokeFixedAssignmentModal: info-banner red + btn-danger-outline

## WP2 — Auth y preferencias (← WP0)
- [ ] Popover menú de usuario (avatar trigger, pop-rows, idioma, tema, logout)
- [ ] Tema como switch Toggle real
- [ ] SessionExpiredModal cabecera ámbar + icono + info-banner
- [ ] Login: info-banner red intentos + ojo contraseña + textos ayuda
- [ ] ChangePassword/ResetPassword: banners amber/blue + iconos circle-check/x

## WP3 — Solicitudes y calendario (← WP0)
- [ ] PendingRequestsPage: tabs caja + count rojo + search-box + avatar/depto
- [ ] Approve/Reject/Create modals: info-banners + barra footer + narrow/field-label.red
- [ ] Calendario: legend + cell-free gris + cell-assigned green-darker

## WP4 — Plazas/puestos, visitantes, auditoría (← WP0)
- [ ] ParkingSpaceFormModal + DeskFormModal: Toggle real + field-row + info-banner blue + narrow
- [ ] ParkingSpacesPage: columna titular + legend + activar/desactivar; DesksPage: legend
- [ ] Visitantes: search-box + info-banner blue + field-row + textarea notas
- [ ] Auditoría: pills por color de acción (2 rutas separadas)

## WP5 — Móvil y plano (← WP0)
- [ ] MyWeekPage: week-cards color + botones Solicitar/Liberar (plaza y puesto)
- [ ] ReleaseResourceModal: resumen + pill + info-banners
- [ ] Plano editor: marcadores neutros + botón guardar + banner ayuda
- [ ] Plano panel lateral: titular + chevron; datebar sin input duplicado; marcador 28px
- [ ] CreateRequestModal: banner rojo si 0 disponibles + iconos de recurso

## QA
- [ ] test-runner (cobertura ≥80%)
- [ ] verification-specialist PASS
- [ ] reality-checker READY (paridad visual ocular)
- [ ] PR Closes #81 + archive
