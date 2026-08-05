## Context

`restructure-admin-workflows` (change activo, sin archivar) dejó constancia de que "puestos para visitantes" era una decisión de negocio pendiente porque `visitor_reservations` solo conocía `parking_space_id`. Este change ejecuta esa generalización siguiendo el mismo patrón de recurso genérico que `generic-resource-refactor` (`V12__generic_resource_refactor.sql`) ya aplicó a `requests`, `fixed_assignments` y `releases`: sustituir la columna específica por `resource_type` + `resource_id`.

Restricciones del proyecto: prosa de negocio en español, identificadores de código en inglés; backend Spring Boot 3.3 / Java 21 / WAR sobre Tomcat, frontend React 18 + Vite con design system propio; SQL Server 2022 con Flyway como dueño del esquema; Quality Gate obligatorio (cobertura ≥80% líneas / ≥75% branches, 0 violations Sonar nuevas).

## Goals / Non-Goals

**Goals:**
- Generalizar `visitor_reservations` a `resource_type` + `resource_id` (plaza o puesto), preservando los datos existentes.
- Que la reserva de visitante cuente en la disponibilidad y se refleje como ocupada en el plano de puestos, el calendario semanal y la ocupación por fecha, para ambos tipos de recurso.
- Unificar el asistente de reserva del ADMIN para que sirva tanto a EMPLEADO (con email) como a VISITANTE (sin email), eliminando el modal dedicado.
- Mantener intactas las reglas ya vigentes: solo `ADMIN`, anulación exclusiva de reservas futuras, unicidad recurso+fecha.

**Non-Goals:**
- Retirada física de `VisitorReservationModal.tsx` y su test (queda como código muerto sin invocarse; no forma parte de este change).
- Actualización de `docs/openapi.yaml` al contrato `resourceType`/`resourceId` (sigue documentando `parkingSpaceId`; gap identificado, no corregido en estos commits).
- Selector de tipo de recurso en el listado/filtro de `VisitorReservationsPanel` (sigue filtrando solo por `resourceId` numérico).
- Notificaciones o plantillas de email para visitantes (decisión ya tomada: nunca se les notifica).

## Decisions

**D1. Generalizar `visitor_reservations` reutilizando el patrón de V12, no una tabla/entidad nueva.**
`ALTER TABLE` añade `resource_type` (default `'PARKING'` para backfill) y `resource_id` (copiado de `parking_space_id`), retira la FK/índice/columna específicos de plaza y crea el índice único `(resource_type, resource_id, reservation_date)`. Alternativa descartada: una tabla `visitor_desk_reservations` paralela — duplicaría servicio, DTOs, auditoría y las comprobaciones de disponibilidad que ya existen para plaza.

**D2. La reserva de visitante se refleja como ocupación de "tercero sin identidad" en el plano y como "visitante nombrado" en calendario/ocupación.**
En el plano de puestos (`FloorPlanQueryService`) el puesto reservado sale `ASSIGNED` igual que si lo tuviera un empleado — el empleado que consulta el plano no necesita saber que es un visitante, solo que no está libre. En el calendario semanal de Ocupación y en "Ocupación por fecha" (vistas exclusivas de `ADMIN`/`AGENCIA`), en cambio, el nombre del visitante SÍ se muestra como ocupante (`CalendarCellState.VISITOR_RESERVATION` / `OccupancyOrigin.VISITOR_RESERVATION`), sin `employeeId` y sin acción de liberar (la reserva de visitante se anula, no se libera).

**D3. El asistente de reserva se unifica en vez de generalizar el modal de visitante por separado.**
Ya existía un asistente de reserva multi-paso para el ADMIN (herencia de `restructure-admin-workflows`); en vez de replicarle la lógica de disponibilidad/fechas/ubicación a `VisitorReservationModal`, se añade un paso de beneficiario (Empleado|Visitante) al asistente existente y se bifurca solo la llamada de confirmación (`POST /requests/admin` vs `POST /visitor-reservations`, con o sin email). Esto evita mantener dos flujos de selección de fecha/ubicación en paralelo. El modal de visitante queda sin invocarse (no se retira físicamente en este change, ver Non-Goals).

**D4. Sin auto-asignación por categoría para visitantes.**
La auto-asignación de plaza (`LocationParking`, prop `allowAuto`) depende de la categoría del empleado (CEO…Empleado); un visitante no tiene categoría, así que el paso de ubicación oculta la tarjeta "asignación automática" cuando `beneficiaryType === 'VISITOR'` y obliga a elegir un recurso concreto en todas las fechas.

**D5. Nunca se notifica por email a un visitante.**
`useVisitorBooking` no dispara ningún email (a diferencia de `POST /requests/admin`, que sí lo hace); el resumen del wizard (`StepSummary`) lo advierte explícitamente ("Reserva de visitante: no se envía ningún email") en vez de mostrar el aviso de notificación, y el resultado final usa una cadena i18n distinta (`summaryVisitor`) sin mencionar notificación.

## Risks / Trade-offs

- **`docs/openapi.yaml` desactualizado** → el schema publicado (`parkingSpaceId`) no coincide con el contrato real desde V25 (`resourceType`/`resourceId`). Riesgo de que un consumidor externo de la API se guíe por el spec incorrecto. Mitigación pendiente: tarea de sincronización en `tasks.md` (no ejecutada en estos commits).
- **Reserva de visitante no transaccional por lote** (`useVisitorBooking` usa `Promise.allSettled`) → una reserva multi-fecha puede quedar parcialmente creada si alguna fecha ya no está disponible; el wizard reporta el resultado por fecha (mismo patrón que la reserva de empleado), no es una regresión de este change.
- **`VisitorReservationModal.tsx` queda como código muerto** → mantiene su test verde pero ya no se importa desde ninguna pantalla; riesgo de confusión para quien navegue el código. Mitigación: registrado como Non-Goal/pendiente en `tasks.md`.
- **Migración `V25` retira la FK de `parking_space_id`** → `resource_id` queda sin FK (igual que `requests`/`fixed_assignments`/`releases` tras V12, por ser polimórfico); la integridad referencial la garantiza la capa de aplicación (`loadResourceActive`), no la base de datos.

## Migration Plan

1. `V25__visitor_reservations_generic_resource.sql` (Flyway): backfill `resource_type = 'PARKING'` + `resource_id = parking_space_id` antes de retirar la columna/FK/índice antiguos; sin ventana de mantenimiento especial (no hay usuarios en producción).
2. Orden de entrega ya ejecutado: (fase A) modelo genérico + disponibilidad backend → (compat) frontend adapta el flujo existente a `resourceType`/`resourceId` mientras se completaba el backend → (fase B) reflejo como ocupado en plano/calendario/ocupación → (fases C+D) unificación del wizard + entrada desde Visitantes.
3. Rollback: al ser feature de una rama sin usuarios en producción, revertir el merge; sin estado persistente que deshacer.
4. Pendiente (no ejecutado): actualizar `docs/openapi.yaml` al contrato `resourceType`/`resourceId`; retirar `VisitorReservationModal.tsx` y su test si se confirma que no se reutilizará.
