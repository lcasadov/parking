## Why

Hoy los vehículos de empleado (capacidad `employee-vehicles`) solo los gestiona el `ADMIN` desde el formulario de empleado. El empleado no puede declarar por sí mismo con qué vehículos irá a la oficina. Se quiere un **self-service**: que cada empleado dé de alta, modifique y borre sus propios vehículos, y que esas altas/modificaciones pasen por una **validación del administrador** (para controlar qué vehículos acceden). El equipo de admin debe enterarse en el momento (email + push) para revisar.

## What Changes

Se aborda en **dos fases**. Este change entrega la **Fase 1 completa** y **diseña** (sin implementar) la **Fase 2**.

### Fase 1 — Self-service del empleado + estado + aviso al admin (alcance de implementación)
- **Nueva capacidad `employee-vehicle-self-service`:** sección **"Mis vehículos"** en el portal del empleado donde da de alta / modifica / borra sus propios vehículos (marca, modelo, matrícula obligatoria, color).
- **Estado de validación** en el vehículo: `PENDING` · `APPROVED` · `REJECTED` (con motivo). Al **dar de alta o modificar** un vehículo desde el self-service, queda en `PENDING`. Los vehículos que **crea el `ADMIN`** (capacidad `employee-vehicles` existente) nacen `APPROVED` (alta por admin = validada).
- **Notificación a administradores** (email + push) cada vez que un empleado **envía o modifica** un vehículo, reutilizando el `NotificationDispatcher` (destinatarios = `Employee` con `role=ADMIN` y `active=true`) y la infraestructura de outbox de email + push existente.
- El empleado **ve el estado** de cada vehículo y, si está `REJECTED`, el **motivo**.

### Fase 2 — Pantalla de validación del admin (solo diseño en este change)
- **Bandeja de validación** en el admin: cola de vehículos pendientes con **aprobar** y **rechazar (con motivo obligatorio)**, reutilizando el patrón de `PendingRequestsPage` + `RejectRequestModal`.
- **Notificación al empleado** (email + push) cuando su vehículo es aprobado o rechazado (con el motivo).
- Se documentan requisitos, diseño y tareas de Fase 2, pero **no se implementan** en este change (irán en un change de continuación).

## Capabilities

### New Capabilities
- `employee-vehicle-self-service`: gestión por el propio empleado de sus vehículos con flujo de validación (estados PENDING/APPROVED/REJECTED con motivo), la sección "Mis vehículos" del portal, el aviso a administradores por email + push al enviar/modificar, y (Fase 2) la pantalla de validación del admin con aprobación/rechazo y el aviso al empleado.

### Modified Capabilities
- `employee-vehicles` (impacto, sin delta de spec propio): los vehículos ganan `status`/`rejectionReason`. El nuevo estado y su semántica los define la capacidad `employee-vehicle-self-service` (requisito "Estado de validación del vehículo"); el CRUD de admin no cambia su contrato de operaciones, pero los vehículos que crea el admin nacen `APPROVED` y su respuesta incluye ahora el estado.

## Impact

- **BD:** `ALTER TABLE employee_vehicles` añadiendo `status` (NVARCHAR, default `PENDING`; migración marca los existentes como `APPROVED`), `rejection_reason` (NVARCHAR NULL), y auditoría mínima de revisión (`reviewed_at`, `reviewed_by` NULL). Nueva migración Flyway **V37**.
- **Backend (Fase 1):** endpoints self-service acotados al empleado autenticado (`/api/v1/me/vehicles` GET/POST/PUT/DELETE), que fuerzan `status=PENDING` en alta/edición; nuevo `NotificationEventType.EMPLOYEE_VEHICLE_SUBMITTED` + método en `NotificationDispatcher` (a admins). El `EmployeeVehicleService`/DTO existentes exponen `status`/`rejectionReason`; el alta por admin fija `APPROVED`.
- **Backend (Fase 2, diseño):** endpoints de validación (`/api/v1/employee-vehicles/pending`, `POST …/{id}/approve`, `POST …/{id}/reject`), eventos `EMPLOYEE_VEHICLE_APPROVED` / `EMPLOYEE_VEHICLE_REJECTED` (al empleado).
- **Frontend (Fase 1):** página `MyVehiclesPage` (portal, patrón `PageHeader` como `MyRequestsPage`/`MyFixedAssignmentsPage`; tarjetas `mv-card` con `status-badge` reutilizado; `InfoBanner` de aviso de validación; reutiliza el modal de alta/edición de `VehiclesPanel` y `ConfirmDialog` de borrado). Tipos/hooks/api self-service; ruta + entrada de navegación en el portal; i18n `vehicles.mine.*` + `vehicles.status.*`.
- **Frontend (Fase 2, diseño):** página `VehicleReviewPage` (clon adelgazado de `PendingRequestsPage`: `PageFrame` + `chip-filters` por estado con contador + tabla + `SortableTh`), `RejectVehicleModal` (clon de `RejectRequestModal`), `VehicleReviewBadge` (contador de pendientes), i18n `vehicles.review.*`.
- **Sin breaking changes de contrato:** las operaciones del CRUD admin se mantienen; solo se añade `status`/`rejectionReason` a la respuesta del vehículo.

## Out of scope

- Acciones en lote (aprobar/rechazar varios a la vez) e historial/auditoría detallada de cambios de estado.
- Usar el estado del vehículo en accesos/plano/matching, ni "vehículo activo".
- Consolidar/retirar el campo suelto `employee.licensePlate` (sigue como en `employee-vehicles`).
- Unicidad GLOBAL de matrícula entre empleados (se mantiene única por empleado; ver Open Questions).
