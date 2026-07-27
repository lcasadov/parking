## Context

Cambio transversal de refinamiento del portal empleado + nueva capacidad admin de
reasignación/intercambio. Reutiliza al máximo lo existente (fixed-assignments, requests,
releases, floor-plan, batch release) para no tocar el esquema de BD.

## Goals

- Que liberar y volver a reservar recupere tu recurso fijo (predecible).
- Un flujo de ausencia rápido para soltar varios días de golpe.
- Portal empleado más claro (Mi Semana, Mis solicitudes, Mis sitios fijos) y sin ruido
  (Plano fuera, popover de perfil fuera, exportar fuera).
- Que el admin pueda reasignar/intercambiar recursos por fecha sin pasos tediosos.

## Decisions

### D1 — Auto-asignación: preferir el fijo propio
En `RequestService`, antes de `autoAssignParkingSpace`/`autoAssignDesk`, resolver el fijo
del empleado para el `dayOfWeek` de la fecha (`findByEmployeeIdAndResourceTypeAndDayOfWeekAndActiveTrue`).
Si existe y ese recurso está libre esa fecha (`isSpaceTakenForDate == false`), asignarlo
(salta la regla de categoría: es tu fijo). Si no, cae a la asignación por categoría actual.
Aplica a PARKING y DESK.

### D2 — Ausencia (empleado): liberación en lote
Modal en "Mis sitios fijos" con selección de días sueltos + rango. Se expande a fechas,
se descartan las que no tengan fijo ese día, y se libera por lote con `useBatchRelease`
(un release por recurso/fecha). Por defecto plaza+puesto, con toggle. Resumen previo y
toast de resultado. Sin email (auto-liberación propia).

### D3 — Mi Semana: mapa y parking
Botón "Mapa" en tarjetas de puesto (todas: Hoy/Mañana/Próximos) → `FloorPlanFocusModal`
enfocando el puesto asignado con resalte/parpadeo (respeta `prefers-reduced-motion`).
Botón "Ir al parking" solo en Hoy/Mañana → abre navegación externa (Google Maps) con la
dirección del parking configurada por el admin (system-settings). Si no existe el ajuste,
se añade a Ajustes admin.

### D4 — Mis solicitudes: meses + dos fechas + acción por estado
Selector de mes (mes actual por defecto, adelante/atrás sin tope). Backend `listMine`
acepta rango de fechas (`from`/`to`) sobre `requestedDate`; paginación 20/pág dentro del mes.
Dos columnas: "Día reservado" (`requestedDate`, principal) y "Solicitado el" (`createdAt`,
secundaria). Orden por `requestedDate` ascendente. Botón por estado (PENDING → "Cancelar
solicitud"; APPROVED → "Liberar") reutilizando `CancelRequestModal` (mode cancel/release)
y toast por estado/recurso. Se quitan exportar y "Nueva solicitud".

### D5 — Mis sitios fijos (read-only)
Renombrar la página; una fila por recurso (agrupando por `resourceId`) con sus días; icono
por tipo (P / ordenador); columna "Recurso". Sin "Liberar" por fila (la liberación puntual
vive en Mi Semana). Botón "Mapa" en filas de puesto. Botón estrella de ausencia arriba (D2).

### D6 — App shell
Renombrar marca a "Reservas" (sidebar + `<title>` + i18n). Cierre de sesión: la píldora de
perfil dispara un diálogo de confirmación (no el popover). Se elimina el popover redundante
(Idioma/Tema ya están en el pie del sidebar) y "Exportar mis datos".

### D7 — Admin: reasignar/intercambiar por fecha
Desde el modal de asignación de Ocupación: reasignar el recurso de una asignación de un día
e **intercambiar** entre dos empleados. Operación **atómica** (si un lado falla, no se aplica
ninguno) con validación de disponibilidad. **Email a ambos** afectados. Reutiliza la
infraestructura de requests/releases y notificaciones.

## Risks / Trade-offs

- La preferencia por el fijo propio cambia el resultado determinista de la auto-asignación:
  hay que actualizar/añadir tests para no romper los existentes.
- El swap atómico requiere transacción cuidadosa; si el email falla, la operación de datos
  ya está confirmada (email best-effort, no bloquea).
- Quitar el Plano del empleado: la orientación se cubre con el botón "Mapa" contextual.

## Migration Plan

Sin migración de datos. Cambios de código + i18n. El backend requiere recompilar (JDK 21)
y reiniciar el dev; ejecutar `mvn verify` para el gate de tests antes de PR.

## Open Questions

- Confirmar orden asc vs desc en Mis solicitudes (asumido ascendente).
- Confirmar existencia del ajuste "dirección del parking" en system-settings.
