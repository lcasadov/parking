# Proposal: init-requests

## Why
Inicializar la capability **requests** en OpenSpec: dejar documentado como
contrato verificable el flujo de solicitud puntual de plaza (creación con
ventana temporal y unicidad, listados, cancelación y resolución
aprobar/rechazar por el administrador) antes de implementarlo. Es el flujo
central del producto y el que conecta disponibilidad, notificaciones y
auditoría.

## What Changes
- Se añade la capability `requests` con sus Requirements y escenarios BDD.
- Endpoints: `POST /requests`, `GET /requests/mine`, `GET /requests/pending`,
  `GET /requests/{id}`, `POST /requests/{id}/cancel`,
  `POST /requests/{id}/approve`, `POST /requests/{id}/reject`
  (ver `docs/openapi.yaml`).
- Máquina de estados `PENDING → APPROVED | REJECTED | CANCELLED`.
- Ventana de solicitud hoy..hoy+14 y unicidad `PENDING` por empleado/fecha.

## Capabilities
- `requests` (ADDED)

## Impact
- **Entidades**: `Request` (`requests`), `Employee`, `ParkingSpace`; consulta
  `FixedAssignment`, `Release`, `VisitorReservation` al validar disponibilidad
  (ver `docs/data-model.md` §3.5).
- **Seguridad**: RBAC `ADMIN`/`EMPLOYEE` con comprobación de objeto en
  recursos propios (BOLA, `request.employee_id == session.employee_id`); rate
  limiting de `POST /requests` (ver `docs/security-design.md`).
- **UI**: pantalla "Mis solicitudes" (EMPLOYEE), bandeja de pendientes y
  modales de aprobar/rechazar (ADMIN).

## Out of scope
- Cálculo interno de disponibilidad (lo aporta `availability-calendar`; aquí
  solo se consume).
- Envío de emails (lo aporta `notifications`; aquí solo se disparan los eventos).
- Solicitud de puestos de oficina y discriminador `resource_type` (lo aporta
  `generic-resource-refactor` / `desks`).
- Exportación de solicitudes (lo aporta `exports`).
