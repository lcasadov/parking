# Proposal: desk-support-admin-flows

## Why
El backend ya soporta `resourceType = DESK` en solicitudes, aprobación y asignación
fija (modelo genérico de recurso, C1/C2), pero la **UI de administración solo maneja
plazas**: (1) la bandeja de solicitudes no distingue plaza de puesto; (2) el modal de
aprobación siempre ofrece **plazas** aunque la solicitud sea de puesto (y solo puede
asignar una plaza); (3) la asignación fija solo permite plaza. Esto rompe el uso real:
un empleado solicita un puesto y el admin no puede resolverlo correctamente.

## What Changes
- **Bandeja de solicitudes pendientes**: mostrar el **tipo de recurso** (Plaza/Puesto) por fila.
- **Modal de aprobación**: adaptarse al `resourceType` de la solicitud → para `DESK` lista **puestos disponibles** (`GET /availability?date&resourceType=DESK`) con etiqueta "Puesto disponible"; para `PARKING`, plazas. El título refleja el tipo ("Solicitud de plaza" / "Solicitud de puesto").
- **Asignación fija**: permitir asignar un **puesto** (además de plaza) con sus días de la semana, enviando `resourceType` en `PUT /fixed-assignments/employee/{id}`.
- **Mis solicitudes** (empleado): mostrar el tipo de recurso en el listado.

## Capabilities
- `requests` (MODIFIED — la resolución admin distingue y asigna según el tipo de recurso)
- `desks` (MODIFIED — la asignación fija y la resolución de puestos se exponen en la UI admin)

## Impact
- **Frontend**: `PendingRequestsPage`, `ApproveRequestModal`, la pantalla/modal de asignaciones fijas, `MyRequestsPage`, hooks/API de disponibilidad y asignaciones. Solo presentación/UX + wiring; **sin cambio de contrato** (los endpoints ya aceptan `resourceType`/`resource_id`).
- **Backend**: sin cambios.

## Out of scope
- Paridad visual del plano (imagen real) → change aparte.
- Cambios de contrato o backend.
