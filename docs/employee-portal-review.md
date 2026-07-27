# Revisión íntegra del portal de EMPLEADO — para repasar

> Barrido de todas las pantallas, modales y funcionalidades del empleado. Cada
> punto marcado para decidir mañana si se **arregla**, se **implementa** o se
> **descarta**. Prioridad: 🔴 alta · 🟠 media · 🟢 baja/idea.

## 0. Estado de lo implementado esta noche (change `reservas-employee-admin-reassign`)
- ✅ Marca "Reservas"; cierre de sesión con confirmación (sin popover); "Exportar mis datos" fuera.
- ✅ "Plano" fuera del nav del empleado; acceso al plano vía botón "Mapa" contextual.
- ✅ Mi Semana: navegación por semanas en "Próximos días"; botón "Mapa" (puesto) e "Ir al parking" (plaza, Hoy/Mañana); reservar recurso liberado.
- ✅ Mis solicitudes: sin exportar/nueva; acción por estado (Cancelar solicitud / Liberar) con modal y toast coherentes; dos fechas.
- ✅ Mis sitios fijos: read-only + botón estrella de ausencia (liberación en lote por días/rango).
- ✅ Iconos: P (parking) y ordenador (puesto). Toasts opacos y saturados.
- ⏳ Backend en curso (agente): recuperar tu fijo al re-reservar (A), reasignar/intercambiar admin (B), filtro por mes en "mis solicitudes" (D), dirección del parking configurable.

---

## 1. Mi Semana (pantalla índice)
- 🔴 **Aviso "se te reasignará tu fijo"** en el modal cuando reservas un día con tu fijo libre (depende del backend A; ya delegado).
- 🟠 **Selector de mes / salto rápido**: hoy navegas semana a semana; para reservar a 3 meses vista son muchos clics. Idea: un selector de mes o "ir a fecha".
- 🟠 **Estado de carga por semana**: al navegar semanas se ve el spinner global; sería más fino un skeleton de las tarjetas.
- 🟢 **Weekend/festivos**: los findes aparecen como días normales "Sin plaza/puesto". ¿Ocultar findes o marcarlos como no laborables? (requiere config de calendario laboral).
- 🟢 **Resumen semanal**: un contador arriba ("Esta semana: 3 días con plaza, 2 con puesto").
- 🟢 **Icono de "hoy"** más marcado en la tira de próximos días.

## 2. Nueva reserva (modal)
- 🟠 **Puntos en el calendario**: marcar en el calendario los días donde YA tienes reserva/fijo (para no tener que abrir día a día).
- 🟠 **Recordar última selección**: si sueles pedir plaza+puesto, preseleccionar según histórico.
- 🟢 **Reserva recurrente / por rango**: "reservar plaza todos los martes" o un rango de días (hoy es día a día).
- 🟢 **Feedback de auto-asignación**: mostrar qué plaza/puesto te va a tocar antes de enviar (si el backend lo permite).
- 🟢 Revisar en móvil que el scroll al pulsar día lleva bien a las tarjetas (implementado; validar en dispositivo real).

## 3. Mis solicitudes
- 🔴 **Selector de mes** (depende de backend D, delegado): hoy solo pagina 20/pág sin horizonte.
- 🟠 **Filtro por estado** (pendiente/aprobada/cancelada) y por tipo (plaza/puesto).
- 🟠 **Vista tarjeta en móvil**: la tabla responsive con data-label funciona, pero unas tarjetas nativas se leerían mejor.
- 🟢 **Acción "reenviar aviso"** ya existe en el banner de pendientes; revisar visibilidad.
- 🟢 **Exportar** se quitó del empleado; confirmar que no se echa en falta (si acaso, un "descargar mi historial" discreto).

## 4. Mis sitios fijos
- 🟠 **Confirmación/He revisado el resumen de ausencia**: el modal libera al confirmar; añadir un paso de confirmación final si son muchos días.
- 🟠 **Ver liberaciones activas**: la pestaña "Liberaciones" sigue; valorar fusionarla como sección o mostrar un badge "tienes 2 días liberados".
- 🟢 **Calendario multi-select** en el modal de ausencia en vez de inputs de fecha (más visual). Hoy: día suelto + rango con inputs nativos (mobile-friendly).
- 🟢 **Deshacer ausencia**: poder recuperar un día liberado desde aquí (hoy se hace desde Mi Semana).

## 5. Modales de liberar / cancelar
- 🟢 Coherencia de textos ya alineada (cancelar solicitud vs liberar). Revisar el modal de liberación fija (ReleaseResourceModal) por si conviene el mismo lenguaje.
- 🟢 El toast de liberación ahora es específico por recurso; validar en todos los flujos.

## 6. Plano / Mapa del puesto
- 🟠 **Rendimiento móvil del plano**: el focus modal carga el plano completo; en móvil validar zoom/scroll y que el asiento resaltado quede centrado.
- 🟢 **Botón "cómo llegar a mi puesto"**: además de resaltar, indicar planta/zona en texto.

## 7. App shell / navegación / sesión
- 🟠 **Manejo de sesión caducada (401)**: al expirar, redirigir a login con aviso claro; revisar que no se pierda el trabajo en curso. (Sesión ampliada a 30 días — pendiente reiniciar backend.)
- 🟢 **Accesibilidad**: revisar foco visible y navegación por teclado en el nuevo calendario, tarjetas de recurso y chips de ausencia.
- 🟢 **Idioma/tema** en el pie del sidebar: en móvil (drawer) confirmar que se ven y usan bien.

## 8. Móvil (transversal) — validar en dispositivo real
- 🔴 Repaso completo en móvil de: Mi Semana (tarjetas hero + navegación semanas), modal de reserva (calendario + tarjetas + scroll), Mis solicitudes (tabla→tarjeta), Mis sitios fijos (CTA ausencia + modal), toasts.
- 🟠 Verificar que ninguna fila/tabla desborda horizontalmente (se aplicaron `minmax(0,1fr)` y `min-width:0`, pero conviene comprobación visual).

## 9. Deuda de tests (NO de este change)
- 🟠 29 tests fallando en 16 ficheros por reescrituras de sesiones anteriores no reconciliadas: VisitorsPage, DesksPage, FloorPlanPage, OccupancyActionable, AdminCalendarPage, VisitorReservationModal, App, Sidebar (logo), theme (switch), y 7 de RBAC. Requiere un pase dedicado de actualización de tests (no afecta a build ni a los componentes de este change).

## 10. Ideas "rompedoras" (backlog)
- 🟢 **Check-in del día**: notificación/ботón "¿vienes hoy?" que confirme o libere automáticamente.
- 🟢 **Sugerencia inteligente**: "sueles venir L-X-V, ¿reservo la semana?".
- 🟢 **Compañeros**: ver qué días viene tu equipo (opt-in) para coordinar presencia.
- 🟢 **Widget/atajo** de "mi plaza de hoy" para la home / PWA.
