# Proposal: init-availability-calendar

## Why
Inicializar la capability **availability-calendar** en OpenSpec: dejar
documentado, como contrato verificable, el cálculo de disponibilidad de un
recurso para una fecha y las vistas de calendario (disponibilidad puntual,
semanal admin y "Mi Semana"). Es la lógica que `requests` reutiliza al validar
la aprobación y la base de toda la experiencia de consulta de huecos.

## What Changes
- Se añade la capability `availability-calendar` con sus Requirements y escenarios BDD (consulta-only).
- Endpoints: `GET /availability`, `GET /calendar/admin`, `GET /calendar/my-week` (ver `docs/openapi.yaml`).
- Regla de disponibilidad centralizada: activo + (sin `FixedAssignment` vigente o liberado) + sin `Request` `APPROVED` + (solo plazas) sin `VisitorReservation`.
- Restricción de privacidad: `getMyWeek` no expone nombres de otros empleados; `getAdminCalendar` solo `ADMIN`.

## Capabilities
- `availability-calendar` (ADDED)

## Impact
- **Entidades**: lectura de `ParkingSpace`, `FixedAssignment`, `Release`, `Request`, `VisitorReservation`, `Employee`. No escribe ninguna.
- **Seguridad**: `getAdminCalendar` restringido a `ADMIN` (403 para `EMPLOYEE`); `getMyWeek` filtra a recursos propios sin nombres ajenos (ver `docs/security-design.md`).
- **UI**: vista de disponibilidad por fecha, rejilla semanal admin y "Mi Semana"; el grueso de la UI llega con las capabilities de consumo.

## Out of scope
- Cualquier mutación de estado (asignar, liberar, solicitar, reservar) — vive en `fixed-assignments`, `releases`, `requests`, `visitors`.
- El plano de planta y los marcadores por puesto (`floor-plan`, alcance ampliado).
- La generalización a `BookableResource` (`generic-resource-refactor`); aquí el contrato se mantiene sobre plazas.
