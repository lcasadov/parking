# Tasks — visitor-reservations

As-built: reconstrucción del change tras la implementación (commits `aaafb9b`, `e9ce05a`, `ed511ba`, `d87ad87`, `68d3b29`, `b6bcac3`, `3266e1d`). Orden por fases del design (A generalización backend → compat frontend → B reflejo como ocupado → C+D unificación del wizard → UI de Visitantes).

## 1. Fase A — Modelo genérico plaza/puesto + disponibilidad (backend)

- [x] 1.1 Migración `V25__visitor_reservations_generic_resource.sql`: `parking_space_id` → `resource_type` + `resource_id`, backfill `resource_type = 'PARKING'`, índice único `(resource_type, resource_id, reservation_date)` sustituyendo al de plaza
- [x] 1.2 Generalizar `VisitorReservation` (entidad), `VisitorReservationCreateRequest`/`VisitorReservationResponse` (DTOs) y `VisitorReservationRepository` a `resourceType`/`resourceId`
- [x] 1.3 `VisitorReservationService`: validar disponibilidad del recurso por tipo (plaza o puesto) — activo, sin asignación fija vigente no liberada, sin solicitud `APPROVED`, sin otra reserva de visitante
- [x] 1.4 `AvailabilityService.availabilityForDate`/`freeParkingSpacesForDate`/`isSpaceTakenForDate`: descontar reservas de visitante del tipo de recurso correcto (antes solo `PARKING`)
- [x] 1.5 Generalizar la regla del índice único en `GlobalExceptionHandler` (409) y el evento de auditoría
- [x] 1.6 Tests unit + IT actualizados (`VisitorReservationServiceTest`, `VisitorReservationIT`, `VisitorReservationControllerTest`, `AvailabilityServiceTest`, `VisitorManagementIT`, `RequestManagementIT`, `RetentionPurgeIT`, `NotificationOutboxIT`, `AvailabilityCalendarIT`, `VisitorJsonTest`)

## 2. Compatibilidad transitoria del frontend (mientras se completaba el backend)

- [x] 2.1 `VisitorReservationModal.tsx`/`VisitorReservationsPanel.tsx`/mocks/`types/visitor.ts`: enviar y mostrar `resourceType: 'PARKING'` + `resourceId` (mapeado desde la plaza elegida) en vez de `parkingSpaceId`

## 3. Fase B — Reflejar la reserva de visitante como ocupada en planos y ocupación

- [x] 3.1 `CalendarCellState`: añadir `VISITOR_RESERVATION`
- [x] 3.2 `OccupancyOrigin`: añadir `VISITOR_RESERVATION`
- [x] 3.3 `FloorPlanQueryService`: un puesto con reserva de visitante para la fecha sale `ASSIGNED` (ocupado, tercero sin identidad), no `FREE`
- [x] 3.4 `AvailabilityService` (calendario semanal `GET /calendar/admin`): celda `VISITOR_RESERVATION` con el nombre del visitante como ocupante, sin `employeeId`
- [x] 3.5 `AvailabilityService.occupancyForDate` (`GET /occupancy?date=`): origen `VISITOR_RESERVATION` con el nombre del visitante como ocupante
- [x] 3.6 Frontend: `types/calendar.ts`, `types/occupancy.ts`, `utils/adminCalendar.ts` (contado en "ocupados", sin acción de liberar), `utils/calendar.ts`, `utils/calendarLegend.ts` (color de ocupado), `i18n/locales/{es,en}.ts` (etiqueta "Visitante"/"Visitor"), `pages/AdminCalendarPage.tsx`
- [x] 3.7 Tests backend actualizados (`AvailabilityServiceTest`, `FloorPlanQueryServiceTest`)

## 4. Fases C+D — Asistente de reserva unificado (empleado/visitante) + entrada desde Visitantes

- [x] 4.1 `wizardTypes.ts`: `BeneficiaryType` (`EMPLOYEE`|`VISITOR`), `visitorId` en `WizardState`
- [x] 4.2 `StepEmployee.tsx`: conmutador Empleado|Visitante + buscador común (nombre/documento para visitante vía `useVisitorsQuery`)
- [x] 4.3 `hooks/useVisitorBooking.ts` (nuevo): reserva de visitante por lote vía `POST /visitor-reservations` por fecha (`Promise.allSettled`), invalidación de `visitor-reservations`/`calendar`/`occupancy`/`floor-plan`, sin envío de email
- [x] 4.4 `ReservationWizard.tsx`: bifurcar la confirmación — EMPLEADO → `POST /requests/admin` (con email); VISITANTE → `useVisitorBooking` (sin email)
- [x] 4.5 `LocationParking.tsx`/`StepLocation.tsx`/`StepLocationPerDay.tsx`: ocultar la auto-asignación por categoría para visitantes (`allowAuto={state.beneficiaryType === 'EMPLOYEE'}`)
- [x] 4.6 `StepSummary.tsx`: aviso "sin email" para visitantes (`noEmailNotice`) en vez del aviso de notificación; fila de beneficiario con icono/etiqueta distintos
- [x] 4.7 `StepResult.tsx`/`WizardRail.tsx`: reflejar el nombre del visitante como beneficiario (`summaryVisitor`, sin mención a notificación)
- [x] 4.8 i18n (`es.ts`/`en.ts`): claves `wizard.beneficiary.*`, `wizard.summary.visitor`/`noEmailNotice`, `wizard.result.summaryVisitor`
- [x] 4.9 `VisitorReservationsPanel.tsx`: "Nueva reserva" abre el asistente unificado preseleccionado en Visitante (`initialBeneficiaryType="VISITOR"`)
- [x] 4.10 `VisitorsPanel.tsx`: "Reservar" por fila abre el asistente preseleccionado en Visitante y en el visitante de esa fila (`initialVisitorId`)
- [x] 4.11 Build FE OK, lint 0 (verificado en los commits de esta fase)

## 5. UI de Visitantes (ajustes sin cambio de contrato)

- [x] 5.1 Conmutador grande (`SectionSwitch`) para Fichas/Reservas futuras en vez de tabs pequeñas
- [x] 5.2 Botón "Nuevo…" anclado a la derecha (`.toolbar-end`) en ambas sub-vistas
- [x] 5.3 Buscador de fichas pegado a la izquierda (anula `margin-left:auto` global en `styles/components.css`)
- [x] 5.4 Retirar el aviso de "sin email" de la pantalla principal de Visitantes (ya se muestra al crear la reserva)

## 6. Pendiente (gaps identificados, no ejecutados en estos commits)

- [ ] 6.1 Actualizar `docs/openapi.yaml` (`VisitorReservationCreateRequest`/`VisitorReservation`) de `parkingSpaceId` a `resourceType`/`resourceId`
- [ ] 6.2 Retirar `VisitorReservationModal.tsx` y `VisitorReservationModal.test.tsx` (código muerto: ya no se invoca desde ninguna pantalla)
- [ ] 6.3 Añadir selector de tipo de recurso al filtro de `VisitorReservationsPanel` (hoy solo filtra por `resourceId` numérico, sin distinguir plaza/puesto)
