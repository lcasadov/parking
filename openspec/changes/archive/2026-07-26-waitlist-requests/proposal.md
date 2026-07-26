## Why

En cuanto un día aparece "sin plazas", el empleado se queda sin salida: en modo **AUTOMÁTICO** `POST /requests` responde `409 NO_AVAILABILITY` y ni siquiera puede solicitarla; en **MANUAL** la solicitud sí queda pendiente, pero nada le explica por qué merece la pena esperar ni ocurre nada proactivo cuando una plaza se libera. En la práctica las plazas **se liberan a menudo** (un titular fijo que no viene, vacaciones, una solicitud aprobada que se cancela), pero esa disponibilidad sobrevenida no llega a quien la quería. Falta una **lista de espera**: poder solicitar aunque esté lleno, con expectativas claras, y recibir la plaza (o el aviso) cuando se libera.

## What Changes

- **Solicitar aunque esté lleno (opt-in).** `POST /requests` acepta `waitlist: true`. En **AUTOMÁTICO** sin hueco, en vez de `409`, se crea la solicitud `PENDING` marcada como *en lista de espera*; **sin** el opt-in sigue devolviendo `409 NO_AVAILABILITY` (compatibilidad). En **MANUAL** la solicitud se crea `PENDING` como hoy y se marca `waitlisted` cuando no había disponibilidad al crearla.
- **Estado y comunicación claros.** Nuevo flag `waitlisted` en la solicitud, expuesto en `RequestResponse` y en `MyWeekDay`. El empleado ve un aviso honesto cuando no hay hueco ("suelen liberarse: ausencias, vacaciones… apúntate y te avisamos") y un chip **"En lista de espera"** en Mi Semana y en Mis solicitudes. **No** se muestra posición numérica en la cola.
- **Promoción al liberarse un recurso.** Cuando una plaza/puesto queda libre para una fecha (cancelación de una `APPROVED`, o liberación de una asignación fija), el sistema atiende la lista de espera de ese **día y tipo**:
  - **AUTOMÁTICO:** auto-asigna al de **mayor categoría**; a igualdad de categoría, **orden de solicitud (FIFO)**; y notifica al empleado la asignación.
  - **MANUAL:** notifica a los administradores que se ha liberado un recurso con N en espera, para que asignen (el flujo de aprobación de pendientes ya existe).
- **Colas separadas por tipo de recurso** (plaza vs puesto). Sin límite de fechas (igual que las solicitudes actuales).

## Capabilities

### New Capabilities
- `request-waitlist`: cola de espera de solicitudes para días sin disponibilidad, con opt-in en la creación, marca de estado (`waitlisted`), y **promoción** al liberarse un recurso —auto-asignación por categoría/FIFO en modo AUTOMÁTICO; aviso al admin en modo MANUAL.

### Modified Capabilities
- `requests`: `POST /requests` admite `waitlist`; el `409 NO_AVAILABILITY` deja de ser terminal cuando el empleado opta por la espera; `Request`/`RequestResponse` ganan `waitlisted`.
- `employee-portal`: el modal de reserva comunica con honestidad la falta de hueco y ofrece apuntarse; Mi Semana y Mis solicitudes muestran el estado "en lista de espera".
- `notifications`: nuevos avisos de **promoción** (empleado: "se te ha asignado") y de **liberación con cola** (admin, modo MANUAL).

## Impact

- **Backend:** `RequestService` (creación con opt-in; motor de promoción), servicio/puerto de promoción enganchado a la **cancelación de aprobadas** (evento `RequestCancelledEvent` ya existente) y a la **liberación de fija** (se añade evento/hook); `Request` + `RequestEntity` + `RequestMapper` + `RequestResponse` (+`waitlisted`); `MyWeekDay` y su servicio (+`waitlisted`); `GlobalExceptionHandler` (el `409` pasa a condicional). Migración Flyway `V27` (`waitlisted`). **Reutiliza** la prioridad por categoría existente (auto-asignación). Actualiza `docs/openapi.yaml`.
- **Frontend:** `CreateRequestModal` (banner honesto + reintento con `waitlist:true` tras `409`), `MyRequestsPage` y `MyWeekPage` (chip "en espera"), tipos/hooks/API, i18n es/en, MSW.
- **Datos:** columna `waitlisted BIT NOT NULL DEFAULT 0` en `requests` (retro-compatible; sin backfill).
- **Docs:** `docs/openapi.yaml`, este change.

## Out of scope

- Posición numérica visible en la cola; variante "el primero que la pilla" (first-come); prioridad configurable distinta de la categoría.
- UI dedicada de gestión de la cola para el admin (en MANUAL reutiliza la aprobación de pendientes existente).
- Recordatorios/expiración automática de solicitudes en espera; puestos para visitantes; lista de espera para reservas de visitante.
