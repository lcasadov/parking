## 1. App shell — marca "Reservas" y perfil (Feature F, G)

- [x] 1.1 Renombrar la marca del sidebar a "Reservas" (subtítulo "ALEATICA").
- [x] 1.2 Cambiar el `<title>` del documento y textos i18n de marca (es/en) donde ponga "parking".
- [x] 1.3 Cerrar sesión: la píldora de perfil abre un diálogo de confirmación (Cancelar / Cerrar sesión).
- [x] 1.4 Quitar el popover de perfil redundante (idioma/tema ya están en el pie del sidebar).
- [x] 1.5 Quitar "Exportar mis datos" del perfil.

## 2. Navegación empleado — quitar Plano (Feature E/nav)

- [x] 2.1 Eliminar la sección "Plano" de la navegación del empleado (nav; la ruta se mantiene).

## 3. Mi Semana — botones mapa/parking (Feature C)

- [x] 3.1 Botón "Mapa" en tarjetas de puesto (Hoy, Mañana y Próximos) → `FloorPlanFocusModal` con el puesto resaltado/parpadeando (`DeskMapButton`).
- [x] 3.2 Animación de resalte respeta `prefers-reduced-motion` (heredado de FloorPlanFocusModal/FloorPlanSurface).
- [x] 3.3 Botón "Ir al parking" en tarjetas de plaza SOLO en Hoy y Mañana → Google Maps con la dirección del parking (`ParkingDirectionsButton`, se muestra si hay dirección).
- [~] 3.4 Ajuste "dirección del parking" en Ajustes admin: FE preparado (settings.parkingAddress opcional); backend (campo + endpoint + form admin) DELEGADO al agente backend.

## 4. Mis solicitudes — limpieza + meses + dos fechas + acción por estado (Feature D)

- [x] 4.1 Quitar exportar (ExportMenu) y el botón "Nueva solicitud" para el empleado.
- [ ] 4.2 Selector de mes (mes actual por defecto; adelante/atrás sin tope; estado vacío por mes). [PENDIENTE — requiere 4.3]
- [ ] 4.3 Backend: `listMine` acepta rango de fechas (from/to) sobre `requestedDate` + query params; mantener paginación. [PENDIENTE backend]
- [x] 4.4 Mostrar dos fechas: "Día reservado" (requestedDate, principal) y "Solicitado el" (createdAt, secundaria). (Orden por mes llegará con 4.2/4.3.)
- [x] 4.5 Acción por estado: PENDING → "Cancelar solicitud"; APPROVED → "Liberar"; botón pegado a la derecha.
- [x] 4.6 Modal (reutilizar CancelRequestModal mode cancel/release) + toast por estado/recurso.

## 5. Mis sitios fijos — read-only + ausencia (Feature E)

- [x] 5.1 Renombrar "Mis plazas" → "Mis sitios fijos"; página informativa (read-only).
- [x] 5.2 Una fila por recurso (groupFixedAssignments) con sus días; icono por tipo (ResourceIcon).
- [x] 5.3 Quitar el "Liberar" por fila; botón "Mapa" en filas de puesto.
- [x] 5.4 Botón estrella de ausencia arriba (`absence-cta`).
- [x] 5.5 Modal de ausencia (`AbsenceReleaseModal`): días sueltos + rango; toggle plaza/puesto (ambos por defecto).
- [x] 5.6 Solo libera lo que existe ese día (buildReleaseItems por weekday); resumen previo (InfoBanner).
- [x] 5.7 Liberar en lote (Promise.allSettled sobre createRelease); toast de resultado (sin email).

## 6. Auto-asignación — preferir el fijo propio (Feature A)

- [ ] 6.1 Backend: en `RequestService`, preferir el recurso fijo del empleado (por dayOfWeek) si está libre, antes de la asignación por categoría (PARKING y DESK).
- [ ] 6.2 Frontend: avisar en la tarjeta del modal cuando ese día tengas tu fijo libre ("Se te reasignará tu puesto fijo D-01").
- [ ] 6.3 Tests backend: recupera su fijo / fijo ocupado → categoría / sin fijo → categoría.

## 7. Admin — reasignar e intercambiar por fecha (Feature B)

- [ ] 7.1 Backend: endpoint de reasignación de recurso por fecha (valida disponibilidad, 409 si ocupado, auditoría con admin).
- [ ] 7.2 Backend: endpoint de intercambio (swap) atómico entre dos empleados para una fecha.
- [ ] 7.3 Backend: email a ambos afectados (best-effort, no revierte la operación).
- [ ] 7.4 Frontend: en el modal de asignación de Ocupación, acciones de reasignar e intercambiar.
- [ ] 7.5 Tests: swap correcto, swap atómico ante fallo, reasignación a recurso ocupado (409).

## 8. QA y cierre

- [x] 8.1 Frontend: `npm run lint && npm run build` sin errores; tests de los componentes de este change verdes (SidebarUserCard, MyRequestsPage, MyFixedAssignmentsPage, CreateRequestModal, MyWeekPage — 25 tests reconciliados).
- [ ] 8.2 Backend (JDK 21): `mvn verify` verde — DELEGADO al agente backend (A, B, D-filter, parkingAddress).
- [x] 8.3 i18n es/en completos para todos los textos nuevos del frontend implementado.
- [ ] 8.4 Actualizar este tasks.md y archivar el change tras merge.
- [ ] 8.5 DEUDA PREEXISTENTE de tests (NO de este change): 29 fallos en 16 ficheros
      (VisitorsPage, DesksPage, FloorPlanPage, OccupancyActionable, AdminCalendarPage,
      VisitorReservationModal, App, Sidebar[logo], theme[switch], rbac×7) por reescrituras
      de sesiones anteriores no reconciliadas con sus tests. Requiere un pase dedicado.

- [x] 6.2 Aviso en el modal "Se te reasignará tu sitio fijo" (frontend, `shouldShowFixedHint`).

## Pendiente por dependencia del backend (agente en curso)
- [ ] 4.2/4.3 Selector de mes en Mis solicitudes (requiere filtro from/to en listMine).
- [ ] 7.4 Frontend admin: reasignar/intercambiar en el modal de Ocupación (contra endpoints del agente).
- [ ] Form admin de "dirección del parking" en Ajustes (contra el campo del agente).
