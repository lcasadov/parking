# Proposal: init-visitors

## Why
Inicializar la capability **visitors** en OpenSpec: documentar como contrato
verificable la gestión de fichas de visitante (personas externas sin cuenta) y
las reservas de plaza puntuales que el `ADMIN` crea para ellas. Estas reservas
ocupan plazas en el cálculo de disponibilidad, por lo que su contrato debe quedar
fijado antes de implementar `availability-calendar`.

## What Changes
- Se añade la capability `visitors` con sus Requirements y escenarios BDD.
- Endpoints: `listVisitors`, `createVisitor`, `getVisitor`, `updateVisitor`
  (fichas) y `listVisitorReservations`, `createVisitorReservation`,
  `cancelVisitorReservation` (reservas). Ver `docs/openapi.yaml`.
- `nationalId` único → 409; reserva sobre plaza ya ocupada → 409; anulación solo
  de reservas futuras → 400 en pasadas.

## Capabilities
- `visitors` (ADDED)

## Impact
- **Entidades**: `Visitor`, `VisitorReservation` (nuevas), `ParkingSpace` y
  `Employee` (referenciadas). Ver `docs/data-model.md` §3.6 y §3.7.
- **Seguridad**: todas las operaciones restringidas a `ADMIN` (ver
  `docs/security-design.md`).
- **Disponibilidad**: una `VisitorReservation` marca una plaza como no disponible
  para `reservationDate` (solo plazas, nunca puestos).
- **UI**: pantallas de admin para CRUD de visitantes y gestión de reservas.
- **Sin email**: el visitante no tiene cuenta; ninguna operación dispara
  `notifications`.

## Out of scope
- Reserva de visitante sobre puestos (`Desk`) — no aplica nunca.
- Flujo de aprobación de reservas — el admin las crea directamente, sin estados.
- Notificaciones por email al visitante.
- Cálculo de disponibilidad en sí (lo aporta `availability-calendar`; aquí solo se
  garantiza la ocupación de la plaza).
