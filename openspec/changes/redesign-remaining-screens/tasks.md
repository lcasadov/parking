# Tasks — redesign-remaining-screens

> Requiere `redesign-design-system` implementado (tokens + shell). Todos los patrones se
> construyen SOBRE esos tokens; prohibido hex sueltos. Diff por archivo antes de aplicar.

## 1. Patrones reutilizables (primero — el resto los consume)
- [ ] 1.1 `PageHeader` (eyebrow uppercase + título serif + descripción + slot de acciones)
- [ ] 1.2 `Toolbar` (buscador con icono ti-search, filtros, botón primario)
- [ ] 1.3 `DataTable` (cabecera labels uppercase, filas, avatar de color, paginación)
- [ ] 1.4 Estados de tabla: carga (skeleton), vacío (mensaje + acción), error (reintentar)
- [ ] 1.5 `Tabs` de filtro (con contador opcional en la pestaña activa)
- [ ] 1.6 `StatusPill` / `Badge` (estado y categoría) mapeados al color por estado del contract
- [ ] 1.7 `Modal` (cabecera, cuerpo, footer de acciones) + variante destructiva
- [ ] 1.8 `Banner` informativo (azul/verde/rojo) reutilizable en modales y formularios
- [ ] 1.9 `Field` (label uppercase + input/select/textarea + hint + error), `Toggle`,
      tarjetas de día seleccionables, checklist de política de contraseña
- [ ] 1.10 `AuthCard` (tarjeta centrada con logo)
- [ ] 1.11 Patrón móvil: cabecera compacta, tarjeta de día, botonera inferior, lista de recursos

## 2. Pantallas de administración (tabla/toolbar)
- [ ] 2.1 Empleados y plaza fija (chips de días L·M·X·J·V)
- [ ] 2.2 Gestión de plazas (tarjeta config total + tabla + pill activa/inactiva)
- [ ] 2.3 Gestión de puestos (categoría STANDARD/EXECUTIVE + activación)
- [ ] 2.4 Visitantes y reservas (pestañas Fichas/Reservas)
- [ ] 2.5 Disponibilidad por fecha (selector + contador + tabla)
- [ ] 2.6 Liberación administrativa (tabla + acción Nueva liberación)
- [ ] 2.7 Auditoría (rango de fechas + exportar + tabla)
- [ ] 2.8 Accesos / logs de login (filtros OK/KO + tabla)
- [ ] 2.9 Editor de posiciones del plano (marcadores arrastrables + panel de coords)

## 3. Modales de administración
- [ ] 3.1 Edición de empleado (pestañas DETALLES/PLAZA FIJA/HISTÓRICO)
- [ ] 3.2 Aprobar solicitud (campos solo lectura + selector de plaza + banner)
- [ ] 3.3 Rechazar solicitud (variante destructiva + motivo + comentario)
- [ ] 3.4 Alta/edición de plaza (identificador + toggle activa)
- [ ] 3.5 Modal de puesto (número + categoría + activación)
- [ ] 3.6 Reserva de visita (visitante + fecha + plaza + banner)
- [ ] 3.7 Reset administrativo de contraseña (contraseña temporal + copiar)

## 4. Portal del empleado
- [ ] 4.1 Mi semana (tarjetas por día + botonera Solicitar/Liberar)
- [ ] 4.2 Mis solicitudes (tabla + badges recurso/estado + cancelar)
- [ ] 4.3 Mis asignaciones fijas (chips de días + liberar)
- [ ] 4.4 Mis liberaciones (tipo VOLUNTARY/administrativa + anular)
- [ ] 4.5 Solicitud unificada (bloques plaza/puesto + estados error/validación)
- [ ] 4.6 Solicitar plaza / Liberar mi plaza (móvil, marcos de teléfono)
- [ ] 4.7 Plano empleado escritorio (panel de disponibles) y móvil (pinch-zoom)

## 5. Auth y preferencias
- [ ] 5.1 Cambiar contraseña (checklist de política)
- [ ] 5.2 Preferencias (popover de usuario: ES/EN + claro/oscuro + logout)
- [ ] 5.3 Sesión expirada (modal de re-autenticación)

## 6. Tema oscuro
- [ ] 6.1 Definir la variante oscura de todos los tokens (`theme-dark`)
- [ ] 6.2 Verificar contraste y legibilidad en tablas, pills, modales y plano

## 7. Verificación
- [ ] 7.1 Confirmar que NINGUNA pantalla cambia datos/endpoints/lógica
- [ ] 7.2 Todas las pantallas usan los patrones §1 (sin estilos ad-hoc ni hex sueltos)
- [ ] 7.3 Recorrer las 32 pantallas y confirmar aspecto homogéneo (claro y oscuro)
- [ ] 7.4 Marcar tasks; diff por archivo antes de aplicar
