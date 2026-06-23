# Design: init-availability-calendar

## Context
La disponibilidad es una función pura del estado de varias entidades para una
fecha F. No hay una tabla "disponibilidad": se deriva en tiempo de consulta
cruzando `ParkingSpace`, `FixedAssignment`, `Release`, `Request` (`APPROVED`) y
`VisitorReservation`. Esta misma función se invoca desde `requests` al aprobar,
por lo que debe vivir en el dominio (puerto/servicio) y no en el controller.

## Goals
- Una única definición de disponibilidad reutilizable (consulta directa y validación de aprobación) para evitar divergencias.
- Vistas de calendario eficientes (una consulta por semana, no N por celda) que eviten N+1.
- Privacidad: "Mi Semana" nunca expone identidad de terceros; el calendario completo es exclusivo de `ADMIN`.

## Decisions
- **Disponibilidad como servicio de dominio** (`AvailabilityService`/`AvailabilityPort`): recibe fecha F y recurso(s), aplica las cuatro condiciones del README; reutilizado por `getAvailability` y por la aprobación de `requests`. Justificación: evita lógica duplicada y deriva divergente.
- **Consulta-only / sin transacción de escritura**: los tres endpoints son `GET` idempotentes; ninguna ruta muta estado. Justificación: separación clara de responsabilidades y testabilidad.
- **Estado por celda mapeado a enums del API** (`CalendarCellState` para admin: `ASSIGNED`/`RELEASED`/`REQUEST_PENDING`/`REQUEST_APPROVED`/`FREE`; `MyWeekDayState`: `ASSIGNED`/`RELEASED`/`REQUEST_PENDING`/`FREE`). Justificación: el contrato ya los define en `docs/openapi.yaml`.
- **Carga por semana en bloque** (recursos + asignaciones + releases + requests aprobadas + reservas del rango) y composición en memoria. Justificación: evita N+1 al pintar 7 días.
- **Filtrado de privacidad en `getMyWeek`** a nivel de servicio: solo recursos cuyo titular es el solicitante; no se serializa `employeeName`. Justificación: cumplir RGPD/`security-design.md`.
- **Reloj inyectable** (`ClockPort`) para `getMyWeek` sin `weekStart` (semana actual) y para que los tests fijen "hoy".

## Risks
- N+1 al construir el calendario semanal → mitigado con carga por rango y ensamblado en memoria.
- Divergencia entre la disponibilidad de consulta y la de aprobación de `requests` → mitigada compartiendo el mismo servicio de dominio.
- Fuga de identidad en "Mi Semana" → mitigada filtrando en el servicio y no exponiendo campos de empleado en el DTO de `MyWeekResponse`.

## Migration Plan
- **Sin migración de datos ni tablas nuevas**: la capability es consulta-only sobre tablas ya creadas por `parking-spaces`, `fixed-assignments`, `releases`, `requests` y `visitors` (ver `docs/data-model.md`).
- Conviene asegurar índices de apoyo a las consultas por fecha/recurso (FK + `date`/`day_of_week`) ya previstos en `data-model.md`; si faltaran, delegar a `database-optimizer` _[verificar con docs/data-model.md]_.
